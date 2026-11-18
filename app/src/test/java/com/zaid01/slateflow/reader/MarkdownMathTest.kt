package com.zaid01.slateflow.reader

import io.noties.markwon.ext.latex.JLatexMathBlock
import io.noties.markwon.ext.latex.JLatexMathNode
import io.noties.markwon.inlineparser.BackslashInlineProcessor
import io.noties.markwon.inlineparser.MarkwonInlineParser
import org.commonmark.node.*
import org.commonmark.parser.Parser
import org.junit.Assert.*
import org.junit.Test

class MarkdownMathTest {
    private fun parse(source: String): List<Node> {
        val inline = MarkwonInlineParser.factoryBuilder()
            .excludeInlineProcessor(BackslashInlineProcessor::class.java)
            .addInlineProcessor(MathInlineProcessor('$'))
            .addInlineProcessor(MathInlineProcessor('\\'))
            .addInlineProcessor(BackslashInlineProcessor())
        val plugin = MarkdownMathPlugin()
        val builder = Parser.builder().inlineParserFactory(inline.build())
        plugin.configureParser(builder)
        val root = builder.build().parse(source)
        plugin.beforeRender(root)
        val result = mutableListOf<Node>()
        fun visit(node: Node) {
            result.add(node)
            var child = node.firstChild
            while (child != null) { visit(child); child = child.next }
        }
        visit(root)
        return result
    }

    @Test fun inlineDelimitersPreserveTex() {
        val nodes = parse("""Inline ${'$'}x^2${'$'}, \(\frac{1}{2}\), ${'$'}${'$'}\alpha_1${'$'}${'$'} and \[a+b\].""")
        assertEquals(listOf("x^2", "\\frac{1}{2}", "\\alpha_1", "a+b"), nodes.filterIsInstance<JLatexMathNode>().map { it.latex() })
    }

    @Test fun literalCodeCurrencyAndEscapesStayLiteral() {
        val nodes = parse("""
            Prices ${'$'}5 and ${'$'}10. Escaped \${'$'}x\${'$'}. Unclosed ${'$'}x.
            `${'$'}x${'$'}` and `\(x\)`.

            ```kotlin
            val price = "${'$'}x${'$'}"
            > [!NOTE]
            ```

                ${'$'}x${'$'}
        """.trimIndent())
        assertTrue(nodes.none { it is JLatexMathNode || it is JLatexMathBlock })
        assertTrue(nodes.filterIsInstance<FencedCodeBlock>().single().literal.contains("> [!NOTE]"))
    }

    @Test fun displayMathAndFencesWorkInContainers() {
        val nodes = parse("""
            \[
            \begin{aligned}
            a &= b \\
            c &= d
            \end{aligned}
            \]

            > \[x^2\]

            - ```math
              \sum_{i=1}^n i
              ```

            ```latex
            \frac{a}{b}
            ```
        """.trimIndent())
        val math = nodes.filterIsInstance<JLatexMathBlock>()
        assertEquals(4, math.size)
        assertTrue(math[0].latex().contains("a &= b \\\\"))
        assertEquals("x^2", math[1].latex())
        assertTrue(nodes.none { it is FencedCodeBlock })
    }

    @Test fun alertsRenderWithoutChangingCode() {
        val nodes = parse("> [!NOTE]\n> Text\n\n`> [!NOTE]`")
        assertEquals("NOTE", (nodes.filterIsInstance<StrongEmphasis>().single().firstChild as Text).literal)
        assertEquals("> [!NOTE]", nodes.filterIsInstance<Code>().single().literal)
    }

    @Test fun inlineDisplayFollowedByProseDoesNotConsumeTheParagraph() {
        val nodes = parse("\\[x^2\\] followed by **bold** text.")
        assertEquals("x^2", nodes.filterIsInstance<JLatexMathNode>().single().latex())
        assertTrue(nodes.any { it is StrongEmphasis })
        assertTrue(nodes.none { it is JLatexMathBlock })
    }

    @Test(timeout = 3000) fun malformedDollarHeavyInputIsBounded() {
        parse(("${'$'}x ".repeat(40_000)))
    }
}
