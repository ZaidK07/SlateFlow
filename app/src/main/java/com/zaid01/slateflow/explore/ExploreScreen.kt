package com.zaid01.slateflow.explore

import android.net.Uri
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaid01.slateflow.data.Document
import com.zaid01.slateflow.data.FileFilter
import com.zaid01.slateflow.ui.components.*

@Composable
internal fun ExploreScreen(
    model: ExploreModel,
    recentDocuments: List<Document> = emptyList(),
    addFolder: () -> Unit,
    onMarkRead: (Uri, Boolean, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onMarkFolderRead: (String, Boolean, List<IndexedFile>) -> Unit = { _, _, _ -> },
    open: (FolderEntry, FolderLocation) -> Unit,
    openIndexedFile: (IndexedFile) -> Unit = {},
) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    val display by model.display.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FileFilter.ALL) }
    var options by rememberSaveable { mutableStateOf(false) }
    var locations by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.path) { search = "" }
    val inFolder = state.path.isNotEmpty()

    val readUris = remember(recentDocuments) {
        recentDocuments.filter { it.isRead || it.progress >= 0.95f }.flatMap { listOf(it.uri, it.sourceUri) }.filter { it.isNotBlank() }.toSet()
    }
    val readFolderUris = remember(state.folders, state.files, readUris) {
        state.folders.filter { folder ->
            val folderFiles = state.files.filter { it.folderUri == folder.location.uri }
            folderFiles.isNotEmpty() && folderFiles.all { it.uri in readUris }
        }.map { it.location.uri }.toSet()
    }

    val folders = remember(state.folders, search, filter, display.sort, display.onlyUnread, readFolderUris) {
        displayedFolders(state.folders, search, filter, display.sort, display.onlyUnread, readFolderUris)
    }
    val allFiles = remember(state.files, search, filter, display.sort, display.onlyUnread, readUris) {
        displayedFiles(state.files, search, filter, display.sort, display.onlyUnread, readUris)
    }
    val files = remember(state.entries, search, filter, display.onlyUnread, readUris) {
        state.entries.filter { entry ->
            !entry.isFolder &&
            (!display.onlyUnread || entry.uri !in readUris) &&
            entry.name.contains(search.trim(), ignoreCase = true) &&
            (filter == FileFilter.ALL || (filter == FileFilter.PDF && entry.kind == "pdf") || (filter == FileFilter.MARKDOWN && entry.kind == "md"))
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (inFolder) state.path.last().name.substringAfterLast('/')
                            else if (display.viewMode == ExploreViewMode.FILES) "All files"
                            else "Your folders",
                            style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (inFolder) "Documents in this folder"
                            else if (display.viewMode == ExploreViewMode.FILES) "Every PDF and Markdown file discovered across all locations."
                            else "Every folder with PDFs or Markdown, at any depth.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = model::refresh, enabled = !state.scanning) { AppIcon(AppSymbol.REFRESH, "Refresh folders") }
                    IconButton(onClick = { options = true }) { AppIcon(AppSymbol.TUNE, "Explore display options") }
                    FilledTonalIconButton(onClick = addFolder) { AppIcon(AppSymbol.ADD, "Add folder") }
                }

                if (inFolder) {
                    TextButton(onClick = { model.breadcrumb(-1) }) { AppIcon(AppSymbol.BACK); Spacer(Modifier.width(8.dp)); Text("All discovered folders") }
                    Text(state.path.last().name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (!state.hasStoragePermission) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Automatic storage discovery", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Allow all files access to automatically find all PDF and Markdown documents across your tablet without picking folders.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                FilledTonalButton(onClick = { model.requestStoragePermission(context) }) {
                                    Text("Allow access")
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = { locations = true },
                            label = { Text("${state.roots.size} scan ${if (state.roots.size == 1) "location" else "locations"}") },
                            leadingIcon = { AppIcon(AppSymbol.FOLDER, modifier = Modifier.size(18.dp)) }
                        )
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = display.viewMode == ExploreViewMode.FOLDERS,
                                onClick = { model.updateDisplay(display.copy(viewMode = ExploreViewMode.FOLDERS)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {},
                            ) { Text("Folders") }
                            SegmentedButton(
                                selected = display.viewMode == ExploreViewMode.FILES,
                                onClick = { model.updateDisplay(display.copy(viewMode = ExploreViewMode.FILES)) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {},
                            ) { Text("All files") }
                        }
                    }

                    if (state.scanning) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("Scanning subfolders · ${state.visited} checked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (state.scannedAt != null) {
                        Text(
                            "${state.folders.size} ${if (state.folders.size == 1) "folder" else "folders"} (${state.files.size} files) · Updated ${DateUtils.getRelativeTimeSpanString(state.scannedAt!!)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.scanMessage != null) {
                        Text(state.scanMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (state.roots.isNotEmpty()) {
                    OutlinedTextField(
                        search, { search = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (inFolder) "Search this folder" else if (display.viewMode == ExploreViewMode.FILES) "Search files" else "Search folder names or paths") },
                        leadingIcon = { AppIcon(AppSymbol.SEARCH) },
                        trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Clear") } },
                        shape = RoundedCornerShape(12.dp), singleLine = true, colors = nexusFieldColors(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FileFilter.entries.forEach { type ->
                            FilterChip(filter == type, { filter = type }, label = { Text(type.label) })
                        }
                        FilterChip(
                            selected = display.onlyUnread,
                            onClick = { model.updateDisplay(display.copy(onlyUnread = !display.onlyUnread)) },
                            leadingIcon = { if (display.onlyUnread) AppIcon(AppSymbol.CHECK, modifier = Modifier.size(16.dp)) },
                            label = { Text("Unread only") }
                        )
                    }
                }
            }
        }

        when {
            state.loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
            }
            !inFolder && display.viewMode == ExploreViewMode.FILES -> {
                if (state.roots.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    ExploreEmpty("Discover your reading files", "Choose a location to find every PDF and Markdown document.", addFolder)
                } else if (allFiles.isEmpty() && !state.scanning) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    ExploreEmpty("No matching files", if (search.isNotBlank() || filter != FileFilter.ALL || display.onlyUnread) "Try adjusting your search or unread filter." else "No files found. Add another location or refresh.")
                }
                items(allFiles, key = { it.uri }) { file ->
                    val isRead = file.uri in readUris
                    IndexedFileTile(
                        file = file,
                        isRead = isRead,
                        open = { openIndexedFile(file) },
                        onToggleRead = {
                            onMarkRead(file.uri.toUri(), !isRead, file.name, file.kind, file.folderName, file.folderUri)
                        }
                    )
                }
            }
            !inFolder -> {
                if (state.roots.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    ExploreEmpty("Discover your reading folders", "Choose a location. We’ll find every PDF and Markdown folder inside it, including nested folders.", addFolder)
                } else if (folders.isEmpty() && !state.scanning) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    ExploreEmpty("No matching folders", if (search.isNotBlank() || filter != FileFilter.ALL || display.onlyUnread) "Try a different search or filter." else "No PDFs or Markdown files found. Add another location or refresh.")
                }
                items(folders, key = { it.location.uri }) { folder ->
                    val isFolderRead = folder.location.uri in readFolderUris
                    IndexedFolderTile(
                        folder = folder,
                        settings = display,
                        isRead = isFolderRead,
                        open = { model.enterIndexed(folder) },
                        onToggleRead = {
                            val folderFiles = state.files.filter { it.folderUri == folder.location.uri }
                            onMarkFolderRead(folder.location.uri, !isFolderRead, folderFiles)
                        }
                    )
                }
            }
            else -> {
                if (files.isEmpty() && state.message == null) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    ExploreEmpty("No matching documents", "Try another search or filter, or refresh if files have changed.")
                }
                items(files, key = { it.uri }) { entry ->
                    val isRead = entry.uri in readUris
                    ExploreFile(
                        entry = entry,
                        isRead = isRead,
                        open = { open(entry, state.path.last()) },
                        onToggleRead = {
                            onMarkRead(entry.uri.toUri(), !isRead, entry.name, entry.kind ?: "md", state.path.last().name, state.path.last().uri)
                        }
                    )
                }
            }
        }
        if (state.message != null) item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.message!!, color = MaterialTheme.colorScheme.onErrorContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = model::refresh) { Text("Retry") }
                        TextButton(onClick = addFolder) { Text("Choose location") }
                    }
                }
            }
        }
    }
    if (options) ExploreOptions(display, model::updateDisplay) { options = false }
    if (locations) ScanLocations(state.roots, model::removeRoot, addFolder) { locations = false }
}

@Composable
private fun IndexedFolderTile(
    folder: IndexedFolder,
    settings: ExploreDisplaySettings,
    isRead: Boolean = false,
    open: () -> Unit,
    onToggleRead: () -> Unit = {},
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    OutlinedCard(
        onClick = open,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRead) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isRead) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        AppIcon(if (isRead) AppSymbol.CHECK_CIRCLE else AppSymbol.FOLDER)
                    }
                }
                Text(folder.location.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box {
                    IconButton(onClick = { menuOpen = true }) { AppIcon(AppSymbol.MORE, "Folder actions") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (isRead) "Mark all as unread" else "Mark all as read") },
                            onClick = { menuOpen = false; onToggleRead() },
                            leadingIcon = { AppIcon(if (isRead) AppSymbol.MINUS else AppSymbol.CHECK) }
                        )
                    }
                }
            }
            if (settings.showPath) Text(folder.displayPath, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            if (settings.showCounts) Text(folder.summary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            if (settings.showSize) Text(
                if (folder.unknownSizes == 0) Formatter.formatShortFileSize(context, folder.documentBytes)
                else if (folder.documentBytes == 0L) "Size unavailable"
                else "At least ${Formatter.formatShortFileSize(context, folder.documentBytes)} · Size incomplete",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IndexedFileTile(
    file: IndexedFile,
    isRead: Boolean = false,
    open: () -> Unit,
    onToggleRead: () -> Unit = {},
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    OutlinedCard(
        onClick = open,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FileBadge(file.kind)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall, minLines = 1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${file.folderName.substringAfterLast('/')} · ${if (file.sizeKnown) Formatter.formatShortFileSize(context, file.size) else "Size unavailable"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (isRead) {
                AppIcon(AppSymbol.CHECK_CIRCLE, "Read", modifier = Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { AppIcon(AppSymbol.MORE, "Actions") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isRead) "Mark as unread" else "Mark as read") },
                        onClick = { menuOpen = false; onToggleRead() },
                        leadingIcon = { AppIcon(if (isRead) AppSymbol.MINUS else AppSymbol.CHECK) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreFile(
    entry: FolderEntry,
    isRead: Boolean = false,
    open: () -> Unit,
    onToggleRead: () -> Unit = {},
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    OutlinedCard(
        onClick = open,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FileBadge(entry.kind ?: "md")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.name, style = MaterialTheme.typography.titleSmall, minLines = 1, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${if (entry.kind == "pdf") "PDF" else "Markdown"} · ${if (entry.sizeKnown) Formatter.formatShortFileSize(context, entry.size) else "Size unavailable"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isRead) {
                AppIcon(AppSymbol.CHECK_CIRCLE, "Read", modifier = Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { AppIcon(AppSymbol.MORE, "Actions") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isRead) "Mark as unread" else "Mark as read") },
                        onClick = { menuOpen = false; onToggleRead() },
                        leadingIcon = { AppIcon(if (isRead) AppSymbol.MINUS else AppSymbol.CHECK) }
                    )
                }
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
        if (action != null) Button(onClick = action) { Text("Add a location") }
    }
}
