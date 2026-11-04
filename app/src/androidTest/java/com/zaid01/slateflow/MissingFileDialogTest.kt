package com.zaid01.slateflow

import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.zaid01.slateflow.data.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class MissingFileDialogTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun unavailableFilesShowPolishedActionsAndRemoveOnlyTheirHistoryRecord() {
        val context = compose.activity
        val tree = DocumentsContract.buildTreeDocumentUri("com.zaid01.slateflow.test.folders", "root")
        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, "pdf")
        val db = Room.databaseBuilder(context, SlateDatabase::class.java, "slateflow.db").addMigrations(SlateDatabase.MIGRATION_1_2).build()
        val original = File(context.cacheDir, "original-stays.pdf").apply { writeText("Original document") }
        try {
            val model = ViewModelProvider(compose.activity)[SlateModel::class.java]
            compose.waitUntil(10_000) { !model.loading.value }
            db.documents().put(Document().apply {
                this.uri = uri.toString(); sourceUri = uri.toString(); documentKey = DocumentIdentity.key(uri)
                name = "Moved document.pdf"; kind = "pdf"; anchor = 8; fraction = 0.4f
            })
            compose.runOnIdle { model.open(uri) }
            compose.waitUntil(10_000) { model.unavailable.value != null }
            compose.onNodeWithText("File not found").assertIsDisplayed()
            compose.onNodeWithText("Moved document.pdf").assertIsDisplayed()
            compose.onNodeWithText("OK").performClick()
            assertNotNull(db.documents().find(uri.toString()))
            compose.runOnIdle { model.open(uri) }
            compose.waitUntil(10_000) { model.unavailable.value != null }
            compose.onNodeWithText("Remove").performClick()
            compose.waitUntil(10_000) { model.unavailable.value == null }
            assertNull(db.documents().find(uri.toString()))
            assertTrue(original.exists())
        } finally { db.close(); original.delete() }
    }
}
