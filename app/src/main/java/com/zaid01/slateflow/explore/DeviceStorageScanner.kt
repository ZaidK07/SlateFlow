package com.zaid01.slateflow.explore

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import kotlinx.coroutines.withContext
import java.io.File

internal object DeviceStorageScanner {

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun requestAllFilesAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private val ignoredDirectories = setOf(
        "android", "lost.dir", "dcim", ".thumbnails", ".cache", ".git", ".gradle", "cache", "temp"
    )

    private fun isIgnored(file: File): Boolean {
        val name = file.name.lowercase()
        return name.startsWith(".") || name in ignoredDirectories
    }

    private fun isSupportedDocument(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".pdf") || lower.endsWith(".md") || lower.endsWith(".markdown")
    }

    suspend fun scanDevice(
        onProgress: suspend (visited: Int, folders: List<IndexedFolder>, files: List<IndexedFile>) -> Unit
    ): FolderScanResult = withContext(Dispatchers.IO) {
        val primaryStorage = Environment.getExternalStorageDirectory()
        val rootsToScan = mutableListOf<File>()
        if (primaryStorage.exists() && primaryStorage.canRead()) {
            rootsToScan += primaryStorage
        }

        val discoveredFolders = mutableListOf<IndexedFolder>()
        val discoveredFiles = mutableListOf<IndexedFile>()
        var visitedCount = 0
        var skippedCount = 0

        val queue = ArrayDeque<File>()
        for (root in rootsToScan) {
            val children = root.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory && !isIgnored(child)) {
                    queue.add(child)
                } else if (child.isFile && isSupportedDocument(child.name)) {
                    // Document in root /sdcard
                    addFileToFolder(root, listOf(child), discoveredFolders, discoveredFiles)
                }
            }
        }

        val primaryPath = primaryStorage.absolutePath

        while (queue.isNotEmpty()) {
            val dir = queue.removeFirst()
            visitedCount++

            val entries = dir.listFiles()
            if (entries == null) {
                skippedCount++
                continue
            }

            val docFiles = mutableListOf<File>()
            for (entry in entries) {
                if (entry.isDirectory) {
                    if (!isIgnored(entry)) queue.add(entry)
                } else if (entry.isFile && isSupportedDocument(entry.name)) {
                    docFiles += entry
                }
            }

            if (docFiles.isNotEmpty()) {
                val folderUri = Uri.fromFile(dir).toString()
                val relativePath = dir.absolutePath.removePrefix(primaryPath).trimStart('/')
                val display = if (relativePath.isBlank()) dir.name else relativePath

                var pdfCount = 0
                var mdCount = 0
                var totalBytes = 0L

                for (f in docFiles) {
                    val isPdf = f.name.endsWith(".pdf", ignoreCase = true)
                    if (isPdf) pdfCount++ else mdCount++
                    val size = f.length()
                    totalBytes += size

                    discoveredFiles += IndexedFile(
                        uri = Uri.fromFile(f).toString(),
                        name = f.name,
                        kind = if (isPdf) "pdf" else "md",
                        modifiedAt = f.lastModified(),
                        size = size,
                        sizeKnown = true,
                        folderName = dir.name,
                        folderUri = folderUri,
                        grantUri = folderUri
                    )
                }

                discoveredFolders += IndexedFolder(
                    grantUri = folderUri,
                    location = FolderLocation(folderUri, dir.name),
                    displayPath = display,
                    pdfs = pdfCount,
                    markdown = mdCount,
                    documentBytes = totalBytes,
                    unknownSizes = 0
                )

                if (visitedCount % 25 == 0) {
                    onProgress(visitedCount, discoveredFolders.toList(), discoveredFiles.toList())
                    yield()
                }
            }
        }

        onProgress(visitedCount, discoveredFolders.toList(), discoveredFiles.toList())
        FolderScanResult(
            folders = discoveredFolders,
            files = discoveredFiles,
            visited = visitedCount,
            skipped = skippedCount,
            limitReached = false
        )
    }

    private fun addFileToFolder(
        dir: File,
        docFiles: List<File>,
        discoveredFolders: MutableList<IndexedFolder>,
        discoveredFiles: MutableList<IndexedFile>
    ) {
        val folderUri = Uri.fromFile(dir).toString()
        var pdfCount = 0
        var mdCount = 0
        var totalBytes = 0L

        for (f in docFiles) {
            val isPdf = f.name.endsWith(".pdf", ignoreCase = true)
            if (isPdf) pdfCount++ else mdCount++
            val size = f.length()
            totalBytes += size

            discoveredFiles += IndexedFile(
                uri = Uri.fromFile(f).toString(),
                name = f.name,
                kind = if (isPdf) "pdf" else "md",
                modifiedAt = f.lastModified(),
                size = size,
                sizeKnown = true,
                folderName = dir.name,
                folderUri = folderUri,
                grantUri = folderUri
            )
        }

        discoveredFolders += IndexedFolder(
            grantUri = folderUri,
            location = FolderLocation(folderUri, dir.name),
            displayPath = dir.name,
            pdfs = pdfCount,
            markdown = mdCount,
            documentBytes = totalBytes,
            unknownSizes = 0
        )
    }
}
