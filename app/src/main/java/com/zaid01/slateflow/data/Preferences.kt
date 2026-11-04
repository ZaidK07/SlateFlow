package com.zaid01.slateflow.data

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore

internal val Application.preferences by preferencesDataStore("reader_preferences")

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

data class ReaderSettings(
    val dynamic: Boolean = false,
    val margin: Float = 32f,
    val font: Float = 18f,
    val spacing: Float = 1.45f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

enum class HistoryGrouping(val label: String) {
    DATE("Last read"), TYPE("File type"), FOLDER("Folder"), PROGRESS("Reading progress"), NONE("No groups")
}
enum class HistorySort(val label: String) { RECENT("Most recent"), NAME("Name A–Z"), PROGRESS("Reading progress") }
enum class HistoryLayout(val label: String) { GRID("Cards"), LIST("List") }
enum class FileFilter(val label: String) { ALL("All files"), PDF("PDF"), MARKDOWN("Markdown") }

data class DashboardPreferences(
    val grouping: HistoryGrouping = HistoryGrouping.DATE,
    val sort: HistorySort = HistorySort.RECENT,
    val layout: HistoryLayout = HistoryLayout.GRID,
    val filter: FileFilter = FileFilter.ALL,
    val showWelcome: Boolean = true,
    val showContinue: Boolean = true,
)

internal inline fun <reified T : Enum<T>> preferenceEnum(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback
