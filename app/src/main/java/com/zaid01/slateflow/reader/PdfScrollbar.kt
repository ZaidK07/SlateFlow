package com.zaid01.slateflow.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** A dedicated edge gutter keeps the indicator out of the page's drawing area. */
@Composable
internal fun PdfScrollbar(state: LazyListState) {
    var visible by remember { mutableStateOf(false) }
    val scrolling = state.isScrollInProgress
    LaunchedEffect(scrolling) {
        if (scrolling) visible = true
        else {
            delay(600)
            visible = false
        }
    }
    val alpha by animateFloatAsState(if (visible) 0.8f else 0f, tween(180), label = "Scrollbar fade")
    val color = MaterialTheme.colorScheme.outline
    Canvas(Modifier.width(8.dp).fillMaxHeight().testTag("pdf-scrollbar")) {
        val layout = state.layoutInfo
        val first = layout.visibleItemsInfo.firstOrNull() ?: return@Canvas
        val last = layout.visibleItemsInfo.lastOrNull() ?: return@Canvas
        if (layout.totalItemsCount == 0 || (!state.canScrollForward && !state.canScrollBackward)) return@Canvas
        val start = (first.index + (-first.offset).coerceAtLeast(0).toFloat() / first.size.coerceAtLeast(1)) / layout.totalItemsCount
        val end = (last.index + ((layout.viewportEndOffset - last.offset).toFloat() / last.size.coerceAtLeast(1)).coerceIn(0f, 1f)) / layout.totalItemsCount
        val extent = (end - start).coerceIn(0.001f, 1f)
        val height = (size.height * extent).coerceIn(24.dp.toPx().coerceAtMost(size.height), size.height)
        val top = (start / (1f - extent).coerceAtLeast(0.001f)).coerceIn(0f, 1f) * (size.height - height)
        val width = 3.dp.toPx()
        drawRoundRect(color.copy(alpha = alpha), Offset(size.width - width, top), Size(width, height), CornerRadius(width / 2))
    }
}
