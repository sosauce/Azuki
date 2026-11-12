package com.sosauce.azuki.notes.presentation.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.azuki.R
import com.sosauce.azuki.core.preferences.AppSettings
import com.sosauce.azuki.core.preferences.NotesLayout
import com.sosauce.azuki.core.preferences.SettingsRepository
import com.sosauce.azuki.core.presentation.UiText
import com.sosauce.azuki.notes.domain.Note
import com.sosauce.azuki.notes.domain.NoteRepository
import com.sosauce.azuki.notes.domain.NoteSort
import com.sosauce.azuki.notes.domain.TagRepository
import com.sosauce.azuki.notes.presentation.shared.toNoteUi
import com.sosauce.azuki.notes.presentation.shared.toShareText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteListViewModel(
    private val noteRepository: NoteRepository,
    tagRepository: TagRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NoteListState())
    val state = _state.asStateFlow()

    private val _events = Channel<NoteListEvent>()
    val events = _events.receiveAsFlow()

    val searchState = TextFieldState()

    private val selectedTagId = MutableStateFlow<Long?>(null)
    private var latestNotes: List<Note> = emptyList()

    init {
        val query = snapshotFlow { searchState.text.toString().trim() }.distinctUntilChanged()
        val notesAndTags =
            combine(noteRepository.observeNotes(), tagRepository.observeTags(), ::Pair)

        viewModelScope.launch {
            combine(
                notesAndTags,
                settingsRepository.settings,
                query,
                selectedTagId
            ) { (notes, tags), settings, q, tagId ->
                val validTagId = tagId?.takeIf { id -> tags.any { it.id == id } }
                latestNotes = notes
                val filtered = notes
                    .filter { note -> validTagId == null || note.tags.any { it.id == validTagId } }
                    .filter { note -> note.matches(q) }
                    .sortedWith(settings.comparator())
                    .map { it.toNoteUi() }

                _state.update { current ->
                    val visibleIds = filtered.map { it.id }.toSet()
                    current.copy(
                        pinnedNotes = filtered.filter { it.isPinned },
                        otherNotes = filtered.filterNot { it.isPinned },
                        tags = tags,
                        selectedTagId = validTagId,
                        sort = settings.sort,
                        sortAscending = settings.sortAscending,
                        layout = settings.notesLayout,
                        selectedNoteIds = current.selectedNoteIds intersect visibleIds,
                        totalNoteCount = notes.size,
                        isFiltering = q.isNotEmpty() || validTagId != null,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun onAction(action: NoteListAction) {
        when (action) {
            is NoteListAction.OnNoteClick -> {
                if (_state.value.isInSelectionMode) {
                    toggleSelection(action.noteId)
                } else {
                    send(NoteListEvent.NavigateToNote(action.noteId))
                }
            }

            is NoteListAction.OnNoteLongClick -> toggleSelection(action.noteId)
            NoteListAction.OnAddNoteClick -> send(NoteListEvent.NavigateToNote(0L))
            is NoteListAction.OnTagFilterClick -> selectedTagId.value = action.tagId
            is NoteListAction.OnSortChange -> viewModelScope.launch {
                settingsRepository.setSort(
                    action.sort
                )
            }

            is NoteListAction.OnSortDirectionChange -> viewModelScope.launch {
                settingsRepository.setSortAscending(action.ascending)
            }

            NoteListAction.OnToggleLayoutClick -> viewModelScope.launch {
                val layout =
                    if (_state.value.layout == NotesLayout.GRID) NotesLayout.LIST else NotesLayout.GRID
                settingsRepository.setNotesLayout(layout)
            }

            NoteListAction.OnClearSelection -> clearSelection()
            NoteListAction.OnToggleSelectAll -> _state.update { current ->
                val all = current.visibleNotes.map { it.id }.toSet()
                current.copy(selectedNoteIds = if (current.selectedNoteIds == all) emptySet() else all)
            }

            NoteListAction.OnPinSelectedClick -> viewModelScope.launch {
                val current = _state.value
                val selected = current.visibleNotes.filter { it.id in current.selectedNoteIds }
                val pin = !selected.all { it.isPinned }
                noteRepository.setPinned(current.selectedNoteIds, pin)
                clearSelection()
            }

            NoteListAction.OnShareSelectedClick -> {
                val ids = _state.value.selectedNoteIds
                val text = latestNotes
                    .filter { it.id in ids }
                    .joinToString("\n\n---\n\n") { it.toShareText() }
                send(NoteListEvent.ShareText(text))
                clearSelection()
            }

            NoteListAction.OnTrashSelectedClick -> viewModelScope.launch {
                val ids = _state.value.selectedNoteIds
                noteRepository.moveToTrash(ids)
                clearSelection()
                _events.send(
                    NoteListEvent.NotesTrashed(
                        message = UiText.PluralResource(R.plurals.notes_moved_to_trash, ids.size),
                        noteIds = ids
                    )
                )
            }

            is NoteListAction.OnUndoTrash -> viewModelScope.launch { noteRepository.restore(action.noteIds) }
            NoteListAction.OnTrashClick -> send(NoteListEvent.NavigateToTrash)
            NoteListAction.OnSettingsClick -> send(NoteListEvent.NavigateToSettings)
        }
    }

    private fun toggleSelection(noteId: Long) {
        _state.update { current ->
            val ids = current.selectedNoteIds
            current.copy(selectedNoteIds = if (noteId in ids) ids - noteId else ids + noteId)
        }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedNoteIds = emptySet()) }
    }

    private fun send(event: NoteListEvent) {
        viewModelScope.launch { _events.send(event) }
    }
}

private fun Note.matches(query: String): Boolean {
    return query.isEmpty() || title.contains(query, ignoreCase = true) ||
            content.contains(query, ignoreCase = true) ||
            tags.any { it.name.contains(query, ignoreCase = true) }
}

private fun AppSettings.comparator(): Comparator<Note> {
    val base: Comparator<Note> = when (sort) {
        NoteSort.UPDATED -> compareBy { it.updatedAt }
        NoteSort.CREATED -> compareBy { it.createdAt }
        NoteSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) {
            it.title.ifBlank { it.content }.trim()
        }
    }
    return if (sortAscending) base else base.reversed()
}
