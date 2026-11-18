package com.zaid01.slateflow

import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.zaid01.slateflow.data.Document
import com.zaid01.slateflow.data.SlateModel
import com.zaid01.slateflow.data.ThemeMode
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.text.Spanned
import io.noties.markwon.image.AsyncDrawableSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import java.io.File

class OpenMarkdownTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun openMarkdownFileInReader() {
        val context = compose.activity
        val file = File(context.cacheDir, "test_render.md")
        file.writeText(
            "# Title\n" +
            "This is a paragraph with **bold** and *italic* and `code`.\n\n" +
            "- [x] Done\n" +
            "- [ ] Todo\n\n" +
            "$$ \\int_0^1 x dx $$\n\n" +
            "Inline math: \$y = mx + b\$.\n\n" +
            "Backslash math: \\(\\sqrt{x}\\).\n\n" +
            "\\[\n\\frac{a}{b}\n\\]\n\n" +
            "```math\n\\sum_{i=1}^n i\n```\n\n" +
            "```kotlin\nval a = 1\n```\n\n" +
            "| H1 | H2 |\n|---|---|\n| D1 | D2 |\n"
        )

        try {
            val model = ViewModelProvider(compose.activity)[SlateModel::class.java]
            compose.waitUntil(10_000) { !model.loading.value }
            compose.runOnIdle {
                model.active.value = Document().apply {
                    uri = Uri.fromFile(file).toString()
                    name = "test_render.md"
                    kind = "md"
                }
            }
            fun findReader(view: View): TextView? {
                if (view is TextView && view.text.contains("Inline math:")) return view
                if (view is ViewGroup) for (i in 0 until view.childCount) {
                    findReader(view.getChildAt(i))?.let { return it }
                }
                return null
            }
            fun awaitFormulas() = compose.waitUntil(15_000) {
                var ready = false
                compose.runOnIdle {
                    val reader = findReader(compose.activity.window.decorView)
                    val text = reader?.text as? Spanned
                    val spans = text?.getSpans(0, text.length, AsyncDrawableSpan::class.java)
                    ready = spans?.size == 5 && spans.all { it.drawable.hasResult() }
                }
                ready
            }
            awaitFormulas()
            val originalSettings = model.settings.value
            try {
                for (theme in listOf(ThemeMode.LIGHT, ThemeMode.DARK)) {
                    compose.runOnIdle { model.update(originalSettings.copy(themeMode = theme, font = 22f)) }
                    compose.waitUntil(5_000) { model.settings.value.themeMode == theme && model.settings.value.font == 22f }
                    awaitFormulas()
                    val shot = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                    File(context.cacheDir, "math-${theme.name.lowercase()}.png").outputStream().use {
                        shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                    shot.recycle()
                }
            } finally {
                compose.runOnIdle { model.update(originalSettings) }
            }
            awaitFormulas()
            compose.runOnIdle {
                assertNotNull(model.active.value)
                val reader = findReader(compose.activity.window.decorView)!!
                val text = reader.text as Spanned
                assertEquals(5, text.getSpans(0, text.length, AsyncDrawableSpan::class.java).size)
            }
        } finally {
            file.delete()
        }
    }
}
