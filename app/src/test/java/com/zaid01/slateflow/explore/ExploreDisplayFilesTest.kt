package com.zaid01.slateflow.explore

import com.zaid01.slateflow.data.FileFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreDisplayFilesTest {
    private val sampleFiles = listOf(
        IndexedFile("uri://b.pdf", "Book B.pdf", "pdf", modifiedAt = 1000L, size = 5000L, folderName = "Folder 1"),
        IndexedFile("uri://a.md", "Doc A.md", "md", modifiedAt = 3000L, size = 1000L, folderName = "Folder 2"),
        IndexedFile("uri://c.pdf", "Paper C.pdf", "pdf", modifiedAt = 2000L, size = 9000L, folderName = "Folder 1"),
    )

    @Test
    fun sortsByName() {
        val sorted = displayedFiles(sampleFiles, "", FileFilter.ALL, ExploreSort.NAME)
        assertEquals(listOf("Book B.pdf", "Doc A.md", "Paper C.pdf"), sorted.map { it.name })
    }

    @Test
    fun sortsByDateModifiedDescending() {
        val sorted = displayedFiles(sampleFiles, "", FileFilter.ALL, ExploreSort.DATE)
        assertEquals(listOf("Doc A.md", "Paper C.pdf", "Book B.pdf"), sorted.map { it.name })
    }

    @Test
    fun sortsBySizeDescending() {
        val sorted = displayedFiles(sampleFiles, "", FileFilter.ALL, ExploreSort.SIZE)
        assertEquals(listOf("Paper C.pdf", "Book B.pdf", "Doc A.md"), sorted.map { it.name })
    }

    @Test
    fun filtersByFileType() {
        val pdfs = displayedFiles(sampleFiles, "", FileFilter.PDF, ExploreSort.NAME)
        assertEquals(2, pdfs.size)
        assertTrue(pdfs.all { it.kind == "pdf" })

        val mds = displayedFiles(sampleFiles, "", FileFilter.MARKDOWN, ExploreSort.NAME)
        assertEquals(1, mds.size)
        assertEquals("Doc A.md", mds[0].name)
    }

    @Test
    fun filtersUnreadOnly() {
        val readUris = setOf("uri://b.pdf")
        val unread = displayedFiles(sampleFiles, "", FileFilter.ALL, ExploreSort.NAME, onlyUnread = true, readUris = readUris)
        assertEquals(2, unread.size)
        assertTrue(unread.none { it.uri in readUris })
    }

    @Test
    fun searchesByNameOrFolder() {
        val searchDoc = displayedFiles(sampleFiles, "Doc", FileFilter.ALL, ExploreSort.NAME)
        assertEquals(1, searchDoc.size)
        assertEquals("Doc A.md", searchDoc[0].name)

        val searchFolder = displayedFiles(sampleFiles, "Folder 1", FileFilter.ALL, ExploreSort.NAME)
        assertEquals(2, searchFolder.size)
    }
}
