package com.zaid01.slateflow

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.data.DocumentIdentity
import com.zaid01.slateflow.data.SlateDatabase
import org.junit.Assert.*
import org.junit.Test

class HistoryMigrationTest {
    @Test fun versionOneReadingPositionsSurviveTheFolderMetadataUpgrade() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        val uri = "content://com.android.externalstorage.documents/document/primary%3ANotes%2Fchapter.md"
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE documents (uri TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, kind TEXT NOT NULL, openedAt INTEGER NOT NULL, anchor INTEGER NOT NULL, offset INTEGER NOT NULL, fraction REAL NOT NULL, progress REAL NOT NULL)")
            db.execSQL("INSERT INTO documents VALUES (?, 'chapter.md', 'md', 123, 1870, 14, 0.35, 0.7)", arrayOf(uri))
            db.version = 1
        }
        val db = Room.databaseBuilder(context, SlateDatabase::class.java, name).addMigrations(SlateDatabase.MIGRATION_1_2).build()
        try {
            val saved = db.documents().find(uri)
            assertEquals(1870, saved.anchor)
            assertEquals(14, saved.offset)
            assertEquals(0.35f, saved.fraction, 0.0001f)
            assertEquals(uri, saved.sourceUri)
            assertEquals("", saved.folderName)
            assertEquals(uri, db.documents().findByKey(DocumentIdentity.key(Uri.parse(uri))).uri)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun treeAndSingleFileUrisIdentifyTheSameDocument() {
        val single = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Notes/chapter.md")
        val tree = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents", "primary:Notes")
        val child = DocumentsContract.buildDocumentUriUsingTree(tree, "primary:Notes/chapter.md")
        assertEquals(DocumentIdentity.key(single), DocumentIdentity.key(child))
    }
}
