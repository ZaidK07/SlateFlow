package com.zaid01.slateflow.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExploreOptions(settings: ExploreDisplaySettings, update: (ExploreDisplaySettings) -> Unit, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Explore display", style = MaterialTheme.typography.headlineSmall)
            DisplaySwitch("Folder path", settings.showPath) { update(settings.copy(showPath = it)) }
            DisplaySwitch("PDF and Markdown counts", settings.showCounts) { update(settings.copy(showCounts = it)) }
            DisplaySwitch("Document size", settings.showSize) { update(settings.copy(showSize = it)) }
            Text("Size totals the PDFs and Markdown files directly inside each folder.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Sort folders by", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExploreSort.entries.forEach { sort -> FilterChip(settings.sort == sort, { update(settings.copy(sort = sort)) }, label = { Text(sort.label) }) }
            }
        }
    }
}

@Composable
private fun DisplaySwitch(label: String, checked: Boolean, update: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = update), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.weight(1f))
        Switch(checked, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScanLocations(roots: List<RootFolder>, remove: (String) -> Unit, add: () -> Unit, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Scan locations", style = MaterialTheme.typography.headlineSmall)
            Text("Locations are scanned when added. Use Refresh to check for changes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            roots.forEach { root ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(root.location?.name ?: "Unavailable location", style = MaterialTheme.typography.titleMedium)
                        if (root.unavailable) Text("Access lost · Add this location again", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { remove(root.grantUri) }) { Text("Remove") }
                }
            }
            Button(onClick = { dismiss(); add() }) { Text("Add location") }
        }
    }
}
