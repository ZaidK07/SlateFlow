package com.zaid01.slateflow.explore

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale

internal data class FolderLocation(val uri: String, val name: String)
internal data class FolderEntry(
    val uri: String,
    val name: String,
    val isFolder: Boolean,
    val kind: String? = null,
    val modifiedAt: Long = 0,
    val size: Long = 0,
    val sizeKnown: Boolean = true,
)
internal data class FolderSummary(val folders: Int = 0, val pdfs: Int = 0, val markdown: Int = 0) {
    val label: String get() = buildList {
        if (folders > 0) add("$folders ${if (folders == 1) "folder" else "folders"}")
        if (pdfs > 0) add("$pdfs ${if (pdfs == 1) "PDF" else "PDFs"}")
        if (markdown > 0) add("$markdown Markdown")
    }.ifEmpty { listOf("No PDF or Markdown files") }.joinToString(" · ")
}

/** Reads one directory per provider query; recursive discovery is owned by FolderScanner. */
internal class FolderRepository(private val resolver: ContentResolver) {
    suspend fun root(uri: Uri): FolderLocation = withContext(Dispatchers.IO) {
        val document = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val name = resolver.query(document, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: error("This folder is no longer available. Add it again to restore access.")
        FolderLocation(document.toString(), name)
    }

    suspend fun children(location: FolderLocation, sort: Boolean = true): List<FolderEntry> = withContext(Dispatchers.IO) {
        val parent = Uri.parse(location.uri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        val entries = mutableListOf<FolderEntry>()
        resolver.query(children, columns, null, null, null)?.use { cursor ->
            var visited = 0
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                check(++visited <= 20_000) { "This folder has more than 20,000 items. Choose a smaller folder to browse." }
                val mime = cursor.getString(2).orEmpty()
                val name = cursor.getString(1).orEmpty()
                val folder = mime == DocumentsContract.Document.MIME_TYPE_DIR
                val kind = supportedKind(name, mime)
                if (folder || kind != null) {
                    entries += FolderEntry(
                        uri = DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0)).toString(),
                        name = name, isFolder = folder, kind = kind,
                        modifiedAt = cursor.getLong(3), size = cursor.getLong(4), sizeKnown = !cursor.isNull(4),
                    )
                }
            }
        } ?: error("Could not read this folder. Check its access permission and try again.")
        if (!sort) return@withContext entries
        entries.sortedWith(compareByDescending<FolderEntry> { it.isFolder }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    suspend fun summary(location: FolderLocation): FolderSummary {
        val entries = children(location)
        return FolderSummary(entries.count { it.isFolder }, entries.count { !it.isFolder && it.kind == "pdf" }, entries.count { !it.isFolder && it.kind == "md" })
    }
}

internal fun supportedKind(name: String, mime: String): String? = when {
    mime == "application/pdf" || name.endsWith(".pdf", true) -> "pdf"
    name.endsWith(".md", true) || name.endsWith(".markdown", true) || mime == "text/markdown" || mime == "text/plain" -> "md"
    else -> null
}
