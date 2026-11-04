package com.zaid01.slateflow.explore

import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal data class SavedFolderIndex(
    val roots: Set<String>, val folders: List<IndexedFolder>, val scannedAt: Long,
    val skipped: Int = 0, val limitReached: Boolean = false,
)

/** Only folder metadata is cached. Interrupted writes keep the previous snapshot intact. */
internal class FolderIndexStore(file: File) {
    private val file = AtomicFile(file)
    private val mutex = Mutex()

    suspend fun load(roots: Set<String>): SavedFolderIndex? = loadAny()?.takeIf { it.roots == roots }

    suspend fun loadAny(): SavedFolderIndex? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val json = JSONObject(file.openRead().bufferedReader().use { it.readText() })
                if (json.getInt("version") != 1) return@withLock null
                val values = json.getJSONArray("roots")
                val savedRoots = (0 until values.length()).map { values.getString(it) }.toSet()
                val rows = json.getJSONArray("folders")
                val folders = (0 until rows.length()).map { index ->
                    val row = rows.getJSONObject(index)
                    IndexedFolder(
                        grantUri = row.getString("grant"),
                        location = FolderLocation(row.getString("uri"), row.getString("name")),
                        displayPath = row.getString("path"),
                        pdfs = row.getInt("pdfs"), markdown = row.getInt("markdown"),
                        documentBytes = row.optLong("bytes", 0), unknownSizes = row.optInt("unknownSizes", 0),
                    ).also {
                        require(it.grantUri in savedRoots && it.pdfs >= 0 && it.markdown >= 0 && it.documentBytes >= 0 && it.unknownSizes >= 0 && it.pdfs + it.markdown > 0)
                    }
                }
                SavedFolderIndex(savedRoots, folders, json.getLong("scannedAt"), json.optInt("skipped", 0), json.optBoolean("limited", false))
            } catch (exception: java.io.IOException) { null }
            catch (exception: org.json.JSONException) { null }
            catch (exception: IllegalArgumentException) { null }
        }
    }

    suspend fun save(index: SavedFolderIndex) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val json = JSONObject().put("version", 1).put("roots", JSONArray(index.roots.sorted())).put("scannedAt", index.scannedAt).put("skipped", index.skipped).put("limited", index.limitReached)
            json.put("folders", JSONArray().apply {
                index.folders.forEach { folder ->
                    put(JSONObject()
                        .put("grant", folder.grantUri).put("uri", folder.location.uri)
                        .put("name", folder.location.name).put("path", folder.displayPath)
                        .put("pdfs", folder.pdfs).put("markdown", folder.markdown)
                        .put("bytes", folder.documentBytes).put("unknownSizes", folder.unknownSizes))
                }
            })
            val stream = file.startWrite()
            try {
                stream.write(json.toString().toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (exception: Exception) {
                file.failWrite(stream)
                throw exception
            }
        }
    }
}
