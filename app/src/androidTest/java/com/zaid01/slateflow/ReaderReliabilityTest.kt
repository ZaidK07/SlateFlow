package com.zaid01.slateflow

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.data.Document
import com.zaid01.slateflow.data.SlateDatabase
import com.zaid01.slateflow.reader.PdfEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReaderReliabilityTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun readingAnchorsSurviveClosingAndReopeningTheDatabase() {
        val name = "position-test.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, SlateDatabase::class.java, name).build()
        try {
            first.documents().put(Document().apply { uri = "content://test/chapter.md"; this.name = "chapter.md"; kind = "md" })
            first.documents().savePosition("content://test/chapter.md", 1374, 8, 0.45f, 0.62f)
        } finally { first.close() }
        val reopened = Room.databaseBuilder(context, SlateDatabase::class.java, name).build()
        try {
            val saved = reopened.documents().find("content://test/chapter.md")
            assertEquals(1374, saved.anchor)
            assertEquals(8, saved.offset)
            assertEquals(0.45f, saved.fraction, 0.0001f)
            assertEquals(0.62f, saved.progress, 0.0001f)
        } finally { reopened.close(); context.deleteDatabase(name) }
    }

    @Test fun savingOneDocumentDoesNotMoveAnotherDocumentsPosition() {
        val db = Room.inMemoryDatabaseBuilder(context, SlateDatabase::class.java).build()
        try {
            listOf("first", "second").forEach { uri -> db.documents().put(Document().apply { this.uri = uri }) }
            db.documents().savePosition("first", 23, 64, 0.2f, 0.5f)
            db.documents().savePosition("second", 3, 16, 0.1f, 0.9f)
            assertEquals(23, db.documents().find("first").anchor)
            assertEquals(3, db.documents().find("second").anchor)
        } finally { db.close() }
    }

    @Test fun nativePdfRenderingBoundsMemoryAndAllowsPreviouslyEvictedPages() = runBlocking {
        val file = File(context.cacheDir, "reader-test.pdf")
        val pdf = PdfDocument()
        try {
            repeat(6) { index ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(600, 900, index + 1).create())
                page.canvas.drawText("SlateFlow page ${index + 1}", 40f, 80f, Paint().apply { textSize = 24f })
                pdf.finishPage(page)
            }
            file.outputStream().use { pdf.writeTo(it) }
        } finally { pdf.close() }
        try {
            PdfEngine(context, Uri.fromFile(file)).use { engine ->
                assertEquals(6, engine.count)
                repeat(6) { index ->
                    val bitmap = engine.render(index, 2400)
                    assertTrue(bitmap.allocationByteCount <= 4 * 1024 * 1024)
                    assertTrue(bitmap.width <= 1600)
                    assertFalse(bitmap.isRecycled)
                }
                assertFalse(engine.render(0, 2400).isRecycled)
            }
        } finally { file.delete() }
    }
}
