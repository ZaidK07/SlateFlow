package com.zaid01.slateflow

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.lifecycle.ViewModelProvider
import com.zaid01.slateflow.data.Document
import com.zaid01.slateflow.data.SlateModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class PdfScrollTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun verticalSwipeOnPdfPageScrollsTheDocument() {
        val context = compose.activity
        val file = File(context.cacheDir, "scroll-test.pdf")
        val pdf = PdfDocument()
        try {
            repeat(10) { i ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(600, 900, i + 1).create())
                page.canvas.drawText("SlateFlow page ${i + 1}", 40f, 80f, Paint().apply { textSize = 24f })
                pdf.finishPage(page)
            }
            file.outputStream().use(pdf::writeTo)
        } finally { pdf.close() }

        try {
            val model = ViewModelProvider(compose.activity)[SlateModel::class.java]
            compose.waitUntil(10_000) { !model.loading.value }
            compose.runOnIdle {
                model.active.value = Document().apply { uri = Uri.fromFile(file).toString(); name = "scroll-test.pdf"; kind = "pdf" }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Page 1").fetchSemanticsNodes().isNotEmpty() }
            
            val initialBounds = compose.onNodeWithContentDescription("Page 1").fetchSemanticsNode().boundsInRoot
            
            // Perform swipe directly on Page 1
            compose.onNodeWithContentDescription("Page 1").performTouchInput {
                swipeUp()
            }
            compose.waitForIdle()

            val afterNodes = compose.onAllNodesWithContentDescription("Page 1").fetchSemanticsNodes()
            val afterTop = afterNodes.firstOrNull()?.boundsInRoot?.top
            println("DEBUG_SCROLL: initial=${initialBounds.top}, after=$afterTop")
            val scrolled = afterNodes.isEmpty() || (afterTop != null && afterTop < initialBounds.top)
            assertTrue("Expected scrolling after swipe on PDF page", scrolled)
        } finally { file.delete() }
    }
}
