package com.sosauce.azuki.notes.presentation.list

import androidx.compose.runtime.Stable
import com.sosauce.azuki.core.preferences.NotesLayout
import com.sosauce.azuki.core.presentation.UiText
import com.sosauce.azuki.notes.domain.NoteSort
import com.sosauce.azuki.notes.domain.Tag
import com.sosauce.azuki.notes.presentation.shared.NoteUi

@Stable
data class NoteListState(
    val pinnedNotes: List<NoteUi> = emptyList(),
    val otherNotes: List<NoteUi> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val sort: NoteSort = NoteSort.UPDATED,
    val sortAscending: Boolean = false,
    val layout: NotesLayout = NotesLayout.GRID,
    val selectedNoteIds: Set<Long> = emptySet(),
    val totalNoteCount: Int = 0,
    val isFiltering: Boolean = false,
    val isLoading: Boolean = true
) {
    val visibleNotes: List<NoteUi> get() = pinnedNotes + otherNotes
    val isInSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

sealed interface NoteListAction {
    data class OnNoteClick(val noteId: Long) : NoteListAction
    data class OnNoteLongClick(val noteId: Long) : NoteListAction
    data object OnAddNoteClick : NoteListAction
    data class OnTagFilterClick(val tagId: Long?) : NoteListAction
    data class OnSortChange(val sort: NoteSort) : NoteListAction
    data class OnSortDirectionChange(val ascending: Boolean) : NoteListAction
    data object OnToggleLayoutClick : NoteListAction
    data object OnClearSelection : NoteListAction
    data object OnToggleSelectAll : NoteListAction
    data object OnPinSelectedClick : NoteListAction
    data object OnShareSelectedClick : NoteListAction
    data object OnTrashSelectedClick : NoteListAction
    data class OnUndoTrash(val noteIds: Set<Long>) : NoteListAction
    data object OnTrashClick : NoteListAction
    data object OnSettingsClick : NoteListAction
}

sealed interface NoteListEvent {
    /** @param noteId 0 creates a new note */
    data class NavigateToNote(val noteId: Long) : NoteListEvent
    data object NavigateToTrash : NoteListEvent
    data object NavigateToSettings : NoteListEvent
    data class ShareText(val text: String) : NoteListEvent
    data class NotesTrashed(val message: UiText, val noteIds: Set<Long>) : NoteListEvent
}
