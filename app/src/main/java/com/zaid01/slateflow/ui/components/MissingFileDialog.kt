package com.zaid01.slateflow.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun MissingFileDialog(name: String, ok: () -> Unit, remove: () -> Unit) {
    AlertDialog(
        onDismissRequest = ok,
        icon = { AppIcon(AppSymbol.DOCUMENT) },
        title = { Text("File not found") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("This file may have been moved, renamed, or deleted, or access is no longer available.")
                Text("You can remove it from your reading history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = ok) { AppIcon(AppSymbol.CHECK, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("OK") }
        },
        dismissButton = {
            OutlinedButton(onClick = remove, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                AppIcon(AppSymbol.MINUS, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Remove")
            }
        },
    )
}
