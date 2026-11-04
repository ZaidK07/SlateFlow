package com.zaid01.slateflow

import android.provider.DocumentsContract
import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.explore.FolderLocation
import com.zaid01.slateflow.explore.FolderRepository
import com.zaid01.slateflow.explore.FolderScanner
import com.zaid01.slateflow.explore.ScanRoot
import com.zaid01.slateflow.data.DocumentIdentity
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

    @Test fun recursiveScanFindsNestedDocumentFoldersUsingProviderMetadata() = runBlocking {
        val source = repository
        val root = source.root(tree)
        val scanner = FolderScanner({ source.children(it, sort = false) }, { DocumentIdentity.key(android.net.Uri.parse(it)) })
        val result = scanner.scan(listOf(ScanRoot(tree.toString(), root)))
        assertEquals(listOf("Reading", "Reading/Notes"), result.folders.map { it.displayPath })
        assertEquals(3, result.visited)
        assertEquals(320L, result.folders.first().documentBytes)
        assertEquals(80L, result.folders.last().documentBytes)
        assertEquals(0, result.skipped)
    }

    @Test fun nestedAndEmptyFoldersHaveIndependentContents() = runBlocking {
        val notes = FolderLocation(DocumentsContract.buildDocumentUriUsingTree(tree, "notes").toString(), "Notes")
        assertEquals("Chapter.markdown", repository.children(notes).single().name)
        val empty = FolderLocation(DocumentsContract.buildDocumentUriUsingTree(tree, "empty").toString(), "Empty")
        assertTrue(repository.children(empty).isEmpty())
        assertEquals(0, repository.summary(empty).pdfs)
    }
}
