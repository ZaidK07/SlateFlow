package com.zaid01.slateflow.reader

import android.net.Uri
import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Typeface
import com.zaid01.slateflow.ui.components.AppIcon
import com.zaid01.slateflow.ui.components.AppSymbol
import androidx.core.net.toUri
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zaid01.slateflow.data.*
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.markwon.syntax.Prism4jSyntaxHighlight
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.prism4j.Prism4j
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

@Composable
fun DocumentReader(document: Document, settings: ReaderSettings, expanded: Boolean, model: SlateModel) {
    key(document.uri) {
        if (document.kind == "pdf") PdfReader(document, settings, expanded, model)
        else MarkdownReader(document, settings, model)
    }
}

@Composable
private fun PdfReader(doc: Document, settings: ReaderSettings, expanded: Boolean, model: SlateModel) {
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
                val pdf = PdfEngine(context, doc.sourceUri.ifBlank { doc.uri }.toUri())
                synchronized(lock) { if (disposed) pdf.close() else opened = pdf }
                withContext(Dispatchers.Main) { if (!disposed) engine = pdf }
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) {
                withContext(Dispatchers.Main) {
                    if (!disposed) {
                        if (exception is java.io.FileNotFoundException || exception is SecurityException) model.fileUnavailable(doc)
                        else failure = "Could not open this PDF. It may be encrypted or damaged."
                    }
                }
            }
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
        else -> key(expanded, settings.dualSpread) {
            val columns = if (expanded && settings.dualSpread) 2 else 1
            val rows = (pdf.count + columns - 1) / columns
            val state = rememberLazyListState()
            val renderedPages = remember { mutableStateMapOf<Int, Boolean>() }
            var restored by remember { mutableStateOf(false) }
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
            var selection by remember { mutableStateOf<PdfTextSelection?>(null) }
            var showSearch by remember { mutableStateOf(false) }
            var query by remember { mutableStateOf("") }
            var searching by remember { mutableStateOf(false) }
            var searchJob by remember { mutableStateOf<Job?>(null) }
            var lastQuery by remember { mutableStateOf("") }
            var menu by remember { mutableStateOf(false) }
            val snackbar = remember { SnackbarHostState() }
            fun report(message: String) { scope.launch { snackbar.showSnackbar(message) } }
            fun clearSelection() {
                selection = null
                scope.launch { pdf.releaseTextPage() }
            }
            fun selectCurrentPage() {
                scope.launch {
                    try {
                        selection = pdf.selectPage(selection?.page ?: state.firstVisibleItemIndex * columns)
                        if (selection == null) report("This page has no selectable text.")
                    } catch (exception: CancellationException) { throw exception }
                    catch (exception: Exception) { report(exception.message ?: "Could not select this page.") }
                }
            }
            BackHandler(selection != null || showSearch) {
                if (selection != null) clearSelection() else showSearch = false
            }
            LaunchedEffect(state.isScrollInProgress) {
                if (state.isScrollInProgress && !searching && selection != null) clearSelection()
            }
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(state = state, modifier = Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(rows, key = { it }) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    repeat(columns) { column ->
                                        val page = row * columns + column
                                        if (page < pdf.count) {
                                            PdfPage(
                                                engine = pdf,
                                                index = page,
                                                modifier = Modifier.weight(1f),
                                                selection = selection?.takeIf { it.page == page },
                                                selected = { selection = it },
                                                error = ::report,
                                                pdfInvert = settings.pdfInvert,
                                                ready = { renderedPages[page] = true }
                                            )
                                        } else Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        PdfScrollbar(state)
                    }
                    if (selection != null) Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                val selected = selection ?: return@TextButton
                                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("PDF text", selected.text))
                            }) { Text("Copy") }
                            TextButton(onClick = ::selectCurrentPage) { Text("Select page") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = ::clearSelection) { Text("Clear") }
                        }
                    }
                    if (showSearch) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(query, { query = it; searchJob?.cancel(); searching = false }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Find in PDF") }, shape = RoundedCornerShape(12.dp))
                        TextButton(enabled = query.isNotBlank() && !searching, onClick = {
                            searching = true
                            val requestedQuery = query
                            searchJob = scope.launch {
                                try {
                                    val found = pdf.findNext(requestedQuery, selection?.page ?: (state.firstVisibleItemIndex * columns), if (requestedQuery == lastQuery) selection?.end ?: 0 else 0)
                                    lastQuery = requestedQuery
                                    if (found == null) report("No matching text found.")
                                    else {
                                        val row = found.page / columns
                                        state.scrollToItem(row)
                                        val ready = withTimeoutOrNull(10_000) { snapshotFlow { renderedPages[found.page] == true }.first { it } }
                                        if (ready == null) {
                                            report("The matching page could not be displayed. Try again.")
                                            return@launch
                                        }
                                        withFrameNanos { }
                                        val size = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == row }?.size ?: 1
                                        state.scrollToItem(row, ((found.bounds.firstOrNull()?.top ?: 0f) * size - state.layoutInfo.viewportSize.height / 3f).roundToInt().coerceAtLeast(0))
                                        selection = found
                                    }
                                } catch (exception: CancellationException) { throw exception }
                                catch (exception: Exception) { report(exception.message ?: "Could not search this PDF.") }
                                finally { searching = false }
                            }
                        }) { if (searching) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Next") }
                        TextButton(onClick = { searchJob?.cancel(); searching = false; showSearch = false }) { Text("Close") }
                    }
                    val current by remember { derivedStateOf { state.firstVisibleItemIndex * columns + 1 } }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box {
                            IconButton(onClick = { menu = true }) { AppIcon(AppSymbol.MORE, "PDF tools") }
                            DropdownMenu(menu, { menu = false }) {
                                if (expanded) {
                                    DropdownMenuItem(
                                        text = { Text(if (settings.dualSpread) "Single page view" else "Two-page spread") },
                                        onClick = { menu = false; model.update(settings.copy(dualSpread = !settings.dualSpread)) },
                                        leadingIcon = { AppIcon(AppSymbol.SPREAD) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (settings.pdfInvert) "Standard colors" else "Invert colors (Night)") },
                                    onClick = { menu = false; model.update(settings.copy(pdfInvert = !settings.pdfInvert)) },
                                    leadingIcon = { AppIcon(AppSymbol.MOON) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Select page text") },
                                    onClick = { menu = false; selectCurrentPage() },
                                    leadingIcon = { AppIcon(AppSymbol.CHECK) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Find in PDF") },
                                    onClick = { menu = false; showSearch = true },
                                    leadingIcon = { AppIcon(AppSymbol.SEARCH) }
                                )
                            }
                        }
                        Text("$current / ${pdf.count}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(value = current.toFloat(), onValueChange = { page ->
                            scope.launch { state.scrollToItem(((page.roundToInt() - 1) / columns).coerceIn(0, rows - 1)) }
                        }, valueRange = 1f..maxOf(2, pdf.count).toFloat(), enabled = pdf.count > 1, modifier = Modifier.weight(1f))
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun PdfPage(
    engine: PdfEngine,
    index: Int,
    modifier: Modifier,
    selection: PdfTextSelection?,
    selected: (PdfTextSelection?) -> Unit,
    error: (String) -> Unit,
    pdfInvert: Boolean,
    ready: () -> Unit
) {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var pageSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(offset: Offset, currentScale: Float, size: IntSize): Offset {
        if (currentScale <= 1.01f || size.width <= 0 || size.height <= 0) return Offset.Zero
        val maxPanX = (size.width * (currentScale - 1f)) / 2f
        val maxPanY = (size.height * (currentScale - 1f)) / 2f
        return Offset(offset.x.coerceIn(-maxPanX, maxPanX), offset.y.coerceIn(-maxPanY, maxPanY))
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        panOffset = clampOffset(panOffset + panChange, newScale, pageSize)
    }

    val isZoomed = scale > 1.05f

    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { pageSize = it }
    ) {
        val width = with(LocalDensity.current) { maxWidth.roundToPx() }
        var failure by remember { mutableStateOf(false) }
        val bitmap by produceState<android.graphics.Bitmap?>(null, engine, index, width) {
            try { value = engine.render(index, width.coerceAtLeast(1)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure = true }
        }
        val page = bitmap
        LaunchedEffect(page) { if (page != null) { withFrameNanos { }; ready() } }

        val invertFilter = remember {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.85f,  0f,     0f,     0f, 235f,
                         0f,    -0.85f,  0f,     0f, 235f,
                         0f,     0f,    -0.85f,  0f, 235f,
                         0f,     0f,     0f,     1f,   0f
                    )
                )
            )
        }

        if (page != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                SelectablePdfPage(
                    engine = engine,
                    page = index,
                    selection = selection,
                    selected = selected,
                    error = error,
                ) {
                    Image(
                        bitmap = page.asImageBitmap(),
                        contentDescription = "Page ${index + 1}",
                        modifier = Modifier.fillMaxWidth().aspectRatio(page.width.toFloat() / page.height),
                        colorFilter = if (pdfInvert) invertFilter else null
                    )
                }
            }
            if (scale > 1.05f) {
                FilledTonalButton(
                    onClick = { scale = 1f; panOffset = Offset.Zero },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("${(scale * 100).roundToInt()}% · Reset", style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(0.707f), contentAlignment = Alignment.Center) {
                if (failure) Text("Page could not be rendered", Modifier.padding(16.dp))
                else CircularProgressIndicator(Modifier.size(24.dp))
            }
        }
    }
}

internal class MarkdownScroll(context: android.content.Context) : ScrollView(context) {
    var anchor = 0
    var lineFraction = 0f
    var restored = false
    var applying = true
    var onPosition: ((ReadingPosition) -> Unit)? = null
    val text = TextView(context).apply {
        setTextIsSelectable(true)
        movementMethod = android.text.method.LinkMovementMethod.getInstance()
        setPadding(0, 0, 0, 32)
    }
    init {
        isFillViewport = true
        isVerticalScrollBarEnabled = true
        isHorizontalScrollBarEnabled = false
        verticalScrollbarPosition = View.SCROLLBAR_POSITION_RIGHT
        scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
        isScrollbarFadingEnabled = true
        scrollBarDefaultDelayBeforeFade = 600
        scrollBarFadeDuration = 180
        scrollBarSize = (3 * resources.displayMetrics.density).roundToInt()
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
        isFocusableInTouchMode = true
        requestFocus()
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        applying = true
        super.onLayout(changed, left, top, right, bottom)
        restoreAnchor()
        postOnAnimation { restored = true; applying = false }
    }
    override fun onDetachedFromWindow() {
        onPosition = null
        applying = true
        super.onDetachedFromWindow()
    }
    private fun restoreAnchor() {
        val layout = text.layout ?: return
        if (width == 0) return
        val line = layout.getLineForOffset(anchor.coerceIn(0, text.length()))
        val top = layout.getLineTop(line)
        scrollTo(0, top + (lineFraction * (layout.getLineBottom(line) - top)).roundToInt())
    }
}


@Composable
private fun MarkdownReader(doc: Document, settings: ReaderSettings, model: SlateModel) {
    val context = LocalContext.current
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val accent = MaterialTheme.colorScheme.secondary.toArgb()
    val dark = com.zaid01.slateflow.ui.theme.isReaderDark(settings.themeMode)

    val prism4j = remember { Prism4j(SlateGrammarLocator()) }
    val syntaxTheme = remember(dark) {
        if (dark) Prism4jThemeDarkula.create() else Prism4jThemeDefault.create()
    }
    val syntaxPlugin = remember(prism4j, syntaxTheme) {
        SyntaxHighlightPlugin.create(prism4j, syntaxTheme)
    }

    val tableTheme = remember(dark, color, accent) {
        val density = context.resources.displayMetrics.density
        TableTheme.Builder()
            .tableBorderColor(if (dark) android.graphics.Color.rgb(65, 75, 90) else android.graphics.Color.rgb(215, 222, 232))
            .tableBorderWidth((1.5f * density).roundToInt())
            .tableHeaderRowBackgroundColor(if (dark) android.graphics.Color.argb(80, 42, 52, 68) else android.graphics.Color.argb(50, 200, 215, 235))
            .tableOddRowBackgroundColor(if (dark) android.graphics.Color.argb(30, 255, 255, 255) else android.graphics.Color.argb(15, 0, 0, 0))
            .tableCellPadding((10 * density).roundToInt())
            .build()
    }

    // A single worker prevents a formula-heavy chapter from creating dozens of render threads.
    val mathExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    DisposableEffect(mathExecutor) {
        onDispose { mathExecutor.shutdownNow() }
    }
    val markwon: Markwon = remember(context, dark, color, accent, settings.font) {
        val density = context.resources.displayMetrics.density
        ru.noties.jlatexmath.JLatexMathAndroid.init(context.applicationContext)
        Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(MarkdownMathPlugin())
            .usePlugin(TablePlugin.create(tableTheme))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(accent, if (dark) android.graphics.Color.GRAY else android.graphics.Color.LTGRAY, if (dark) android.graphics.Color.DKGRAY else android.graphics.Color.WHITE))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(JLatexMathPlugin.create(settings.font * context.resources.displayMetrics.scaledDensity) { builder ->
                builder.inlinesEnabled(true)
                builder.executorService(mathExecutor)
                builder.errorHandler { _, error ->
                    android.util.Log.w("SlateFlowMath", "Could not render a math expression", error)
                    // Keep the original expression readable when TeX is invalid or unsupported.
                    null
                }
                builder.theme().textColor(color)
                builder.theme().backgroundProvider { android.graphics.drawable.ColorDrawable(if (dark) android.graphics.Color.argb(40, 255, 255, 255) else android.graphics.Color.argb(20, 0, 0, 0)) }
            })
            .usePlugin(syntaxPlugin)
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .headingBreakHeight(1)
                        .headingBreakColor(if (dark) android.graphics.Color.rgb(55, 65, 80) else android.graphics.Color.rgb(220, 225, 235))
                        .headingTextSizeMultipliers(floatArrayOf(1.6f, 1.35f, 1.2f, 1.1f, 1f, 1f))
                        .blockQuoteColor(accent)
                        .blockQuoteWidth((4 * density).roundToInt())
                        .codeBackgroundColor(if (dark) android.graphics.Color.rgb(36, 44, 58) else android.graphics.Color.rgb(238, 242, 248))
                        .codeBlockBackgroundColor(if (dark) android.graphics.Color.rgb(24, 30, 40) else android.graphics.Color.rgb(242, 246, 252))
                        .codeTextColor(accent)
                        .codeTypeface(Typeface.MONOSPACE)
                        .codeBlockTypeface(Typeface.MONOSPACE)
                        .thematicBreakColor(if (dark) android.graphics.Color.rgb(55, 65, 80) else android.graphics.Color.rgb(220, 225, 235))
                        .thematicBreakHeight((1 * density).roundToInt())
                        .linkColor(accent)
                        .isLinkUnderlined(true)
                }
                override fun configureConfiguration(builder: io.noties.markwon.MarkwonConfiguration.Builder) {
                    builder.linkResolver { view, link ->
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(link)).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            view.context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            })
            .build()
    }

    var rendered by remember(doc.uri, markwon) { mutableStateOf<CharSequence?>(null) }
    var failure by remember(doc.uri, markwon) { mutableStateOf<String?>(null) }
    LaunchedEffect(doc.uri, markwon) {
        try {
            rendered = withContext(Dispatchers.Default) {
                val source = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(doc.sourceUri.ifBlank { doc.uri }.toUri())?.buffered()?.use { stream ->
                        val bytes = stream.readNBytes(12 * 1024 * 1024 + 1)
                        require(bytes.size <= 12 * 1024 * 1024) { "This Markdown file exceeds the 12 MB reading limit." }
                        bytes.toString(Charsets.UTF_8)
                    } ?: error("The file is unavailable. Select it again.")
                }
                markwon.render(markwon.parse(source))
            }
        } catch (e: CancellationException) { throw e }
        catch (exception: Exception) {
            if (exception is java.io.FileNotFoundException || exception is SecurityException) model.fileUnavailable(doc)
            else failure = "Could not read this Markdown file. It may be too large or damaged."
        }
    }
    when {
        failure != null -> ReaderMessage(failure!!)
        rendered == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else -> BoxWithConstraints(Modifier.fillMaxSize()) {
            val margin = settings.margin.dp.coerceAtMost((maxWidth - 160.dp).coerceAtLeast(0.dp) / 2)
            val density = LocalDensity.current
            val marginPixels = with(density) { margin.roundToPx() }
            key(dark) {
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
                    val themedContext = android.view.ContextThemeWrapper(ctx, if (dark) com.zaid01.slateflow.R.style.Theme_SlateFlow_Reader_Dark else com.zaid01.slateflow.R.style.Theme_SlateFlow_Reader_Light)
                    MarkdownScroll(themedContext).apply { anchor = doc.anchor; lineFraction = doc.fraction }
                }, update = { scroll ->
                    scroll.onPosition = { p ->
                        doc.anchor = p.anchor; doc.offset = p.offset; doc.fraction = p.fraction; doc.progress = p.progress
                        model.position(doc.uri, p)
                    }
                    if (scroll.text.paddingLeft != marginPixels) {
                        scroll.text.setPadding(marginPixels, 0, marginPixels, (32 * scroll.resources.displayMetrics.density).roundToInt())
                    }
                    scroll.verticalScrollbarThumbDrawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.argb(180, android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color)))
                        cornerRadius = 4 * scroll.resources.displayMetrics.density
                    }
                    scroll.text.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.zaid01.slateflow.R.font.inter)
                    scroll.text.setTextColor(color)
                    scroll.text.setLinkTextColor(accent)
                    scroll.text.textSize = settings.font
                    scroll.text.setLineSpacing(0f, settings.spacing)
                    if (scroll.text.text !== rendered) markwon.setParsedMarkdown(scroll.text, rendered as android.text.Spanned)
                })
            }
        }
    }
}

@Composable
private fun ReaderMessage(message: String) { Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
