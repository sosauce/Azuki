package com.sosauce.azuki.core.ui.components.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String, val indent: Int) : MarkdownBlock
    data class Numbered(val number: String, val text: String, val indent: Int) : MarkdownBlock

    /** @param lineIndex index of the source line, used to toggle the box. */
    data class Task(val text: String, val checked: Boolean, val lineIndex: Int, val indent: Int) :
        MarkdownBlock

    data class Quote(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String) : MarkdownBlock
    data object Divider : MarkdownBlock
}

/** Small Markdown subset: headings, lists, task lists, quotes, fenced code, dividers and inline styles. */
object MarkdownParser {

    private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
    private val taskRegex = Regex("""^(\s*)[-*+]\s+\[([ xX])]\s?(.*)$""")
    private val bulletRegex = Regex("""^(\s*)[-*+]\s+(.*)$""")
    private val numberedRegex = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")
    private val quoteRegex = Regex("""^\s*>\s?(.*)$""")
    private val dividerRegex = Regex("""^\s*([-*_])(\s*\1){2,}\s*$""")
    private val fenceRegex = Regex("""^\s*```.*$""")
    private val taskBoxRegex = Regex("""\[([ xX])]""")

    fun parse(source: String): List<MarkdownBlock> {
        val lines = source.split('\n')
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        val quote = mutableListOf<String>()
        var code: MutableList<String>? = null

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
            paragraph.clear()
        }

        fun flushQuote() {
            if (quote.isNotEmpty()) blocks += MarkdownBlock.Quote(quote.joinToString("\n"))
            quote.clear()
        }

        lines.forEachIndexed { index, line ->
            val openCode = code
            if (openCode != null) {
                if (fenceRegex.matches(line)) {
                    blocks += MarkdownBlock.CodeBlock(openCode.joinToString("\n"))
                    code = null
                } else {
                    openCode += line
                }
                return@forEachIndexed
            }

            val quoteMatch = quoteRegex.matchEntire(line)
            if (quoteMatch != null) {
                flushParagraph()
                quote += quoteMatch.groupValues[1]
                return@forEachIndexed
            }
            flushQuote()

            when {
                line.isBlank() -> flushParagraph()
                fenceRegex.matches(line) -> {
                    flushParagraph()
                    code = mutableListOf()
                }

                dividerRegex.matches(line) -> {
                    flushParagraph()
                    blocks += MarkdownBlock.Divider
                }

                else -> {
                    val block = parseLine(line, index)
                    if (block == null) {
                        paragraph += line.trim()
                    } else {
                        flushParagraph()
                        blocks += block
                    }
                }
            }
        }
        code?.let { blocks += MarkdownBlock.CodeBlock(it.joinToString("\n")) }
        flushParagraph()
        flushQuote()
        return blocks
    }

    private fun parseLine(line: String, index: Int): MarkdownBlock? {
        headingRegex.matchEntire(line)?.let {
            return MarkdownBlock.Heading(level = it.groupValues[1].length, text = it.groupValues[2])
        }
        taskRegex.matchEntire(line)?.let {
            return MarkdownBlock.Task(
                text = it.groupValues[3],
                checked = it.groupValues[2].isNotBlank(),
                lineIndex = index,
                indent = it.groupValues[1].indentLevel()
            )
        }
        bulletRegex.matchEntire(line)?.let {
            return MarkdownBlock.Bullet(
                text = it.groupValues[2],
                indent = it.groupValues[1].indentLevel()
            )
        }
        numberedRegex.matchEntire(line)?.let {
            return MarkdownBlock.Numbered(
                number = it.groupValues[2],
                text = it.groupValues[3],
                indent = it.groupValues[1].indentLevel()
            )
        }
        return null
    }

    private fun String.indentLevel(): Int = replace("\t", "  ").length / 2

    /** Flips `[ ]` <-> `[x]` on the given source line. */
    fun toggleTask(source: String, lineIndex: Int): String {
        val lines = source.split('\n').toMutableList()
        val line = lines.getOrNull(lineIndex) ?: return source
        if (!taskRegex.matches(line)) return source
        val box = taskBoxRegex.find(line) ?: return source
        val replacement = if (box.groupValues[1].isBlank()) "[x]" else "[ ]"
        lines[lineIndex] = line.replaceRange(box.range, replacement)
        return lines.joinToString("\n")
    }

    /** Markdown stripped down to readable text, for previews and sharing stats. */
    fun toPlainText(source: String): String = source
        .split('\n')
        .filterNot { fenceRegex.matches(it) || dividerRegex.matches(it) }
        .joinToString("\n") { line ->
            line
                .replace(headingRegex, "$2")
                .replace(Regex("""^(\s*)[-*+]\s+\[[xX]]\s?"""), "$1✓ ")
                .replace(Regex("""^(\s*)[-*+]\s+\[ ]\s?"""), "$1○ ")
                .replace(Regex("""^(\s*)[-*+]\s+"""), "$1• ")
                .replace(Regex("""^\s*>\s?"""), "")
        }
        .replace(Regex("""\[([^\]]+)]\(([^)\s]+)\)"""), "$1")
        .replace(Regex("""(\*\*|__|~~|`)"""), "")
        .replace(Regex("""(?<![\w*])[*_](?=\S)(.+?)(?<=\S)[*_](?![\w*])"""), "$1")
        .trim()
}

data class MarkdownInlineStyles(
    val linkColor: Color,
    val codeBackground: Color
)

private val linkRegex = Regex("""\[([^\]]+)]\(([^)\s]+)\)""")

fun parseInlineMarkdown(text: String, styles: MarkdownInlineStyles): AnnotatedString =
    buildAnnotatedString { appendInline(text, styles) }

private fun AnnotatedString.Builder.appendInline(text: String, styles: MarkdownInlineStyles) {
    var i = 0
    while (i < text.length) {
        val consumed = when {
            text.startsWith("**", i) || text.startsWith("__", i) ->
                wrapped(
                    text,
                    i,
                    text.substring(i, i + 2),
                    SpanStyle(fontWeight = FontWeight.Black),
                    styles
                )

            text.startsWith("~~", i) ->
                wrapped(
                    text,
                    i,
                    "~~",
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    styles
                )

            text[i] == '*' || text[i] == '_' ->
                wrapped(
                    text,
                    i,
                    text[i].toString(),
                    SpanStyle(fontStyle = FontStyle.Italic),
                    styles
                )

            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = styles.codeBackground
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    end + 1 - i
                } else 0
            }

            text[i] == '[' -> {
                val match = linkRegex.find(text, i)?.takeIf { it.range.first == i }
                if (match != null) {
                    withLink(
                        LinkAnnotation.Url(
                            url = match.groupValues[2],
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = styles.linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) { appendInline(match.groupValues[1], styles) }
                    match.value.length
                } else 0
            }

            else -> 0
        }
        if (consumed > 0) {
            i += consumed
        } else {
            append(text[i])
            i++
        }
    }
}

/** Appends `delimiter…delimiter` starting at [start] with [style]. Returns consumed chars, 0 if unmatched. */
private fun AnnotatedString.Builder.wrapped(
    text: String,
    start: Int,
    delimiter: String,
    style: SpanStyle,
    styles: MarkdownInlineStyles
): Int {
    val contentStart = start + delimiter.length
    if (contentStart >= text.length || text[contentStart].isWhitespace()) return 0
    val end = text.indexOf(delimiter, contentStart)
    if (end <= contentStart || text[end - 1].isWhitespace()) return 0
    withStyle(style) { appendInline(text.substring(contentStart, end), styles) }
    return end + delimiter.length - start
}
