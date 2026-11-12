package com.sosauce.azuki.notes.presentation.shared

import android.text.format.DateUtils
import com.sosauce.azuki.core.ui.components.markdown.MarkdownParser
import com.sosauce.azuki.notes.domain.Note
import com.sosauce.azuki.notes.domain.Tag
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


data class NoteUi(
    val id: Long,
    val title: String,
    val preview: String,
    val seedColor: Int?,
    val isPinned: Boolean,
    val tags: List<Tag>,
    /** Relative "edited" time for the list, or "trashed" time in the trash. */
    val formattedDate: String
)

fun Note.toNoteUi(now: Long = System.currentTimeMillis()): NoteUi = NoteUi(
    id = id,
    title = title.trim(),
    preview = MarkdownParser.toPlainText(content).take(400),
    seedColor = seedColor,
    isPinned = isPinned,
    tags = tags,
    formattedDate = (trashedAt ?: updatedAt).toRelativeTime(now)
)

fun Long.toRelativeTime(now: Long = System.currentTimeMillis()): String =
    DateUtils.getRelativeTimeSpanString(this, now, DateUtils.MINUTE_IN_MILLIS).toString()

fun Long.toFullDateTime(): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(this))

/** Text used when sharing or copying a note. */
fun Note.toShareText(): String = listOf(title.trim(), content.trim())
    .filter { it.isNotEmpty() }
    .joinToString("\n\n")
