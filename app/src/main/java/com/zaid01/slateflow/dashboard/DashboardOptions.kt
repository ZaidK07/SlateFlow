package com.zaid01.slateflow.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaid01.slateflow.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardOptions(preferences: DashboardPreferences, update: (DashboardPreferences) -> Unit, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your dashboard", style = MaterialTheme.typography.headlineSmall)
            Text("Group by", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryGrouping.entries.forEach { value -> FilterChip(preferences.grouping == value, { update(preferences.copy(grouping = value)) }, label = { Text(value.label) }) }
            }
            Text("Sort by", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistorySort.entries.forEach { value -> FilterChip(preferences.sort == value, { update(preferences.copy(sort = value)) }, label = { Text(value.label) }) }
            }
            Text("Display", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryLayout.entries.forEach { value -> FilterChip(preferences.layout == value, { update(preferences.copy(layout = value)) }, label = { Text(value.label) }) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Welcome card", Modifier.weight(1f))
                Switch(preferences.showWelcome, { update(preferences.copy(showWelcome = it)) })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Continue reading", Modifier.weight(1f))
                Switch(preferences.showContinue, { update(preferences.copy(showContinue = it)) })
            }
        }
    }
}
