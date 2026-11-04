package com.zaid01.slateflow.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaid01.slateflow.data.Document
import kotlin.math.roundToInt

@Composable
internal fun FileBadge(kind: String) {
    val pdf = kind == "pdf"
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (pdf) colors.secondaryContainer else colors.primaryContainer,
        contentColor = if (pdf) colors.onSecondaryContainer else colors.onPrimaryContainer,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Text(if (pdf) "PDF" else "MD", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun DocumentTile(document: Document, list: Boolean, open: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = open, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FileBadge(document.kind)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(document.name, style = MaterialTheme.typography.titleSmall, maxLines = if (list) 1 else 2, minLines = if (list) 1 else 2, overflow = TextOverflow.Ellipsis)
                    Text(document.folderName.ifBlank { if (document.kind == "pdf") "PDF document" else "Markdown document" }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (list) Text("${(document.progress.coerceIn(0f, 1f) * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = colors.primary)
            }
            LinearProgressIndicator(
                progress = { document.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = colors.primary, trackColor = colors.surfaceVariant,
                drawStopIndicator = {},
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(DateUtils.getRelativeTimeSpanString(document.openedAt).toString(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                if (!list) Text("${(document.progress.coerceIn(0f, 1f) * 100).roundToInt()}% read", style = MaterialTheme.typography.labelSmall, color = colors.primary)
            }
        }
    }
}
