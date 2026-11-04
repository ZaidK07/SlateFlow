package com.zaid01.slateflow.dashboard

import com.zaid01.slateflow.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistorySectionsTest {
    private val today = LocalDate.of(2026, 11, 4)
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun document(name: String, daysAgo: Long = 0, kind: String = "md", progress: Float = 0f, folder: String = "") = Document().apply {
        this.name = name
        this.uri = name
        this.kind = kind
        this.progress = progress
        this.folderName = folder
        this.openedAt = today.minusDays(daysAgo).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    }

    @Test fun datesRespectLocalMidnightAndAlwaysKeepNewestGroupsFirst() {
        val documents = listOf(document("Old", 20), document("Today"), document("Recent", 5), document("Yesterday", 1))
        val result = historySections(documents, DashboardPreferences(), "", today, zone)
        assertEquals(listOf("Today", "Yesterday", "Last 7 days", "Earlier"), result.map { it.title })
        assertEquals("Today", result.first().documents.single().name)
    }

    @Test fun filterSearchAndNameSortApplyTogetherWithoutLosingFolderMatches() {
        val documents = listOf(document("Zebra", kind = "pdf", folder = "Study"), document("Alpha", kind = "pdf", folder = "Study"), document("Notes", folder = "Study"), document("Unrelated", kind = "pdf"))
        val preferences = DashboardPreferences(grouping = HistoryGrouping.NONE, sort = HistorySort.NAME, filter = FileFilter.PDF)
        assertEquals(listOf("Alpha", "Zebra"), historySections(documents, preferences, " study ", today, zone).single().documents.map { it.name })
    }

    @Test fun progressGroupsDistinguishUnreadCurrentAndFinishedDocuments() {
        val documents = listOf(document("Finished", progress = 1f), document("Unread"), document("Current", progress = 0.5f))
        val result = historySections(documents, DashboardPreferences(grouping = HistoryGrouping.PROGRESS), "", today, zone)
        assertEquals(listOf("In progress", "Not started", "Finished"), result.map { it.title })
    }

    @Test fun foldersWithTheSameNameRemainSeparateGroups() {
        val first = document("One", folder = "Notes").apply { folderUri = "content://one" }
        val second = document("Two", folder = "Notes").apply { folderUri = "content://two" }
        val groups = historySections(listOf(first, second), DashboardPreferences(grouping = HistoryGrouping.FOLDER), "", today, zone)
        assertEquals(2, groups.size)
        assertEquals(2, groups.map { it.key }.toSet().size)
    }

    @Test fun individuallyOpenedFilesHaveAnExplicitFolderGroup() {
        val result = historySections(listOf(document("One"), document("Two", folder = "Notes")), DashboardPreferences(grouping = HistoryGrouping.FOLDER), "", today, zone)
        assertEquals(setOf("Notes", "Opened individually"), result.map { it.title }.toSet())
    }
}
