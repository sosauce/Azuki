package com.sosauce.azuki.core.preferences

import com.sosauce.azuki.core.ui.CutePaletteStyle
import com.sosauce.azuki.core.ui.CuteTheme
import com.sosauce.azuki.notes.domain.NoteSort

data class AppSettings(
    val theme: String = CuteTheme.SYSTEM,
    val paletteStyle: String = CutePaletteStyle.TONAL_SPOT,
    val useSystemFont: Boolean = false,
    val defaultOpenMode: OpenMode = OpenMode.VIEW,
    val notesLayout: NotesLayout = NotesLayout.GRID,
    val editorFontSize: Int = DEFAULT_EDITOR_FONT_SIZE,
    val showMarkdownToolbar: Boolean = true,
    val sort: NoteSort = NoteSort.UPDATED,
    val sortAscending: Boolean = false
) {
    companion object {
        const val DEFAULT_EDITOR_FONT_SIZE = 16
        const val MIN_EDITOR_FONT_SIZE = 12
        const val MAX_EDITOR_FONT_SIZE = 24
    }
}

/** How existing notes open. New notes always open in edit mode. */
enum class OpenMode { VIEW, EDIT }

enum class NotesLayout { GRID, LIST }
