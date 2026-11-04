package com.zaid01.slateflow.explore

import org.junit.Assert.*
import org.junit.Test

class FolderScanPolicyTest {
    @Test fun firstUseScansAllChosenLocations() {
        val plan = folderScanPlan(setOf("books", "study"), null)
        assertEquals(setOf("books", "study"), plan.grantsToScan)
        assertFalse(plan.reuseIndex)
    }

    @Test fun reopeningAnUnchangedIndexNeverRequestsAnotherScan() {
        val plan = folderScanPlan(setOf("books", "study"), setOf("study", "books"))
        assertTrue(plan.reuseIndex)
        assertTrue(plan.grantsToScan.isEmpty())
    }

    @Test fun addingALocationOnlyScansTheNewTreeAndRetainsExistingRows() {
        val plan = folderScanPlan(setOf("books", "study"), setOf("books"))
        assertEquals(setOf("study"), plan.grantsToScan)
        assertTrue(plan.retainPrevious)
    }

    @Test fun removingLocationsReconcilesRemainingCoverageInsteadOfKeepingRevokedRows() {
        val plan = folderScanPlan(setOf("study"), setOf("books", "study"))
        assertEquals(setOf("study"), plan.grantsToScan)
        assertFalse(plan.retainPrevious)
        assertTrue(folderScanPlan(emptySet(), setOf("study")).grantsToScan.isEmpty())
    }

    @Test fun incrementallyScannedOverlappingLocationsDoNotDuplicateAnIndexedFolder() {
        val previous = IndexedFolder("a", FolderLocation("a:shared", "Notes"), "Study/Notes", 1, 0)
        val current = IndexedFolder("b", FolderLocation("b:shared", "Notes"), "Notes", 1, 0)
        val merged = mergeIndexedFolders(listOf(previous), listOf(current)) { it.substringAfter(':') }
        assertEquals(listOf(previous), merged)
    }
}
