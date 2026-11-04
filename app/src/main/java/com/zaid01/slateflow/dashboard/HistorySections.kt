package com.zaid01.slateflow.dashboard

import com.zaid01.slateflow.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

internal data class HistorySection(val title: String, val documents: List<Document>, val key: String = title)

internal fun historySections(
    documents: List<Document>,
    preferences: DashboardPreferences,
    search: String,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): List<HistorySection> {
    val filtered = documents.filter { document ->
        val matchesType = when (preferences.filter) {
            FileFilter.ALL -> true
            FileFilter.PDF -> document.kind == "pdf"
            FileFilter.MARKDOWN -> document.kind == "md"
        }
        matchesType && (search.isBlank() || document.name.contains(search.trim(), ignoreCase = true) || document.folderName.contains(search.trim(), ignoreCase = true))
    }
    val sorted = when (preferences.sort) {
        HistorySort.RECENT -> filtered.sortedByDescending { it.openedAt }
        HistorySort.NAME -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
        HistorySort.PROGRESS -> filtered.sortedByDescending { it.progress }
    }
    val groups = sorted.groupBy { document ->
        when (preferences.grouping) {
            HistoryGrouping.NONE -> "Reading history"
            HistoryGrouping.TYPE -> if (document.kind == "pdf") "PDF documents" else "Markdown documents"
            HistoryGrouping.FOLDER -> document.folderUri.ifBlank { document.folderName.ifBlank { "__individual__" } }
            HistoryGrouping.PROGRESS -> when {
                document.progress >= 0.995f -> "Finished"
                document.progress > 0f -> "In progress"
                else -> "Not started"
            }
            HistoryGrouping.DATE -> {
                val date = Instant.ofEpochMilli(document.openedAt).atZone(zone).toLocalDate()
                when {
                    date >= today -> "Today"
                    date == today.minusDays(1) -> "Yesterday"
                    date >= today.minusDays(6) -> "Last 7 days"
                    else -> "Earlier"
                }
            }
        }
    }
    val ordered = when (preferences.grouping) {
        HistoryGrouping.DATE -> listOf("Today", "Yesterday", "Last 7 days", "Earlier").filter(groups::containsKey)
        HistoryGrouping.PROGRESS -> listOf("In progress", "Not started", "Finished").filter(groups::containsKey)
        HistoryGrouping.FOLDER -> groups.keys.sortedWith(compareBy<String> { groups.getValue(it).first().folderName.lowercase(Locale.ROOT) }.thenBy { it })
        HistoryGrouping.TYPE -> groups.keys.sortedBy { it.lowercase(Locale.ROOT) }
        HistoryGrouping.NONE -> groups.keys.toList()
    }
    return ordered.map { key ->
        val title = if (preferences.grouping == HistoryGrouping.FOLDER) {
            groups.getValue(key).first().folderName.ifBlank { "Opened individually" }
        } else key
        HistorySection(title, groups.getValue(key), key)
    }
}
