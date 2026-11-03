package com.zaid01.slateflow.reader

import android.net.Uri
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import com.zaid01.slateflow.data.*
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
fun DocumentReader(document: Document, settings: ReaderSettings, expanded: Boolean, model: SlateModel) {
    key(document.uri) {
        if (document.kind == "pdf") PdfReader(document, expanded, model)
        else MarkdownReader(document, settings, model)
    }
}

@Composable
private fun PdfReader(doc: Document, expanded: Boolean, model: SlateModel) {
    val context = LocalContext.current
    var engine by remember { mutableStateOf<PdfEngine?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    DisposableEffect(doc.uri) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var opened: PdfEngine? = null
        var disposed = false
        val lock = Any()
        scope.launch {
            try {
                val pdf = PdfEngine(context, Uri.parse(doc.uri))
                synchronized(lock) { if (disposed) pdf.close() else opened = pdf }
                withContext(Dispatchers.Main) { if (!disposed) engine = pdf }
            } catch (e: Exception) { withContext(Dispatchers.Main) { if (!disposed) failure = "Could not open this PDF. It may be encrypted, damaged, or no longer accessible." } }
        }
        onDispose {
            synchronized(lock) {
                disposed = true
                scope.launch { opened?.close(); scope.cancel() }
            }
        }
    }
    val pdf = engine
    when {
        failure != null -> ReaderMessage(failure!!)
        pdf == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        pdf.count == 0 -> ReaderMessage("This PDF has no pages.")
        else -> key(expanded) {
            val columns = if (expanded) 2 else 1
            val rows = (pdf.count + columns - 1) / columns
            val state = rememberLazyListState()
            val renderedPages = remember { mutableStateMapOf<Int, Boolean>() }
            var restored by remember { mutableStateOf(false) }
            // Capture the current in-memory position when the spread changes.
            val initial = remember { ReadingPosition(doc.anchor, doc.offset, doc.fraction, doc.progress) }
            val initialRow = (initial.anchor / columns).coerceIn(0, rows - 1)
            LaunchedEffect(pdf, columns) { state.scrollToItem(initialRow) }
            val initialPagesReady = (initialRow * columns until minOf((initialRow + 1) * columns, pdf.count)).all { renderedPages[it] == true }
            LaunchedEffect(initialPagesReady) {
                if (initialPagesReady && !restored) {
                    withFrameNanos { }
                    withFrameNanos { }
                    val size = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == initialRow }?.size ?: 1
                    state.scrollToItem(initialRow, (initial.fraction * size).roundToInt())
                    restored = true
                }
            }
            LaunchedEffect(state, restored) {
                if (restored) snapshotFlow {
                    val item = state.layoutInfo.visibleItemsInfo.firstOrNull()
                    val anchor = state.firstVisibleItemIndex * columns
                    ReadingPosition(anchor, state.firstVisibleItemScrollOffset,
                        state.firstVisibleItemScrollOffset.toFloat() / (item?.size ?: 1).coerceAtLeast(1),
                        ((anchor + 1f) / pdf.count).coerceIn(0f, 1f))
                }.distinctUntilChanged().collect { p ->
                    doc.anchor = p.anchor; doc.offset = p.offset; doc.fraction = p.fraction; doc.progress = p.progress
                    model.position(doc.uri, p)
                }
            }
            val scope = rememberCoroutineScope()
            Column(Modifier.fillMaxSize()) {
                LazyColumn(state = state, modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(rows, key = { it }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(columns) { column ->
                                val page = row * columns + column
                                if (page < pdf.count) PdfPage(pdf, page, Modifier.weight(1f)) { renderedPages[page] = true }
                                else Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                val current by remember { derivedStateOf { state.firstVisibleItemIndex * columns + 1 } }
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("$current / ${pdf.count}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = current.toFloat(), onValueChange = { page ->
                        scope.launch { state.scrollToItem(((page.roundToInt() - 1) / columns).coerceIn(0, rows - 1)) }
                    }, valueRange = 1f..maxOf(2, pdf.count).toFloat(), enabled = pdf.count > 1, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PdfPage(engine: PdfEngine, index: Int, modifier: Modifier, ready: () -> Unit) {
    BoxWithConstraints(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        val width = with(LocalDensity.current) { maxWidth.roundToPx() }
        var failure by remember { mutableStateOf(false) }
        val bitmap by produceState<android.graphics.Bitmap?>(null, engine, index, width) {
            try { value = engine.render(index, width.coerceAtLeast(1)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure = true }
        }
        val page = bitmap
        LaunchedEffect(page) { if (page != null) { withFrameNanos { }; ready() } }
        if (page != null) Image(page.asImageBitmap(), "Page ${index + 1}", Modifier.fillMaxWidth().aspectRatio(page.width.toFloat() / page.height))
        else Box(Modifier.fillMaxWidth().aspectRatio(0.707f), contentAlignment = Alignment.Center) {
            if (failure) Text("Page could not be rendered", Modifier.padding(16.dp))
            else CircularProgressIndicator(Modifier.size(24.dp))
        }
    }
}

private class MarkdownScroll(context: android.content.Context) : ScrollView(context) {
    var anchor = 0
    var lineFraction = 0f
    var restored = false
    var applying = false
    var onPosition: ((ReadingPosition) -> Unit)? = null
    val text = TextView(context).apply { setTextIsSelectable(true); setPadding(0, 0, 0, 32) }
    init {
        isFillViewport = true
        addView(text, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        setOnScrollChangeListener { _, _, y, _, _ ->
            if (restored && !applying) {
                val layout = text.layout ?: return@setOnScrollChangeListener
                val line = layout.getLineForVertical(y)
                anchor = layout.getLineStart(line)
                val top = layout.getLineTop(line)
                lineFraction = (y - top).toFloat() / (layout.getLineBottom(line) - top).coerceAtLeast(1)
                onPosition?.invoke(ReadingPosition(anchor, y - top, lineFraction, y.toFloat() / (text.height - height).coerceAtLeast(1)))
            }
        }
        text.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> restoreAnchor() }
    }
    fun restoreAnchor() {
        val layout = text.layout ?: return
        if (width == 0) return
        applying = true
        val line = layout.getLineForOffset(anchor.coerceIn(0, text.length()))
        val top = layout.getLineTop(line)
        scrollTo(0, top + (lineFraction * (layout.getLineBottom(line) - top)).roundToInt())
        restored = true
        applying = false
    }
}

@Composable
private fun MarkdownReader(doc: Document, settings: ReaderSettings, model: SlateModel) {
    val context = LocalContext.current
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    val markwon = remember { Markwon.builder(context)
        .usePlugin(TablePlugin.create(context)).usePlugin(StrikethroughPlugin.create())
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder.headingBreakHeight(1).headingTextSizeMultipliers(floatArrayOf(1.6f, 1.35f, 1.2f, 1.1f, 1f, 1f))
            }
        }).build() }
    var rendered by remember { mutableStateOf<CharSequence?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(doc.uri) {
        try {
            rendered = withContext(Dispatchers.Default) {
                val source = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(Uri.parse(doc.uri))?.buffered()?.use { stream ->
                        val bytes = stream.readNBytes(12 * 1024 * 1024 + 1)
                        require(bytes.size <= 12 * 1024 * 1024) { "This Markdown file exceeds the 12 MB reading limit." }
                        bytes.toString(Charsets.UTF_8)
                    } ?: error("The file is unavailable. Select it again.")
                }
                markwon.render(markwon.parse(source))
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { failure = e.message ?: "Could not read this Markdown file." }
    }
    when {
        failure != null -> ReaderMessage(failure!!)
        rendered == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val margin = settings.margin.dp.coerceAtMost((maxWidth - 160.dp).coerceAtLeast(0.dp) / 2)
            AndroidView(modifier = Modifier.fillMaxSize().padding(horizontal = margin), factory = { ctx ->
                MarkdownScroll(ctx).apply { anchor = doc.anchor; lineFraction = doc.fraction }
            }, update = { scroll ->
                scroll.onPosition = { p ->
                    doc.anchor = p.anchor; doc.offset = p.offset; doc.fraction = p.fraction; doc.progress = p.progress
                    model.position(doc.uri, p)
                }
                scroll.text.setTextColor(color)
                scroll.text.setLinkTextColor(accent)
                scroll.text.textSize = settings.font
                scroll.text.setLineSpacing(0f, settings.spacing)
                if (scroll.text.text !== rendered) markwon.setParsedMarkdown(scroll.text, rendered as android.text.Spanned)
                scroll.text.doOnLayout { scroll.restoreAnchor() }
            })
        }
    }
}

@Composable
private fun ReaderMessage(message: String) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
