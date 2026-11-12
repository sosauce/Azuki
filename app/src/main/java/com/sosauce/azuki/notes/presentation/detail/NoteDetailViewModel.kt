@file:OptIn(FlowPreview::class)

package com.sosauce.azuki.notes.presentation.detail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.azuki.core.preferences.OpenMode
import com.sosauce.azuki.core.preferences.SettingsRepository
import com.sosauce.azuki.core.ui.components.markdown.MarkdownParser
import com.sosauce.azuki.notes.domain.Note
import com.sosauce.azuki.notes.domain.NoteRepository
import com.sosauce.azuki.notes.domain.TagRepository
import com.sosauce.azuki.notes.presentation.shared.toFullDateTime
import com.sosauce.azuki.notes.presentation.shared.toShareText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val KEY_NOTE_ID = "note_id"
private const val AUTOSAVE_DEBOUNCE_MS = 600L

/**
 * @param noteId id of the note to open, 0 for a new note (created on the first non-empty save)
 * @param appScope outlives this ViewModel, used to flush the last edits when the screen closes
 */
class NoteDetailViewModel(
    noteId: Long,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val settingsRepository: SettingsRepository,
    private val appScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(NoteDetailState())
    val state = _state.asStateFlow()

    private val _events = Channel<NoteDetailEvent>()
    val events = _events.receiveAsFlow()

    val titleState = TextFieldState()
    val contentState = TextFieldState()

    /** Last version written to (or read from) the database, null until the note exists. */
    private var savedNote: Note? = null
    private var tagsDirty = false
    private var isTrashed = false
    private val saveMutex = Mutex()

    init {
        // A new note that was already saved keeps its id across process death.
        val initialId = savedStateHandle.get<Long>(KEY_NOTE_ID) ?: noteId

        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val note = if (initialId != 0L) noteRepository.getNote(initialId) else null
            if (note != null) {
                savedNote = note
                titleState.setTextAndPlaceCursorAtEnd(note.title)
                contentState.setTextAndPlaceCursorAtEnd(note.content)
            }
            _state.update {
                it.copy(
                    noteId = note?.id ?: 0L,
                    isLoading = false,
                    isEditing = note == null || settings.defaultOpenMode == OpenMode.EDIT,
                    seedColor = note?.seedColor ?: it.seedColor,
                    isPinned = note?.isPinned ?: false,
                    selectedTagIds = note?.tags?.map { tag -> tag.id }?.toSet() ?: emptySet(),
                    createdAt = note?.createdAt?.toFullDateTime().orEmpty(),
                    updatedAt = note?.updatedAt?.toFullDateTime().orEmpty()
                )
            }

            snapshotFlow { titleState.text.toString() to contentState.text.toString() }
                .drop(1)
                .debounce(AUTOSAVE_DEBOUNCE_MS)
                .collect { (title, content) -> save(title, content) }
        }

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        editorFontSize = settings.editorFontSize,
                        showMarkdownToolbar = settings.showMarkdownToolbar
                    )
                }
            }
        }

        viewModelScope.launch {
            tagRepository.observeTags().collect { tags ->
                _state.update { current ->
                    current.copy(
                        allTags = tags,
                        selectedTagIds = current.selectedTagIds.filterTo(mutableSetOf()) { id ->
                            tags.any { it.id == id }
                        }
                    )
                }
            }
        }
    }

    fun onAction(action: NoteDetailAction) {
        when (action) {
            NoteDetailAction.OnToggleEditMode -> _state.update { it.copy(isEditing = !it.isEditing) }
            NoteDetailAction.OnTogglePin -> {
                _state.update { it.copy(isPinned = !it.isPinned) }
                saveAsync()
            }

            is NoteDetailAction.OnColorChange -> {
                _state.update { it.copy(seedColor = action.seedColor) }
                saveAsync()
            }

            is NoteDetailAction.OnToggleTag -> {
                _state.update { current ->
                    val ids = current.selectedTagIds
                    current.copy(selectedTagIds = if (action.tagId in ids) ids - action.tagId else ids + action.tagId)
                }
                tagsDirty = true
                saveAsync()
            }

            is NoteDetailAction.OnCreateTag -> viewModelScope.launch {
                if (action.name.isBlank()) return@launch
                val id = tagRepository.createTag(action.name)
                _state.update { it.copy(selectedTagIds = it.selectedTagIds + id) }
                tagsDirty = true
                save()
            }

            is NoteDetailAction.OnToggleTask -> {
                val updated =
                    MarkdownParser.toggleTask(contentState.text.toString(), action.lineIndex)
                contentState.setTextAndPlaceCursorAtEnd(updated)
            }

            is NoteDetailAction.OnFormat -> contentState.edit { applyMarkdownFormat(action.format) }
            is NoteDetailAction.OnShareClick -> viewModelScope.launch {
                _events.send(NoteDetailEvent.ShareText(currentNoteSnapshot().toShareText()))
            }

            is NoteDetailAction.OnCopyClick -> viewModelScope.launch {
                _events.send(NoteDetailEvent.CopyText(currentNoteSnapshot().toShareText()))
            }

            is NoteDetailAction.OnMoveToTrashClick -> viewModelScope.launch {
                save()
                savedNote?.let { noteRepository.moveToTrash(listOf(it.id)) }
                isTrashed = true
                _events.send(NoteDetailEvent.NoteTrashed)
            }
        }
    }

    private fun currentNoteSnapshot(): Note = Note(
        title = titleState.text.toString(),
        content = contentState.text.toString()
    )

    private fun saveAsync() {
        viewModelScope.launch { save() }
    }

    private suspend fun save(
        title: String = titleState.text.toString(),
        content: String = contentState.text.toString()
    ) = saveMutex.withLock {
        if (isTrashed) return@withLock
        val current = _state.value
        val base = savedNote
        if (base == null && title.isBlank() && content.isBlank()) return@withLock

        val contentChanged = base == null || base.title != title || base.content != content
        val metaChanged =
            base == null || base.seedColor != current.seedColor || base.isPinned != current.isPinned
        if (!contentChanged && !metaChanged && !tagsDirty) return@withLock

        val now = System.currentTimeMillis()
        val toSave = (base ?: Note(createdAt = now, updatedAt = now)).copy(
            title = title,
            content = content,
            seedColor = current.seedColor,
            isPinned = current.isPinned,
            updatedAt = if (contentChanged) now else base.updatedAt ?: now
        )
        val id = noteRepository.saveNote(toSave)
        if (tagsDirty || base == null) {
            noteRepository.setTags(id, current.selectedTagIds)
            tagsDirty = false
        }
        savedNote = toSave.copy(id = id)
        savedStateHandle[KEY_NOTE_ID] = id
        _state.update {
            it.copy(
                noteId = id,
                createdAt = toSave.createdAt.toFullDateTime(),
                updatedAt = toSave.updatedAt.toFullDateTime()
            )
        }
    }

    override fun onCleared() {
        // Capture the text now, the save itself runs after this ViewModel is gone.
        val title = titleState.text.toString()
        val content = contentState.text.toString()
        appScope.launch {
            save(title, content)
            val note = savedNote
            if (!isTrashed && note != null && note.isBlank) {
                noteRepository.deleteForever(listOf(note.id))
            }
        }
        super.onCleared()
    }
}
