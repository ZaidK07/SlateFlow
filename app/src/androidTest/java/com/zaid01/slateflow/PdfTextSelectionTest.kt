package com.zaid01.slateflow

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.reader.PdfEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PdfTextSelectionTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun fixture(name: String, draw: (Canvas) -> Unit): File {
        val file = File(context.cacheDir, name)
        val pdf = PdfDocument()
        try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(600, 900, 1).create())
            draw(page.canvas)
            pdf.finishPage(page)
            file.outputStream().use(pdf::writeTo)
        } finally { pdf.close() }
        return file
    }

    @Test fun mixedContentKeepsImagesAndProvidesRealSelectableTextAndSearch() = runBlocking {
        val paint = Paint().apply { textSize = 24f; color = Color.BLACK }
        val file = fixture("mixed-content-test.pdf") { canvas ->
            canvas.drawText("SlateFlow selection works", 40f, 80f, paint)
            val image = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            try { canvas.drawBitmap(image, 40f, 180f, null) } finally { image.recycle() }
            canvas.drawText("Text survives mixed content", 40f, 320f, paint)
        }
        try {
            PdfEngine(context, Uri.fromFile(file)).use { engine ->
                val bitmap = engine.render(0, 600)
                assertEquals(Color.RED, bitmap.getPixel(60, 200))
                val word = engine.selectWord(0, 60f / 600, 70f / 900)!!
                assertEquals("SlateFlow", word.text)
                assertTrue(word.bounds.isNotEmpty())
                assertTrue(word.bounds.all { it.left >= 0 && it.top >= 0 && it.right <= 1 && it.bottom <= 1 })
                val endpoint = 40 + paint.measureText("SlateFlow selection work") + paint.measureText("s") / 2
                val extended = engine.extendSelection(0, word.start, endpoint / 600, 70f / 900)!!
                assertEquals("SlateFlow selection works", extended.text.trim())
                val all = engine.selectPage(0)!!
                assertTrue(all.text.contains("Text survives mixed content"))
                assertEquals("mixed", engine.findNext("mixed", 0, 0)!!.text)
                engine.releaseTextPage()
                assertNotNull(engine.selectWord(0, 60f / 600, 70f / 900))
            }
        } finally { file.delete() }
    }

    @Test fun imageOnlyPagesAreNotMisrepresentedAsSelectableText() = runBlocking {
        val file = fixture("image-content-test.pdf") { canvas ->
            val image = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
            try {
                image.eraseColor(Color.WHITE)
                Canvas(image).drawText("This text is pixels", 5f, 50f, Paint().apply { color = Color.BLACK; textSize = 24f })
                canvas.drawBitmap(image, 40f, 80f, null)
            } finally { image.recycle() }
        }
        try {
            PdfEngine(context, Uri.fromFile(file)).use { engine ->
                assertNull(engine.selectPage(0))
                assertNull(engine.findNext("text", 0, 0))
            }
        } finally { file.delete() }
    }
}
