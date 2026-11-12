package com.sosauce.azuki.settings.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.azuki.R
import com.sosauce.azuki.core.presentation.UiText
import com.sosauce.azuki.notes.domain.Tag
import com.sosauce.azuki.notes.domain.TagRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ManageTagsState(
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface ManageTagsAction {
    data class OnCreateTag(val name: String) : ManageTagsAction
    data class OnRenameTag(val tagId: Long, val name: String) : ManageTagsAction
    data class OnDeleteTag(val tagId: Long) : ManageTagsAction
}

sealed interface ManageTagsEvent {
    data class ShowMessage(val message: UiText) : ManageTagsEvent
}

class ManageTagsViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ManageTagsState())
    val state = _state.asStateFlow()

    private val _events = Channel<ManageTagsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeTags().collect { tags ->
                _state.update { it.copy(tags = tags, isLoading = false) }
            }
        }
    }

    fun onAction(action: ManageTagsAction) {
        viewModelScope.launch {
            when (action) {
                is ManageTagsAction.OnCreateTag -> {
                    if (action.name.isNotBlank()) tagRepository.createTag(action.name)
                }

                is ManageTagsAction.OnRenameTag -> {
                    if (action.name.isBlank()) return@launch
                    val renamed = tagRepository.renameTag(action.tagId, action.name)
                    if (!renamed) {
                        _events.send(
                            ManageTagsEvent.ShowMessage(
                                UiText.StringResource(
                                    R.string.tag_already_exists,
                                    arrayOf(action.name.trim())
                                )
                            )
                        )
                    }
                }

                is ManageTagsAction.OnDeleteTag -> tagRepository.deleteTag(action.tagId)
            }
        }
    }
}
