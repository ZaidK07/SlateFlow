package com.zaid01.slateflow.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Serializes access: PdfRenderer permits only one open page at a time. */
class PdfEngine(context: Context, uri: Uri) : Closeable {
    private val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("The PDF is unavailable. Select the file again.")
    private val renderer = try { PdfRenderer(descriptor) } catch (e: Exception) { descriptor.close(); throw e }
    val count = renderer.pageCount
    private var closed = false
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    suspend fun render(index: Int, requestedWidth: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(this@PdfEngine) {
            check(!closed) { "The PDF has been closed." }
            val key = "$index:$requestedWidth"
            cache.get(key) ?: renderer.openPage(index).use { page ->
                val ratio = page.height.toFloat() / page.width
                // Cap each bitmap at 4 MiB, including unusually tall PDF pages.
                val width = minOf(requestedWidth, 1600, sqrt(1_048_576f / ratio).toInt()).coerceAtLeast(1)
                val height = (width * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(key, bitmap)
                    while (cache.snapshot().size > 3) cache.remove(cache.snapshot().keys.first())
                }
            }
        }
    }
    override fun close() = synchronized(this) {
        if (!closed) { closed = true; cache.evictAll(); renderer.close(); descriptor.close() }
        // Displayed bitmaps are released by the UI/GC; never recycle a live Canvas bitmap.
    }
}
