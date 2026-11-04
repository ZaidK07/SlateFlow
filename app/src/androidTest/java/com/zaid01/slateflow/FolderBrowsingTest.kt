package com.zaid01.slateflow

import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.explore.FolderLocation
import com.zaid01.slateflow.explore.FolderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FolderBrowsingTest {
    private val authority = "com.zaid01.slateflow.test.folders"
    private val tree get() = DocumentsContract.buildTreeDocumentUri(authority, "root")
    private val repository get() = FolderRepository(InstrumentationRegistry.getInstrumentation().targetContext.contentResolver)

    @Test fun folderQueriesKeepSupportedFilesAndCountOnlyDirectChildren() = runBlocking {
        val root = repository.root(tree)
        assertEquals("Reading", root.name)
        val children = repository.children(root)
        assertEquals(listOf("Empty", "Notes", "Guide.PDF", "Readme.md"), children.map { it.name })
        assertTrue(children.none { it.name == "Photo.jpg" || it.name == "Chapter.markdown" })
        val summary = repository.summary(root)
        assertEquals(2, summary.folders)
        assertEquals(1, summary.pdfs)
        assertEquals(1, summary.markdown)
        assertTrue(children.all { DocumentsContract.isTreeUri(android.net.Uri.parse(it.uri)) })
    }

    @Test fun nestedAndEmptyFoldersHaveIndependentContents() = runBlocking {
        val notes = FolderLocation(DocumentsContract.buildDocumentUriUsingTree(tree, "notes").toString(), "Notes")
        assertEquals("Chapter.markdown", repository.children(notes).single().name)
        val empty = FolderLocation(DocumentsContract.buildDocumentUriUsingTree(tree, "empty").toString(), "Empty")
        assertTrue(repository.children(empty).isEmpty())
        assertEquals(0, repository.summary(empty).pdfs)
    }
}
