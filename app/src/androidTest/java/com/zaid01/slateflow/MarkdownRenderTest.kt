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
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownRenderTest {
    @Test
    fun testMarkwonRenderingWithAllPlugins() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prism4j = Prism4j(SlateGrammarLocator())
        val syntaxTheme = Prism4jThemeDefault.create()
        val syntaxPlugin = SyntaxHighlightPlugin.create(prism4j, syntaxTheme)

        val markwon = Markwon.builder(context)
            .usePlugin(MarkwonInlineParserPlugin.create())
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
            markwon.setParsedMarkdown(textView, rendered)
        }
    }
}
