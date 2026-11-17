package com.zaid01.slateflow.dashboard

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.zaid01.slateflow.data.Document
import com.zaid01.slateflow.data.FileFilter
import com.zaid01.slateflow.ui.components.*
import kotlin.math.roundToInt

@Composable
internal fun HistoryScreen(
    documents: List<Document>,
    open: (Document) -> Unit,
    remove: (String) -> Unit,
    toggleRead: (Uri, Boolean) -> Unit,
    openFile: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FileFilter.ALL) }
    var documentPendingRemoval by remember { mutableStateOf<Document?>(null) }

    val filtered = remember(documents, search, filter) {
        documents.filter { doc ->
            (filter == FileFilter.ALL || (filter == FileFilter.PDF && doc.kind == "pdf") || (filter == FileFilter.MARKDOWN && doc.kind == "md")) &&
            (doc.name.contains(search.trim(), ignoreCase = true) || doc.folderName.contains(search.trim(), ignoreCase = true))
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reading history", style = MaterialTheme.typography.titleLarge)
                        Text("${documents.size} ${if (documents.size == 1) "document" else "documents"} read or in progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text("Search reading history") },
                    leadingIcon = { AppIcon(AppSymbol.SEARCH) },
                    trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Clear") } },
                    singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                    }
                }
            }
        }

        if (filtered.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(AppSymbol.DOCUMENT, modifier = Modifier.size(36.dp))
                Text(if (documents.isEmpty()) "No reading history yet" else "No matching documents", style = MaterialTheme.typography.titleMedium)
                Text(if (documents.isEmpty()) "Open a PDF or Markdown file to start reading." else "Try another search term or filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (documents.isEmpty()) Button(onClick = openFile) { Text("Open a file") }
            }
        }

        items(filtered, key = { it.uri }) { doc ->
            HistoryTile(
                document = doc,
                open = { open(doc) },
                onToggleRead = {
                    val targetUri = doc.sourceUri.ifBlank { doc.uri }.toUri()
                    toggleRead(targetUri, !doc.isRead && doc.progress < 0.95f)
                },
                onRemove = { documentPendingRemoval = doc }
            )
        }
    }

    if (documentPendingRemoval != null) {
        val target = documentPendingRemoval!!
        AlertDialog(
            onDismissRequest = { documentPendingRemoval = null },
            title = { Text("Remove from history?") },
            text = { Text("\"${target.name}\" will be removed from your reading history and saved position. The actual file will not be deleted.") },
            dismissButton = { TextButton(onClick = { documentPendingRemoval = null }) { Text("Cancel") } },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uriToRemove = target.uri
                        documentPendingRemoval = null
                        remove(uriToRemove)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            }
        )
    }
}

@Composable
private fun HistoryTile(
    document: Document,
    open: () -> Unit,
    onToggleRead: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val isCompleted = document.isRead || document.progress >= 0.95f

    OutlinedCard(
        onClick = open,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FileBadge(document.kind)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(document.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        document.folderName.ifBlank { if (document.kind == "pdf") "PDF document" else "Markdown document" },
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) { AppIcon(AppSymbol.MORE, "Document actions") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (isCompleted) "Mark as unread" else "Mark as read") },
                            onClick = { menuOpen = false; onToggleRead() },
                            leadingIcon = { AppIcon(if (isCompleted) AppSymbol.MINUS else AppSymbol.CHECK) }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from history") },
                            onClick = { menuOpen = false; onRemove() },
                            leadingIcon = { AppIcon(AppSymbol.TRASH) }
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { document.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (isCompleted) colors.primary else colors.secondary,
                trackColor = colors.surfaceVariant,
                drawStopIndicator = {},
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(DateUtils.getRelativeTimeSpanString(document.openedAt).toString(), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Text(
                    if (isCompleted) "Completed" else "${(document.progress.coerceIn(0f, 1f) * 100).roundToInt()}% read",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCompleted) colors.primary else colors.secondary
                )
            }
        }
    }
}
