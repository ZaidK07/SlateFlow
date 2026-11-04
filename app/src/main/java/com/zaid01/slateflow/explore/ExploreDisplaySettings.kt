package com.zaid01.slateflow.explore

enum class ExploreSort(val label: String) { PATH("Folder path"), NAME("Folder name"), SIZE("Largest first"), DOCUMENTS("Most documents") }

internal data class ExploreDisplaySettings(
    val showPath: Boolean = true,
    val showSize: Boolean = true,
    val showCounts: Boolean = true,
    val sort: ExploreSort = ExploreSort.PATH,
)

internal fun displayedFolders(folders: List<IndexedFolder>, search: String, filter: com.zaid01.slateflow.data.FileFilter, sort: ExploreSort): List<IndexedFolder> {
    val matching = folders.filter { folder ->
        folder.displayPath.contains(search.trim(), ignoreCase = true) && when (filter) {
            com.zaid01.slateflow.data.FileFilter.ALL -> true
            com.zaid01.slateflow.data.FileFilter.PDF -> folder.pdfs > 0
            com.zaid01.slateflow.data.FileFilter.MARKDOWN -> folder.markdown > 0
        }
    }
    val pathOrder = compareBy<IndexedFolder> { it.displayPath.lowercase(java.util.Locale.ROOT) }.thenBy { it.location.uri }
    return when (sort) {
        ExploreSort.PATH -> matching.sortedWith(pathOrder)
        ExploreSort.NAME -> matching.sortedWith(compareBy<IndexedFolder> { it.location.name.lowercase(java.util.Locale.ROOT) }.then(pathOrder))
        ExploreSort.SIZE -> matching.sortedWith(compareByDescending<IndexedFolder> { it.documentBytes }.then(pathOrder))
        ExploreSort.DOCUMENTS -> matching.sortedWith(compareByDescending<IndexedFolder> { it.pdfs + it.markdown }.then(pathOrder))
    }
}
