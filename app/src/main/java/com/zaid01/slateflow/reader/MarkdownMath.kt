package com.zaid01.slateflow.reader

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.ext.latex.JLatexMathBlock
import io.noties.markwon.ext.latex.JLatexMathNode
import io.noties.markwon.inlineparser.BackslashInlineProcessor
import io.noties.markwon.inlineparser.InlineProcessor
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.BlockQuote
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.parser.block.*

/** Native math parsing: code spans, escapes and link destinations remain owned by CommonMark. */
internal class MarkdownMathPlugin : AbstractMarkwonPlugin() {
    override fun configure(registry: io.noties.markwon.MarkwonPlugin.Registry) {
        // Replace the default escape processor so math gets first refusal on a backslash.
        registry.require(MarkwonInlineParserPlugin::class.java).factoryBuilder()
            .excludeInlineProcessor(BackslashInlineProcessor::class.java)
            .addInlineProcessor(MathInlineProcessor('$'))
            .addInlineProcessor(MathInlineProcessor('\\'))
            .addInlineProcessor(BackslashInlineProcessor())
    }

    override fun configureParser(builder: Parser.Builder) {
        builder.customBlockParserFactory(BracketMathParser.Factory())
    }

    override fun beforeRender(node: Node) {
        // Walk iteratively: deeply nested documents must not exhaust the Java stack.
        var current: Node? = node.firstChild
        while (current != null) {
            val next = current.firstChild ?: run {
                var cursor = current
                while (cursor != null && cursor !== node && cursor.next == null) cursor = cursor.parent
                if (cursor === node) null else cursor?.next
            }
            if (current is Text && current.previous == null && current.parent is Paragraph && current.parent.parent is BlockQuote) {
                val match = ALERT.find(current.literal)
                if (match != null) {
                    current.insertBefore(StrongEmphasis().apply { appendChild(Text(match.groupValues[1])) })
                    current.literal = current.literal.substring(match.value.length)
                }
            }
            if (current is FencedCodeBlock && current.info.trim().lowercase() in MATH_LANGUAGES) {
                val math = JLatexMathBlock().apply { latex(current.literal) }
                current.insertBefore(math)
                current.unlink()
            }
            current = next
        }
    }

    private companion object {
        val ALERT = Regex("""^\[!(NOTE|TIP|WARNING|IMPORTANT|CAUTION)\]""")
        val MATH_LANGUAGES = setOf("math", "latex", "tex")
    }
}

/** Limit failed delimiter searches to avoid quadratic scans on malformed, dollar-heavy input. */
internal class MathInlineProcessor(private val trigger: Char) : InlineProcessor() {
    override fun specialCharacter() = trigger

    override fun parse(): Node? {
        val start = index
        val slash = trigger == '\\'
        val opening = when {
            slash && input.startsWith("\\(", start) -> "\\("
            slash && input.startsWith("\\[", start) -> "\\["
            slash -> return null
            input.startsWith("$$", start) -> "$$"
            else -> "$"
        }
        val closing = when (opening) { "\\(" -> "\\)"; "\\[" -> "\\]"; else -> opening }
        val contentStart = start + opening.length
        if (contentStart >= input.length) return null
        val single = opening == "$"
        if (single && (input[contentStart].isWhitespace() || (start > 0 && input[start - 1].isLetterOrDigit()))) return null
        val limit = minOf(input.length, contentStart + 16_384)
        var cursor = contentStart
        while (cursor < limit) {
            val char = input[cursor]
            if (single && char == '\n') return null
            if (input.startsWith(closing, cursor)) {
                if (cursor == contentStart) return null
                val end = cursor + closing.length
                if (single && (input[cursor - 1].isWhitespace() || (end < input.length && input[end].isDigit()))) return null
                index = end
                return JLatexMathNode().apply { latex(input.substring(contentStart, cursor)) }
            }
            // Escaped dollars/brackets cannot close the expression; TeX row breaks are preserved.
            cursor += if (char == '\\' && cursor + 1 < limit) 2 else 1
        }
        return null
    }
}

/** Display math using \[ ... \], including nested list and quote containers. */
private class BracketMathParser(private var closed: Boolean, initial: String) : AbstractBlockParser() {
    private val math = JLatexMathBlock()
    private val source = StringBuilder(initial)
    override fun getBlock() = math
    override fun tryContinue(state: ParserState): BlockContinue =
        if (closed) BlockContinue.finished() else BlockContinue.atIndex(state.index)

    override fun addLine(line: CharSequence) {
        // The opening line is consumed by Factory; subsequent lines retain TeX whitespace.
        if (line.isEmpty()) return
        val end = line.toString().trimEnd()
        if (end.endsWith("\\]")) {
            source.append(end.dropLast(2))
            closed = true
        } else source.append(line).append('\n')
    }
    override fun closeBlock() { math.latex(source.toString()) }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(state: ParserState, matched: MatchedBlockParser): BlockStart? {
            if (state.indent >= 4) return BlockStart.none()
            val line = state.line.toString().substring(state.nextNonSpaceIndex).trimEnd()
            if (!line.startsWith("\\[")) return BlockStart.none()
            val content = line.substring(2)
            val closed = content.endsWith("\\]")
            if (!closed && content.contains("\\]")) return BlockStart.none()
            val initial = if (closed) content.dropLast(2) else "$content\n"
            return BlockStart.of(BracketMathParser(closed, initial)).atIndex(state.line.length + 1)
        }
    }
}
