package com.zaid01.slateflow.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun SelectablePdfPage(
    engine: PdfEngine,
    page: Int,
    selection: PdfTextSelection?,
    selected: (PdfTextSelection?) -> Unit,
    error: (String) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    var size by remember { mutableStateOf(IntSize.Zero) }
    var anchor by remember { mutableStateOf<Int?>(null) }
    var request by remember { mutableStateOf<Job?>(null) }
    val currentSelected by rememberUpdatedState(selected)
    val currentError by rememberUpdatedState(error)
    fun extend(fixed: Int, position: Offset) {
        request?.cancel()
        request = scope.launch {
            try {
                engine.extendSelection(page, fixed, position.x / size.width.coerceAtLeast(1), position.y / size.height.coerceAtLeast(1))?.let(currentSelected)
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) { currentError(exception.message ?: "Could not select this text.") }
        }
    }
    Box(Modifier.fillMaxWidth().onSizeChanged { size = it }.pointerInput(engine, page) {
        detectDragGesturesAfterLongPress(
            onDragStart = { position ->
                anchor = null
                request?.cancel()
                request = scope.launch {
                    try {
                        val word = engine.selectWord(page, position.x / size.width.coerceAtLeast(1), position.y / size.height.coerceAtLeast(1))
                        anchor = word?.start
                        currentSelected(word)
                        if (word == null) currentError("No selectable text here. This part may be an image.")
                    } catch (exception: CancellationException) { throw exception }
                    catch (exception: Exception) { currentError(exception.message ?: "Could not select this text.") }
                }
            },
            onDrag = { change, _ ->
                change.consume()
                anchor?.let { extend(it, change.position) }
            },
        )
    }) {
        content()
        if (selection != null && selection.bounds.isNotEmpty()) {
            val color = MaterialTheme.colorScheme.secondary
            Canvas(Modifier.matchParentSize()) {
                selection.bounds.forEach { bounds ->
                    drawRect(color.copy(alpha = 0.28f), Offset(bounds.left * size.width, bounds.top * size.height), Size(bounds.width() * size.width, bounds.height() * size.height))
                }
            }
            val first = selection.bounds.first()
            val last = selection.bounds.last()
            SelectionHandle(Offset(first.left * size.width, first.bottom * size.height)) { extend(selection.end - 1, it) }
            SelectionHandle(Offset(last.right * size.width, last.bottom * size.height)) { extend(selection.start, it) }
        }
    }
}

@Composable
private fun SelectionHandle(position: Offset, move: (Offset) -> Unit) {
    val density = LocalDensity.current
    val half = with(density) { 18.dp.toPx() }
    val currentPosition by rememberUpdatedState(position)
    val currentMove by rememberUpdatedState(move)
    val color = MaterialTheme.colorScheme.secondary
    Canvas(Modifier.offset { IntOffset((position.x - half).roundToInt(), position.y.roundToInt()) }.size(36.dp).pointerInput(Unit) {
        var dragged = Offset.Zero
        detectDragGestures(
            onDragStart = { dragged = currentPosition },
            onDrag = { change, amount -> change.consume(); dragged += amount; currentMove(dragged) },
        )
    }) {
        drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, 8.dp.toPx()), 2.dp.toPx())
        drawCircle(color, 6.dp.toPx(), Offset(size.width / 2, 10.dp.toPx()))
    }
}
