package com.zaid01.slateflow.dashboard

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
import com.zaid01.slateflow.data.*
import com.zaid01.slateflow.ui.components.*
import kotlin.math.roundToInt

@Composable
internal fun DashboardScreen(
    documents: List<Document>,
    preferences: DashboardPreferences,
    customize: (DashboardPreferences) -> Unit,
    open: (Document) -> Unit,
    openFile: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var customizing by rememberSaveable { mutableStateOf(false) }
    val sections = remember(documents, preferences, search) { historySections(documents, preferences, search) }
    val columns = if (preferences.layout == HistoryLayout.LIST) GridCells.Fixed(1) else GridCells.Adaptive(280.dp)
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (preferences.showWelcome) item(key = "welcome", span = { GridItemSpan(maxLineSpan) }) {
            WelcomeCard(documents.size)
        }
        if (preferences.showContinue && documents.isNotEmpty()) item(key = "continue", span = { GridItemSpan(maxLineSpan) }) {
            ContinueCard(documents.maxBy { it.openedAt }) { open(documents.maxBy { it.openedAt }) }
        }
        item(key = "toolbar", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reading history", style = MaterialTheme.typography.titleLarge)
                        Text("${documents.size} ${if (documents.size == 1) "document" else "documents"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { customizing = true }) { AppIcon(AppSymbol.TUNE, "Customize dashboard") }
                }
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    placeholder = { Text("Search your reading history") },
                    leadingIcon = { AppIcon(AppSymbol.SEARCH) },
                    trailingIcon = { if (search.isNotEmpty()) TextButton(onClick = { search = "" }) { Text("Clear") } },
                    singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = nexusFieldColors(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileFilter.entries.forEach { filter ->
                        FilterChip(selected = preferences.filter == filter, onClick = { customize(preferences.copy(filter = filter)) }, label = { Text(filter.label) })
                    }
                }
            }
        }
        if (sections.isEmpty()) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(AppSymbol.DOCUMENT, modifier = Modifier.size(36.dp))
                Text(if (documents.isEmpty()) "Your next read starts here" else "No matching documents", style = MaterialTheme.typography.titleMedium)
                Text(if (documents.isEmpty()) "Open a file or find one in Explore." else "Try another search or file filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (documents.isEmpty()) Button(onClick = openFile) { Text("Open a file") }
            }
        }
        sections.forEach { section ->
            item(key = "section:${section.key}", span = { GridItemSpan(maxLineSpan) }) {
                Text("${section.title}  ·  ${section.documents.size}", Modifier.padding(top = 8.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(section.documents, key = { it.uri }) { document ->
                DocumentTile(document, preferences.layout == HistoryLayout.LIST) { open(document) }
            }
        }
    }
    if (customizing) DashboardOptions(preferences, customize) { customizing = false }
}

@Composable
private fun WelcomeCard(count: Int) {
    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { AppIcon(AppSymbol.DASHBOARD) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Welcome back", style = MaterialTheme.typography.titleMedium)
                Text(if (count == 0) "A fresh page, a little focus." else "Your reading, right where you left it.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ContinueCard(document: Document, open: () -> Unit) {
    Card(onClick = open, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FileBadge(document.kind)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Continue reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(document.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text("${(document.progress.coerceIn(0f, 1f) * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            AppIcon(AppSymbol.CHEVRON)
        }
    }
}
