package com.zaid01.slateflow

import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.explore.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FolderIndexStoreTest {
    @Test fun savedIndexSurvivesRestartAndIsScopedToItsExactScanLocations() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "folder-index-test.json")
        file.delete()
        try {
            val folder = IndexedFolder("grant", FolderLocation("uri", "Notes"), "Study/Physics/Notes", 2, 3, 4096, 1)
            FolderIndexStore(file).save(SavedFolderIndex(setOf("grant"), listOf(folder), 1234))
            val reloaded = FolderIndexStore(file).load(setOf("grant"))!!
            assertEquals(listOf(folder), reloaded.folders)
            assertEquals(1234L, reloaded.scannedAt)
            assertNull(FolderIndexStore(file).load(setOf("another-grant")))
            assertNull(FolderIndexStore(file).load(emptySet()))
        } finally { file.delete() }
    }

    @Test fun anInterruptedWriteKeepsThePreviousCompletedIndex() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "interrupted-index-test.json")
        try {
            val snapshot = SavedFolderIndex(setOf("g"), listOf(IndexedFolder("g", FolderLocation("u", "Notes"), "Study/Notes", 1, 0, 100)), 1000)
            FolderIndexStore(file).save(snapshot)
            android.util.AtomicFile(file).startWrite().use { it.write("incomplete replacement".toByteArray()) }
            assertEquals(snapshot, FolderIndexStore(file).load(setOf("g")))
        } finally { android.util.AtomicFile(file).delete() }
    }

    @Test fun corruptCacheIsIgnoredSoTheNextLaunchCanRescan() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "broken-index-test.json")
        try {
            file.writeText("{broken json")
            assertNull(FolderIndexStore(file).load(setOf("grant")))
        } finally { file.delete() }
    }
}
