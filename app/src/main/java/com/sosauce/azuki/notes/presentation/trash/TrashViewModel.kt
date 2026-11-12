package com.sosauce.azuki.notes.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.azuki.R
import com.sosauce.azuki.core.presentation.UiText
import com.sosauce.azuki.notes.domain.NoteRepository
import com.sosauce.azuki.notes.presentation.shared.NoteUi
import com.sosauce.azuki.notes.presentation.shared.toNoteUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashState(
    val notes: List<NoteUi> = emptyList(),
    val selectedNoteIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
) {
    val isInSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

sealed interface TrashAction {
    data class OnNoteClick(val noteId: Long) : TrashAction
    data object OnClearSelection : TrashAction
    data object OnToggleSelectAll : TrashAction
    data object OnRestoreSelected : TrashAction
    data object OnDeleteSelected : TrashAction
    data object OnEmptyTrash : TrashAction
}

sealed interface TrashEvent {
    data class ShowMessage(val message: UiText) : TrashEvent
}

class TrashViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrashState())
    val state = _state.asStateFlow()

    private val _events = Channel<TrashEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            noteRepository.observeTrashedNotes().collect { notes ->
                val uiNotes = notes.map { it.toNoteUi() }
                val ids = uiNotes.map { it.id }.toSet()
                _state.update {
                    it.copy(
                        notes = uiNotes,
                        selectedNoteIds = it.selectedNoteIds intersect ids,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onAction(action: TrashAction) {
        when (action) {
            is TrashAction.OnNoteClick -> _state.update { current ->
                val ids = current.selectedNoteIds
                current.copy(selectedNoteIds = if (action.noteId in ids) ids - action.noteId else ids + action.noteId)
            }

            TrashAction.OnClearSelection -> clearSelection()
            TrashAction.OnToggleSelectAll -> _state.update { current ->
                val all = current.notes.map { it.id }.toSet()
                current.copy(selectedNoteIds = if (current.selectedNoteIds == all) emptySet() else all)
            }

            TrashAction.OnRestoreSelected -> viewModelScope.launch {
                val ids = _state.value.selectedNoteIds
                noteRepository.restore(ids)
                clearSelection()
                _events.send(
                    TrashEvent.ShowMessage(
                        UiText.PluralResource(
                            R.plurals.notes_restored,
                            ids.size
                        )
                    )
                )
            }

            TrashAction.OnDeleteSelected -> viewModelScope.launch {
                val ids = _state.value.selectedNoteIds
                noteRepository.deleteForever(ids)
                clearSelection()
                _events.send(
                    TrashEvent.ShowMessage(
                        UiText.PluralResource(
                            R.plurals.notes_deleted,
                            ids.size
                        )
                    )
                )
            }

            TrashAction.OnEmptyTrash -> viewModelScope.launch {
                noteRepository.emptyTrash()
                _events.send(TrashEvent.ShowMessage(UiText.StringResource(R.string.trash_emptied)))
            }
        }
    }

    private fun clearSelection() {
        _state.update { it.copy(selectedNoteIds = emptySet()) }
    }
}
