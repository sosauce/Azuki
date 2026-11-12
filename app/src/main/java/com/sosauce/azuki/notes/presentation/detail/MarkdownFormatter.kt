package com.sosauce.azuki.notes.presentation.detail

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.insert
import androidx.compose.ui.text.TextRange

enum class MarkdownFormat {
    BOLD, ITALIC, STRIKETHROUGH, CODE, LINK,
    HEADING, BULLET_LIST, NUMBERED_LIST, TASK_LIST, QUOTE
}

private val headingPrefix = Regex("""^#{1,6}\s""")
private val taskPrefix = Regex("""^[-*+]\s\[[ xX]]\s?""")
private val bulletPrefix = Regex("""^[-*+]\s""")
private val bulletOnlyPrefix = Regex("""^[-*+]\s(?!\[[ xX]])""")
private val numberedPrefix = Regex("""^\d+[.)]\s""")
private val quotePrefix = Regex("""^>\s?""")

/** Applies [format] around the current selection (inline) or to the selected lines (block). */
fun TextFieldBuffer.applyMarkdownFormat(format: MarkdownFormat) {
    when (format) {
        MarkdownFormat.BOLD -> wrapSelection("**")
        MarkdownFormat.ITALIC -> wrapSelection("_")
        MarkdownFormat.STRIKETHROUGH -> wrapSelection("~~")
        MarkdownFormat.CODE -> {
            val selected = selectedText()
            if (selected.contains('\n')) wrapSelection("```\n", "\n```") else wrapSelection("`")
        }

        MarkdownFormat.LINK -> insertLink()
        MarkdownFormat.HEADING -> toggleLinePrefix(headingPrefix) { "# " }
        MarkdownFormat.BULLET_LIST -> toggleLinePrefix(bulletOnlyPrefix) { "- " }
        MarkdownFormat.NUMBERED_LIST -> toggleLinePrefix(numberedPrefix) { index -> "${index + 1}. " }
        MarkdownFormat.TASK_LIST -> toggleLinePrefix(taskPrefix) { "- [ ] " }
        MarkdownFormat.QUOTE -> toggleLinePrefix(quotePrefix) { "> " }
    }
}

private fun TextFieldBuffer.selectedText(): String =
    asCharSequence().substring(selection.min, selection.max)

private fun TextFieldBuffer.wrapSelection(start: String, end: String = start) {
    val selStart = selection.min
    val selEnd = selection.max
    insert(selEnd, end)
    insert(selStart, start)
    selection = if (selStart == selEnd) {
        TextRange(selStart + start.length)
    } else {
        TextRange(selStart + start.length, selEnd + start.length)
    }
}

private fun TextFieldBuffer.insertLink() {
    val selStart = selection.min
    val label = selectedText()
    val placeholder = "url"
    replace(selStart, selection.max, "[$label]($placeholder)")
    val urlStart = selStart + label.length + 3
    selection = if (label.isEmpty()) {
        TextRange(selStart + 1)
    } else {
        TextRange(urlStart, urlStart + placeholder.length)
    }
}

/**
 * Adds the prefix to every line touched by the selection, or removes it when all of them already have it.
 * Any other block prefix (list, quote, heading) is replaced, so lists can be converted into each other.
 */
private fun TextFieldBuffer.toggleLinePrefix(
    detect: Regex,
    prefixFor: (index: Int) -> String
) {
    val text = asCharSequence().toString()
    val selStart = selection.min
    val selEnd = selection.max

    val lineStarts = mutableListOf<Int>()
    var lineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let {
        if (selStart == 0) 0 else it + 1
    }
    while (true) {
        lineStarts += lineStart
        val next = text.indexOf('\n', lineStart)
        if (next == -1 || next >= selEnd) break
        lineStart = next + 1
    }

    fun lineAt(start: Int): String {
        val end = text.indexOf('\n', start).let { if (it == -1) text.length else it }
        return text.substring(start, end)
    }

    val removing = lineStarts.all { detect.find(lineAt(it))?.range?.first == 0 }
    var firstDelta = 0
    var totalDelta = 0

    // Walk backwards so earlier offsets stay valid.
    lineStarts.indices.reversed().forEach { index ->
        val start = lineStarts[index]
        val line = lineAt(start)
        val existing = listOf(taskPrefix, bulletPrefix, numberedPrefix, quotePrefix, headingPrefix)
            .firstNotNullOfOrNull { regex -> regex.find(line)?.takeIf { it.range.first == 0 } }
        val existingLength = existing?.value?.length ?: 0
        val newPrefix = if (removing) "" else prefixFor(index)
        replace(start, start + existingLength, newPrefix)
        val delta = newPrefix.length - existingLength
        totalDelta += delta
        if (index == 0) firstDelta = delta
    }

    val newStart = (selStart + firstDelta).coerceIn(lineStarts.first(), length)
    val newEnd = (selEnd + totalDelta).coerceIn(newStart, length)
    selection = TextRange(newStart, newEnd)
}
