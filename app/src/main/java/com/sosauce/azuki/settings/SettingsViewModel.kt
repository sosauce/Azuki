package com.sosauce.azuki.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.azuki.core.preferences.AppSettings
import com.sosauce.azuki.core.preferences.NotesLayout
import com.sosauce.azuki.core.preferences.OpenMode
import com.sosauce.azuki.core.preferences.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val settings: AppSettings = AppSettings()
)

sealed interface SettingsAction {
    data class OnThemeChange(val theme: String) : SettingsAction
    data class OnPaletteStyleChange(val style: String) : SettingsAction
    data class OnUseSystemFontChange(val use: Boolean) : SettingsAction
    data class OnDefaultOpenModeChange(val mode: OpenMode) : SettingsAction
    data class OnNotesLayoutChange(val layout: NotesLayout) : SettingsAction
    data class OnEditorFontSizeChange(val size: Int) : SettingsAction
    data class OnShowMarkdownToolbarChange(val show: Boolean) : SettingsAction
    data object OnManageTagsClick : SettingsAction
}

sealed interface SettingsEvent {
    data object NavigateToManageTags : SettingsEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        viewModelScope.launch {
            when (action) {
                is SettingsAction.OnThemeChange -> settingsRepository.setTheme(action.theme)
                is SettingsAction.OnPaletteStyleChange -> settingsRepository.setPaletteStyle(action.style)
                is SettingsAction.OnUseSystemFontChange -> settingsRepository.setUseSystemFont(
                    action.use
                )

                is SettingsAction.OnDefaultOpenModeChange -> settingsRepository.setDefaultOpenMode(
                    action.mode
                )

                is SettingsAction.OnNotesLayoutChange -> settingsRepository.setNotesLayout(action.layout)
                is SettingsAction.OnEditorFontSizeChange -> settingsRepository.setEditorFontSize(
                    action.size.coerceIn(
                        AppSettings.MIN_EDITOR_FONT_SIZE,
                        AppSettings.MAX_EDITOR_FONT_SIZE
                    )
                )

                is SettingsAction.OnShowMarkdownToolbarChange -> settingsRepository.setShowMarkdownToolbar(
                    action.show
                )

                SettingsAction.OnManageTagsClick -> _events.send(SettingsEvent.NavigateToManageTags)
            }
        }
    }
}
