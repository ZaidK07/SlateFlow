package com.zaid01.slateflow

import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.reader.SlateGrammarLocator
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.zaid01.slateflow.reader.MarkdownMathPlugin
import io.noties.markwon.image.AsyncDrawableSpan
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownRenderTest {
    @Test
    fun testMarkwonRenderingWithAllPlugins() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ru.noties.jlatexmath.JLatexMathAndroid.init(context)
        val prism4j = Prism4j(SlateGrammarLocator())
        val syntaxTheme = Prism4jThemeDefault.create()
        val syntaxPlugin = SyntaxHighlightPlugin.create(prism4j, syntaxTheme)

        val markwon = Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(MarkdownMathPlugin())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(JLatexMathPlugin.create(36f) { builder ->
                builder.inlinesEnabled(true)
            })
            .usePlugin(syntaxPlugin)
            .build()

        val sample = "# Header\n" +
            "Text with **bold** and *italic* and ~~strike~~.\n" +
            "- [x] Done\n" +
            "- [ ] Todo\n\n" +
            "```kotlin\nfun test() = println(\"hello\")\n```\n\n" +
            "Math: $$ \\frac{1}{2} $$ and inline \$x = 1\$.\n\n" +
            "| A | B |\n|---|---|\n| 1 | 2 |\n"

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val textView = TextView(context)
            val parsed = markwon.parse(sample)
            val rendered = markwon.render(parsed)
            assertNotNull(rendered)
            assertEquals("Both double and single dollar math must become drawable spans", 2,
                rendered.getSpans(0, rendered.length, AsyncDrawableSpan::class.java).size)
            markwon.setParsedMarkdown(textView, rendered)
        }
    }
    @Test
    fun supportedMathProducesRealDrawables() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ru.noties.jlatexmath.JLatexMathAndroid.init(context)
        val formulas = listOf(
            "x^2 + y^2 = z^2",
            "\\frac{1}{2}",
            "\\sum_{i=1}^{n} i",
            "\\int_0^1 x^2 \\, dx",
            "\\begin{pmatrix}a & b \\\\ c & d\\end{pmatrix}",
            "\\begin{aligned}a &= b+c \\\\ d &= e+f\\end{aligned}",
            "\\sqrt{\\alpha^2+\\beta^2}"
        )
        formulas.forEach { formula ->
            val drawable = ru.noties.jlatexmath.JLatexMathDrawable.builder(formula)
                .textSize(36f).color(android.graphics.Color.WHITE).build()
            assertTrue("Formula must have visible dimensions: $formula", drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)
            val bitmap = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, bitmap.width, bitmap.height)
            drawable.draw(android.graphics.Canvas(bitmap))
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue("Formula must draw visible ink: $formula", pixels.any { android.graphics.Color.alpha(it) > 0 })
            bitmap.recycle()
        }
    }

}
