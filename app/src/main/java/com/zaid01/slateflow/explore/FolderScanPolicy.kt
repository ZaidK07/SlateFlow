package com.zaid01.slateflow.explore

internal data class FolderScanPlan(val grantsToScan: Set<String>, val retainPrevious: Boolean, val reuseIndex: Boolean)

/** Opening Explore never rescans an unchanged, already indexed set of locations. */
internal fun folderScanPlan(grants: Set<String>, indexedGrants: Set<String>?): FolderScanPlan = when {
    indexedGrants == null -> FolderScanPlan(grants, retainPrevious = false, reuseIndex = false)
    grants == indexedGrants -> FolderScanPlan(emptySet(), retainPrevious = true, reuseIndex = true)
    indexedGrants.any { it !in grants } -> FolderScanPlan(grants, retainPrevious = false, reuseIndex = false)
    else -> FolderScanPlan(grants - indexedGrants, retainPrevious = true, reuseIndex = false)
}

internal fun mergeIndexedFolders(previous: List<IndexedFolder>, current: List<IndexedFolder>, identity: (String) -> String): List<IndexedFolder> =
    (previous + current).distinctBy { identity(it.location.uri) }.sortedBy { it.displayPath.lowercase(java.util.Locale.ROOT) }
