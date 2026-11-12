package com.sosauce.azuki.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sosauce.azuki.notes.domain.NoteSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val PALETTE_STYLE = stringPreferencesKey("palette_style")
        val USE_SYSTEM_FONT = booleanPreferencesKey("use_system_font")
        val DEFAULT_OPEN_MODE = stringPreferencesKey("default_open_mode")
        val NOTES_LAYOUT = stringPreferencesKey("notes_layout")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val SHOW_MARKDOWN_TOOLBAR = booleanPreferencesKey("show_markdown_toolbar")
        val SORT = stringPreferencesKey("sort")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .map { prefs ->
            val defaults = AppSettings()
            AppSettings(
                theme = prefs[Keys.THEME] ?: defaults.theme,
                paletteStyle = prefs[Keys.PALETTE_STYLE] ?: defaults.paletteStyle,
                useSystemFont = prefs[Keys.USE_SYSTEM_FONT] ?: defaults.useSystemFont,
                defaultOpenMode = prefs[Keys.DEFAULT_OPEN_MODE].toEnum(defaults.defaultOpenMode),
                notesLayout = prefs[Keys.NOTES_LAYOUT].toEnum(defaults.notesLayout),
                editorFontSize = prefs[Keys.EDITOR_FONT_SIZE] ?: defaults.editorFontSize,
                showMarkdownToolbar = prefs[Keys.SHOW_MARKDOWN_TOOLBAR]
                    ?: defaults.showMarkdownToolbar,
                sort = prefs[Keys.SORT].toEnum(defaults.sort),
                sortAscending = prefs[Keys.SORT_ASCENDING] ?: defaults.sortAscending
            )
        }
        .distinctUntilChanged()

    suspend fun setTheme(theme: String) = set(Keys.THEME, theme)
    suspend fun setPaletteStyle(style: String) = set(Keys.PALETTE_STYLE, style)
    suspend fun setUseSystemFont(use: Boolean) = set(Keys.USE_SYSTEM_FONT, use)
    suspend fun setDefaultOpenMode(mode: OpenMode) = set(Keys.DEFAULT_OPEN_MODE, mode.name)
    suspend fun setNotesLayout(layout: NotesLayout) = set(Keys.NOTES_LAYOUT, layout.name)
    suspend fun setEditorFontSize(size: Int) = set(Keys.EDITOR_FONT_SIZE, size)
    suspend fun setShowMarkdownToolbar(show: Boolean) = set(Keys.SHOW_MARKDOWN_TOOLBAR, show)
    suspend fun setSort(sort: NoteSort) = set(Keys.SORT, sort.name)
    suspend fun setSortAscending(ascending: Boolean) = set(Keys.SORT_ASCENDING, ascending)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
