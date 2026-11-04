package com.zaid01.slateflow.explore

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaid01.slateflow.data.FileFilter
import com.zaid01.slateflow.ui.components.*

@Composable
internal fun ExploreScreen(model: ExploreModel, addFolder: () -> Unit, open: (FolderEntry, FolderLocation) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val summaries by model.summaries.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FileFilter.ALL) }
    LaunchedEffect(state.path) { search = "" }
    val entries = remember(state.entries, search, filter) {
        state.entries.filter {
            it.name.contains(search.trim(), ignoreCase = true) &&
                (it.isFolder || filter == FileFilter.ALL || (filter == FileFilter.PDF && it.kind == "pdf") || (filter == FileFilter.MARKDOWN && it.kind == "md"))
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(state.path.lastOrNull()?.name ?: "Your folders", style = MaterialTheme.typography.titleLarge)
                        Text(if (state.path.isEmpty()) "PDFs and Markdown, organized where they live." else if (state.loading) "Reading folder contents…" else FolderSummary(state.entries.count { it.isFolder }, state.entries.count { it.kind == "pdf" }, state.entries.count { it.kind == "md" }).label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = model::refresh) { AppIcon(AppSymbol.REFRESH, "Refresh folders") }
                    FilledTonalIconButton(onClick = addFolder) { AppIcon(AppSymbol.ADD, "Add folder") }
                }
                if (state.path.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { model.breadcrumb(-1) }) { Text("Folders") }
                        state.path.forEachIndexed { index, location ->
                            AppIcon(AppSymbol.CHEVRON, modifier = Modifier.size(16.dp))
                            TextButton(onClick = { model.breadcrumb(index) }) { Text(location.name, maxLines = 1) }
                        }
                    }
                    OutlinedTextField(
                        search, { search = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search this folder") }, leadingIcon = { AppIcon(AppSymbol.SEARCH) },
                        trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Clear") } },
                        shape = RoundedCornerShape(12.dp), singleLine = true, colors = nexusFieldColors(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FileFilter.entries.forEach { type -> FilterChip(filter == type, { filter = type }, label = { Text(type.label) }) }
                    }
                }
            }
        }
        if (state.loading) item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
        } else if (state.path.isEmpty()) {
            if (state.roots.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                ExploreEmpty("A home for your documents", "Add a folder to browse its PDFs, Markdown files, and subfolders.", addFolder)
            }
            items(state.roots, key = { it.grantUri }) { root ->
                val location = root.location
                if (location != null) LaunchedEffect(location.uri, state.revision) { model.loadSummary(location) }
                FolderTile(
                    name = location?.name ?: "Folder unavailable",
                    summary = if (root.unavailable) "Access lost · Remove and add this folder again" else summaries[location?.uri] ?: "Reading folder contents…",
                    open = { if (location != null) model.enter(location) },
                    remove = { model.removeRoot(root.grantUri) },
                )
            }
        } else {
            if (entries.isEmpty() && state.message == null) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                ExploreEmpty("Nothing to show here", if (search.isNotBlank()) "Try a different search." else "No matching PDF or Markdown files in this folder.")
            }
            val folders = entries.filter { it.isFolder }
            val files = entries.filterNot { it.isFolder }
            if (folders.isNotEmpty()) item(key = "folder-heading", span = { GridItemSpan(maxLineSpan) }) {
                Text("Folders", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(folders, key = { it.uri }) { entry ->
                val location = FolderLocation(entry.uri, entry.name)
                LaunchedEffect(entry.uri, state.revision) { model.loadSummary(location) }
                FolderTile(entry.name, summaries[entry.uri] ?: "Reading folder contents…", { model.enter(location) })
            }
            if (files.isNotEmpty()) item(key = "file-heading", span = { GridItemSpan(maxLineSpan) }) {
                Text("Documents", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(files, key = { it.uri }) { entry -> ExploreFile(entry) { open(entry, FolderLocation(state.path.last().uri, state.path.joinToString(" / ") { it.name })) } }
        }
        if (state.message != null) item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.message!!, color = MaterialTheme.colorScheme.onErrorContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = model::refresh) { Text("Retry") }
                        TextButton(onClick = addFolder) { Text("Choose folder") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTile(name: String, summary: String, open: () -> Unit, remove: (() -> Unit)? = null) {
    var menu by remember { mutableStateOf(false) }
    OutlinedCard(onClick = open, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { AppIcon(AppSymbol.FOLDER) }
                }
                Spacer(Modifier.weight(1f))
                if (remove != null) Box {
                    IconButton(onClick = { menu = true }) { AppIcon(AppSymbol.MORE, "Options for $name") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Remove from Explore") }, onClick = { menu = false; remove() })
                    }
                } else AppIcon(AppSymbol.CHEVRON)
            }
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExploreFile(entry: FolderEntry, open: () -> Unit) {
    val context = LocalContext.current
    OutlinedCard(onClick = open, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FileBadge(entry.kind ?: "md")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.name, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${if (entry.kind == "pdf") "PDF" else "Markdown"} · ${Formatter.formatShortFileSize(context, entry.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExploreEmpty(title: String, message: String, action: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppIcon(AppSymbol.FOLDER, modifier = Modifier.size(36.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null) Button(onClick = action) { Text("Add a folder") }
    }
}
