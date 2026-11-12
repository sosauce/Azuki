package com.sosauce.azuki.notes.presentation.detail

import androidx.compose.runtime.Stable
import com.sosauce.azuki.core.preferences.AppSettings
import com.sosauce.azuki.notes.domain.Tag

@Stable
data class NoteDetailState(
    val noteId: Long = 0L,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val seedColor: Int? = null,
    val isPinned: Boolean = false,
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<Tag> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val editorFontSize: Int = AppSettings.DEFAULT_EDITOR_FONT_SIZE,
    val showMarkdownToolbar: Boolean = true
) {
    val tags: List<Tag> get() = allTags.filter { it.id in selectedTagIds }
}

sealed interface NoteDetailAction {
    data object OnToggleEditMode : NoteDetailAction
    data object OnTogglePin : NoteDetailAction
    data class OnColorChange(val seedColor: Int?) : NoteDetailAction
    data class OnToggleTag(val tagId: Long) : NoteDetailAction
    data class OnCreateTag(val name: String) : NoteDetailAction
    data class OnToggleTask(val lineIndex: Int) : NoteDetailAction
    data class OnFormat(val format: MarkdownFormat) : NoteDetailAction
    data object OnShareClick : NoteDetailAction
    data object OnCopyClick : NoteDetailAction
    data object OnMoveToTrashClick : NoteDetailAction
}

sealed interface NoteDetailEvent {
    data class ShareText(val text: String) : NoteDetailEvent
    data class CopyText(val text: String) : NoteDetailEvent
    data object NoteTrashed : NoteDetailEvent
}
