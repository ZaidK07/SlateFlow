package com.zaid01.slateflow.explore

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import java.util.ArrayDeque
import java.util.Locale

internal data class ScanRoot(val grantUri: String, val location: FolderLocation)
internal data class IndexedFolder(
    val grantUri: String,
    val location: FolderLocation,
    val displayPath: String,
    val pdfs: Int,
    val markdown: Int,
    val documentBytes: Long = 0,
    val unknownSizes: Int = 0,
) {
    val summary: String get() = FolderSummary(pdfs = pdfs, markdown = markdown).label
}
internal data class ScanProgress(val visited: Int, val folders: List<IndexedFolder>)
internal data class FolderScanResult(val folders: List<IndexedFolder>, val visited: Int, val skipped: Int, val limitReached: Boolean)

/** Iterative traversal avoids stack overflow and queries each provider directory once. */
internal class FolderScanner(
    private val children: suspend (FolderLocation) -> List<FolderEntry>,
    private val identity: (String) -> String,
    private val folderLimit: Int = 20_000,
) {
    private data class Pending(val grantUri: String, val location: FolderLocation, val displayPath: String)

    suspend fun scan(roots: List<ScanRoot>, progress: suspend (ScanProgress) -> Unit = {}): FolderScanResult {
        val queue = ArrayDeque<Pending>()
        val scheduled = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val found = mutableListOf<IndexedFolder>()
        var skipped = 0
        var limitReached = false
        var lastUpdate = System.nanoTime()

        fun enqueue(pending: Pending) {
            val key = identity(pending.location.uri)
            if (key in scheduled) return
            if (scheduled.size >= folderLimit) {
                limitReached = true
                return
            }
            scheduled += key
            queue.addLast(pending)
        }
        roots.sortedBy { it.location.name.lowercase(Locale.ROOT) }.forEach {
            enqueue(Pending(it.grantUri, it.location, it.location.name))
        }
        progress(ScanProgress(0, emptyList()))
        while (queue.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val pending = queue.removeFirst()
            val key = identity(pending.location.uri)
            if (!visited.add(key)) continue
            val entries = try {
                children(pending.location)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                skipped++
                emptyList()
            }
            var pdfs = 0
            var markdown = 0
            var bytes = 0L
            var unknownSizes = 0
            for (entry in entries) {
                if (entry.isFolder) {
                    enqueue(Pending(pending.grantUri, FolderLocation(entry.uri, entry.name), "${pending.displayPath}/${entry.name}"))
                } else if (entry.kind == "pdf" || entry.kind == "md") {
                    if (entry.kind == "pdf") pdfs++ else markdown++
                    if (entry.sizeKnown) bytes += entry.size.coerceAtLeast(0) else unknownSizes++
                }
            }
            if (pdfs + markdown > 0) {
                found += IndexedFolder(pending.grantUri, pending.location, pending.displayPath, pdfs, markdown, bytes, unknownSizes)
            }
            if (System.nanoTime() - lastUpdate >= 100_000_000L) {
                progress(ScanProgress(visited.size, found.toList()))
                lastUpdate = System.nanoTime()
            }
            yield()
        }
        val sorted = found.sortedBy { it.displayPath.lowercase(Locale.ROOT) }
        progress(ScanProgress(visited.size, sorted))
        return FolderScanResult(sorted, visited.size, skipped, limitReached)
    }
}
