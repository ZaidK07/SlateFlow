package com.zaid01.slateflow.explore

import com.zaid01.slateflow.data.FileFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FolderScannerTest {
    private fun folder(id: String, name: String = id) = FolderEntry(id, name, true)
    private fun document(id: String, kind: String, bytes: Long = 100) = FolderEntry(id, id, false, kind, size = bytes)
    private fun root(id: String = "root") = ScanRoot("grant:$id", FolderLocation(id, id))

    @Test fun findsEveryDocumentFolderAndHidesEmptyIntermediateFolders() = runBlocking {
        val tree = mapOf(
            "root" to listOf(folder("middle"), folder("empty"), document("guide", "pdf", 400)),
            "middle" to listOf(folder("deep")),
            "deep" to listOf(document("notes", "md", 120), document("second", "pdf", 200)),
            "empty" to emptyList(),
        )
        val result = FolderScanner({ tree.getValue(it.uri) }, { it }).scan(listOf(root()))
        assertEquals(listOf("root", "root/middle/deep"), result.folders.map { it.displayPath })
        assertEquals(4, result.visited)
        assertEquals(1, result.folders.last().pdfs)
        assertEquals(1, result.folders.last().markdown)
        assertEquals(320L, result.folders.last().documentBytes)
    }

    @Test fun deepTreesUseIterationInsteadOfTheCallStack() = runBlocking {
        val depth = 1500
        val result = FolderScanner({ location ->
            val index = location.uri.toInt()
            if (index == depth) listOf(document("last", "md")) else listOf(folder("${index + 1}"))
        }, { it }).scan(listOf(root("0")))
        assertEquals(depth + 1, result.visited)
        assertEquals("1500", result.folders.single().location.name)
    }

    @Test fun overlappingRootsAndProviderAliasesDoNotDuplicateFolders() = runBlocking {
        val tree = mapOf(
            "a:root" to listOf(folder("a:shared", "Shared")),
            "b:shared" to listOf(document("book", "pdf")),
        )
        val roots = listOf(ScanRoot("a", FolderLocation("a:root", "Parent")), ScanRoot("b", FolderLocation("b:shared", "Shared")))
        val result = FolderScanner({ tree.getValue(it.uri) }, { it.substringAfter(':') }).scan(roots)
        assertEquals(2, result.visited)
        assertEquals(1, result.folders.size)
    }

    @Test fun providerCyclesAreVisitedOnlyOnce() = runBlocking {
        val result = FolderScanner({ listOf(folder("root"), document("notes", "md")) }, { it }).scan(listOf(root()))
        assertEquals(1, result.visited)
        assertEquals(1, result.folders.size)
    }

    @Test fun anUnreadableBranchDoesNotHideReadableSiblings() = runBlocking {
        val result = FolderScanner({ location ->
            when (location.uri) {
                "root" -> listOf(folder("broken"), folder("readable"))
                "broken" -> throw SecurityException("Access revoked")
                else -> listOf(document("readme", "md"))
            }
        }, { it }).scan(listOf(root()))
        assertEquals(1, result.skipped)
        assertEquals("root/readable", result.folders.single().displayPath)
    }

    @Test fun cancellationPropagatesInsteadOfBeingReportedAsAnUnreadableFolder() = runBlocking {
        try {
            FolderScanner({ throw CancellationException("Stopped") }, { it }).scan(listOf(root()))
            fail("Cancellation must propagate")
        } catch (expected: CancellationException) { assertEquals("Stopped", expected.message) }
    }

    @Test fun aBoundedTraversalReportsItsLimitInsteadOfClaimingACompleteScan() = runBlocking {
        val result = FolderScanner({ location ->
            if (location.uri == "root") (1..10).map { folder("$it") } else emptyList()
        }, { it }, folderLimit = 3).scan(listOf(root()))
        assertEquals(3, result.visited)
        assertTrue(result.limitReached)
    }

    @Test fun sizeTotalsIgnoreUnsupportedFilesAndFlagUnknownSizes() = runBlocking {
        val result = FolderScanner({ listOf(
            document("known", "pdf", 350), document("image", "image", 900),
            document("unknown", "md", 999).copy(sizeKnown = false),
            folder("root", "A directory.md").copy(kind = "md"),
        ) }, { it }).scan(listOf(root()))
        assertEquals(350L, result.folders.single().documentBytes)
        assertEquals(1, result.folders.single().unknownSizes)
        assertEquals(1, result.folders.single().markdown)
    }

    @Test fun folderSearchTypeFiltersAndSizeSortingWorkTogether() {
        val folders = listOf(
            IndexedFolder("g", FolderLocation("a", "Math"), "Study/Math", 3, 0, 400),
            IndexedFolder("g", FolderLocation("b", "Notes"), "Study/Science/Notes", 1, 2, 900),
            IndexedFolder("g", FolderLocation("c", "Notes"), "Books/Notes", 0, 4, 1400),
        )
        val result = displayedFolders(folders, "study", FileFilter.PDF, ExploreSort.SIZE)
        assertEquals(listOf("b", "a"), result.map { it.location.uri })
        assertEquals(3, displayedFolders(folders, "", FileFilter.ALL, ExploreSort.DOCUMENTS).size)
    }
}
