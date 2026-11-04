package com.zaid01.slateflow.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.util.LruCache
import io.legere.pdfiumandroid.PdfPage
import io.legere.pdfiumandroid.PdfTextPage
import io.legere.pdfiumandroid.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal data class PdfTextSelection(
    val page: Int, val start: Int, val end: Int,
    val text: String, val bounds: List<RectF>,
)

/** One native document, a bounded bitmap cache, and at most one open text page. */
class PdfEngine(context: Context, uri: Uri) : Closeable {
    private val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("The PDF is unavailable. Select the file again.")
    private val document = try { PdfiumCore(context).newDocument(descriptor) } catch (exception: Throwable) { descriptor.close(); throw exception }
    val count = document.getPageCount()
    private var closed = false
    private var textSession: TextSession? = null
    private data class TextSession(val index: Int, val page: PdfPage, val text: PdfTextPage) : Closeable {
        override fun close() { try { text.close() } finally { page.close() } }
    }
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    suspend fun render(index: Int, requestedWidth: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(this@PdfEngine) {
            check(!closed) { "The PDF has been closed." }
            val key = "$index:$requestedWidth"
            cache.get(key) ?: (document.openPage(index) ?: error("The page is unavailable.")).use { page ->
                val ratio = page.getPageHeightPoint().toFloat() / page.getPageWidthPoint().coerceAtLeast(1)
                val width = minOf(requestedWidth, 1600, sqrt(1_048_576f / ratio).toInt()).coerceAtLeast(1)
                val height = (width * ratio).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    page.renderPageBitmap(bitmap, 0, 0, width, height, renderAnnot = true)
                    cache.put(key, bitmap)
                    while (cache.snapshot().size > 3) cache.remove(cache.snapshot().keys.first())
                }
            }
        }
    }

    private fun textPage(index: Int): TextSession {
        check(!closed) { "The PDF has been closed." }
        textSession?.takeIf { it.index == index }?.let { return it }
        textSession?.close()
        textSession = null
        val page = document.openPage(index) ?: error("The page is unavailable.")
        return try { TextSession(index, page, page.openTextPage()).also { textSession = it } }
        catch (exception: Throwable) { page.close(); throw exception }
    }

    /** Points and highlight bounds are normalized to the exact displayed page, including rotation. */
    private fun characterAt(session: TextSession, x: Float, y: Float): Int {
        val point = session.page.mapDeviceCoordsToPage(0, 0, 10_000, 10_000, 0, (x.coerceIn(0f, 1f) * 10_000).roundToInt(), (y.coerceIn(0f, 1f) * 10_000).roundToInt())
        return session.text.textPageGetCharIndexAtPos(point.x.toDouble(), point.y.toDouble(), 8.0, 8.0)
    }

    internal suspend fun selectWord(index: Int, x: Float, y: Float): PdfTextSelection? = withContext(Dispatchers.IO) {
        synchronized(this@PdfEngine) {
            val session = textPage(index)
            val hit = characterAt(session, x, y)
            if (hit < 0) return@synchronized null
            val total = session.text.textPageCountChars()
            var start = hit
            var end = hit + 1
            fun wordCharacter(char: Char) = char.isLetterOrDigit() || char == '_'
            if (wordCharacter(session.text.textPageGetUnicode(hit))) {
                while (start > 0 && hit - start < 512 && wordCharacter(session.text.textPageGetUnicode(start - 1))) start--
                while (end < total && end - hit < 512 && wordCharacter(session.text.textPageGetUnicode(end))) end++
            }
            selection(session, start, end)
        }
    }

    internal suspend fun extendSelection(index: Int, fixed: Int, x: Float, y: Float): PdfTextSelection? = withContext(Dispatchers.IO) {
        synchronized(this@PdfEngine) {
            val session = textPage(index)
            val hit = characterAt(session, x, y)
            if (hit < 0) return@synchronized null
            selection(session, min(fixed, hit), max(fixed, hit) + 1)
        }
    }

    internal suspend fun selectPage(index: Int): PdfTextSelection? = withContext(Dispatchers.IO) {
        synchronized(this@PdfEngine) {
            val session = textPage(index)
            selection(session, 0, session.text.textPageCountChars())
        }
    }

    internal suspend fun findNext(query: String, fromPage: Int, fromCharacter: Int): PdfTextSelection? = withContext(Dispatchers.IO) {
        if (query.isBlank() || count == 0) return@withContext null
        for (step in 0..count) {
            currentCoroutineContext().ensureActive()
            val match = synchronized(this@PdfEngine) {
                val index = (fromPage + step) % count
                val session = textPage(index)
                val start = if (step == 0) fromCharacter.coerceAtLeast(0) else 0
                session.text.findStart(query, emptySet(), start)?.use { search ->
                    if (search.findNext()) {
                        val first = search.getSchResultIndex()
                        selection(session, first, first + search.getSchCount())
                    } else null
                }
            }
            if (match != null) return@withContext match
        }
        null
    }

    private fun selection(session: TextSession, start: Int, end: Int): PdfTextSelection? {
        if (end <= start) return null
        require(end - start <= 100_000) { "This selection is too large. Select a smaller section." }
        val text = session.text.textPageGetText(start, end - start).orEmpty()
        if (text.isBlank()) return null
        val count = session.text.textPageCountRects(start, end - start)
        val bounds = (0 until count).mapNotNull { rectIndex ->
            val rect = session.text.textPageGetRect(rectIndex) ?: return@mapNotNull null
            val first = session.page.mapPageCoordsToDevice(0, 0, 10_000, 10_000, 0, rect.left.toDouble(), rect.top.toDouble())
            val last = session.page.mapPageCoordsToDevice(0, 0, 10_000, 10_000, 0, rect.right.toDouble(), rect.bottom.toDouble())
            RectF(min(first.x, last.x) / 10_000f, min(first.y, last.y) / 10_000f, max(first.x, last.x) / 10_000f, max(first.y, last.y) / 10_000f)
        }
        return PdfTextSelection(session.index, start, end, text, bounds)
    }

    suspend fun releaseTextPage() = withContext(Dispatchers.IO) {
        synchronized(this@PdfEngine) { textSession?.close(); textSession = null }
    }

    override fun close() = synchronized(this) {
        if (!closed) {
            closed = true
            textSession?.close()
            textSession = null
            cache.evictAll()
            try { document.close() } finally { descriptor.close() }
        }
    }
}
