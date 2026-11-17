package com.zaid01.slateflow.explore

import com.zaid01.slateflow.data.FileFilter
import java.util.Locale

enum class ExploreSort(val label: String) {
    PATH("Folder path"),
    NAME("Name A–Z"),
    DATE("Recently modified"),
    SIZE("Largest size"),
    DOCUMENTS("Most documents")
}

enum class ExploreViewMode(val label: String) {
    FOLDERS("Folders"),
    FILES("All files")
}

internal data class ExploreDisplaySettings(
    val showPath: Boolean = true,
    val showSize: Boolean = true,
    val showCounts: Boolean = true,
    val sort: ExploreSort = ExploreSort.PATH,
    val viewMode: ExploreViewMode = ExploreViewMode.FOLDERS,
    val onlyUnread: Boolean = false,
)

internal fun displayedFolders(
    folders: List<IndexedFolder>,
    search: String,
    filter: FileFilter,
    sort: ExploreSort,
    onlyUnread: Boolean = false,
    readFolderUris: Set<String> = emptySet(),
): List<IndexedFolder> {
    val matching = folders.filter { folder ->
        (!onlyUnread || folder.location.uri !in readFolderUris) &&
        folder.displayPath.contains(search.trim(), ignoreCase = true) && when (filter) {
            FileFilter.ALL -> true
            FileFilter.PDF -> folder.pdfs > 0
            FileFilter.MARKDOWN -> folder.markdown > 0
        }
    }
    val pathOrder = compareBy<IndexedFolder> { it.displayPath.lowercase(Locale.ROOT) }.thenBy { it.location.uri }
    return when (sort) {
        ExploreSort.PATH -> matching.sortedWith(pathOrder)
        ExploreSort.NAME -> matching.sortedWith(compareBy<IndexedFolder> { it.location.name.lowercase(Locale.ROOT) }.then(pathOrder))
        ExploreSort.DATE -> matching.sortedWith(pathOrder)
        ExploreSort.SIZE -> matching.sortedWith(compareByDescending<IndexedFolder> { it.documentBytes }.then(pathOrder))
        ExploreSort.DOCUMENTS -> matching.sortedWith(compareByDescending<IndexedFolder> { it.pdfs + it.markdown }.then(pathOrder))
    }
}

internal fun displayedFiles(
    files: List<IndexedFile>,
    search: String,
    filter: FileFilter,
    sort: ExploreSort,
    onlyUnread: Boolean = false,
    readUris: Set<String> = emptySet(),
): List<IndexedFile> {
    val matching = files.filter { file ->
        (!onlyUnread || file.uri !in readUris) &&
        (file.name.contains(search.trim(), ignoreCase = true) || file.folderName.contains(search.trim(), ignoreCase = true)) &&
        when (filter) {
            FileFilter.ALL -> true
            FileFilter.PDF -> file.kind == "pdf"
            FileFilter.MARKDOWN -> file.kind == "md"
        }
    }
    val nameOrder = compareBy<IndexedFile> { it.name.lowercase(Locale.ROOT) }.thenBy { it.uri }
    return when (sort) {
        ExploreSort.NAME -> matching.sortedWith(nameOrder)
        ExploreSort.DATE -> matching.sortedWith(compareByDescending<IndexedFile> { it.modifiedAt }.then(nameOrder))
        ExploreSort.SIZE -> matching.sortedWith(compareByDescending<IndexedFile> { it.size }.then(nameOrder))
        ExploreSort.PATH, ExploreSort.DOCUMENTS -> matching.sortedWith(compareBy<IndexedFile> { it.folderName.lowercase(Locale.ROOT) }.then(nameOrder))
    }
}
