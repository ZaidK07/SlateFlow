package com.zaid01.slateflow

import android.content.ClipboardManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.lifecycle.ViewModelProvider
import com.zaid01.slateflow.data.Document
import com.zaid01.slateflow.data.SlateModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class PdfCopyInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun longPressExposesCopyAndCopiesTheActualPdfText() {
        val context = compose.activity
        val file = File(context.cacheDir, "copy-interaction.pdf")
        val pdf = PdfDocument()
        try {
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(600, 900, 1).create())
            page.canvas.drawText("SlateFlow selection works", 40f, 80f, Paint().apply { textSize = 24f })
            pdf.finishPage(page)
            file.outputStream().use(pdf::writeTo)
        } finally { pdf.close() }
        try {
            val model = ViewModelProvider(compose.activity)[SlateModel::class.java]
            compose.waitUntil(10_000) { !model.loading.value }
            compose.runOnIdle {
                model.active.value = Document().apply { uri = Uri.fromFile(file).toString(); name = "copy-interaction.pdf"; kind = "pdf" }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Page 1").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Page 1").performTouchInput { longClick(Offset(width * 0.1f, height * 0.078f)) }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Copy").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Copy").performClick()
            compose.runOnIdle {
                val text = context.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString()
                assertTrue(text.contains("SlateFlow"))
            }
        } finally { file.delete() }
    }
}
