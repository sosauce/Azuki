@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sosauce.azuki.R
import com.sosauce.azuki.app.navigation.SettingsScreens
import com.sosauce.azuki.core.preferences.AppSettings
import com.sosauce.azuki.core.preferences.NotesLayout
import com.sosauce.azuki.core.preferences.OpenMode
import com.sosauce.azuki.core.ui.AzukiTheme
import com.sosauce.azuki.core.ui.CutePaletteStyle
import com.sosauce.azuki.core.ui.CuteTheme
import com.sosauce.azuki.core.ui.anyDarkColorScheme
import com.sosauce.azuki.core.ui.anyLightColorScheme
import com.sosauce.azuki.core.ui.components.buttons.CuteNavigationButton
import com.sosauce.azuki.core.ui.isDarkTheme
import com.sosauce.azuki.settings.components.PaletteTile
import com.sosauce.nekobites.components.settings.AboutCard
import com.sosauce.nekobites.components.settings.ScrollPage
import com.sosauce.nekobites.components.settings.SelectableTile
import com.sosauce.nekobites.components.settings.SettingOption
import com.sosauce.nekobites.components.settings.SettingsCard
import com.sosauce.nekobites.components.settings.SettingsChoice
import com.sosauce.nekobites.components.settings.SettingsEntry
import com.sosauce.nekobites.components.settings.SettingsGroup
import com.sosauce.nekobites.components.settings.SettingsPicker
import com.sosauce.nekobites.components.settings.SettingsSlider
import com.sosauce.nekobites.components.settings.SettingsSwitch
import com.sosauce.nekobites.helpers.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel

private const val GITHUB_RELEASES = "https://github.com/sosauce/Azuki/releases"

@Composable
fun SettingsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToManageTags: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.NavigateToManageTags -> onNavigateToManageTags()
        }
    }

    SettingsScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction
    )
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onNavigateBack: () -> Unit,
    onAction: (SettingsAction) -> Unit
) {
    val backStack = rememberNavBackStack(SettingsScreens.Home)
    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeLastOrNull() else onNavigateBack()
    }

    Scaffold(
        bottomBar = { CuteNavigationButton(onNavigateUp = goBack) }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = slideInHorizontally { it } + fadeIn(),
                    initialContentExit = slideOutHorizontally { -it / 4 } + fadeOut()
                )
            },
            popTransitionSpec = {
                ContentTransform(
                    targetContentEnter = slideInHorizontally { -it / 4 } + fadeIn(),
                    initialContentExit = slideOutHorizontally { it } + fadeOut()
                )
            },
            predictivePopTransitionSpec = {
                ContentTransform(
                    targetContentEnter = slideInHorizontally { -it / 4 } + fadeIn(),
                    initialContentExit = slideOutHorizontally { it } + fadeOut()
                )
            },
            entryProvider = entryProvider {
                entry<SettingsScreens.Home> {
                    SettingsHome(
                        contentPadding = paddingValues,
                        onNavigate = { backStack.add(it) },
                        onManageTagsClick = { onAction(SettingsAction.OnManageTagsClick) }
                    )
                }
                entry<SettingsScreens.LookAndFeel> {
                    ScrollPage(contentPadding = paddingValues) {
                        AppearanceSettings(settings = state.settings, onAction = onAction)
                    }
                }
                entry<SettingsScreens.Notes> {
                    ScrollPage(contentPadding = paddingValues) {
                        NotesSettings(settings = state.settings, onAction = onAction)
                    }
                }
                entry<SettingsScreens.Editor> {
                    ScrollPage(contentPadding = paddingValues) {
                        EditorSettings(settings = state.settings, onAction = onAction)
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsHome(
    contentPadding: PaddingValues,
    onNavigate: (SettingsScreens) -> Unit,
    onManageTagsClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val badgeColors = listOf(colors.primary, colors.secondary, colors.tertiary)
    val entries = listOf(
        SettingsEntry(
            icon = R.drawable.palette,
            name = stringResource(R.string.look_and_feel),
            description = stringResource(R.string.look_and_feel_desc),
            onClick = { onNavigate(SettingsScreens.LookAndFeel) }
        ),
        SettingsEntry(
            icon = R.drawable.note,
            name = stringResource(R.string.notes),
            description = stringResource(R.string.notes_settings_desc),
            onClick = { onNavigate(SettingsScreens.Notes) }
        ),
        SettingsEntry(
            icon = R.drawable.edit,
            name = stringResource(R.string.editor),
            description = stringResource(R.string.editor_settings_desc),
            onClick = { onNavigate(SettingsScreens.Editor) }
        ),
        SettingsEntry(
            icon = R.drawable.sell,
            name = stringResource(R.string.manage_tags),
            description = stringResource(R.string.manage_tags_desc),
            onClick = onManageTagsClick
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
            .padding(horizontal = 16.dp)
    ) {
        AboutCard(
            icon = R.drawable.app_icon,
            appName = stringResource(R.string.app_name),
            updateLink = GITHUB_RELEASES
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            entries.fastForEachIndexed { index, entry ->
                SettingsCard(
                    entry = entry,
                    isFirst = index == 0,
                    isLast = index == entries.lastIndex,
                    badgeContainer = badgeColors[index % badgeColors.size]
                )
            }
        }
    }
}

private data class ThemeOption(
    val value: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val background: Color,
    val iconColor: Color
)

@Composable
private fun AppearanceSettings(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val anyDark = anyDarkColorScheme()
    val anyLight = anyLightColorScheme()
    val themes = listOf(
        ThemeOption(
            value = CuteTheme.SYSTEM,
            label = R.string.theme_system,
            icon = R.drawable.system_theme,
            background = if (isSystemDark) anyDark.surfaceContainerHighest else anyLight.surfaceContainerHighest,
            iconColor = if (isSystemDark) anyDark.onSurface else anyLight.onSurface
        ),
        ThemeOption(
            value = CuteTheme.DARK,
            label = R.string.theme_dark,
            icon = R.drawable.dark_mode,
            background = anyDark.surfaceContainerHighest,
            iconColor = anyDark.onSurface
        ),
        ThemeOption(
            value = CuteTheme.LIGHT,
            label = R.string.theme_light,
            icon = R.drawable.light_mode,
            background = anyLight.surfaceContainerHighest,
            iconColor = anyLight.onSurface
        ),
        ThemeOption(
            value = CuteTheme.AMOLED,
            label = R.string.theme_amoled,
            icon = R.drawable.amoled,
            background = Color.Black,
            iconColor = Color.White
        )
    )
    val isDark = isDarkTheme(settings.theme, isSystemDark)

    SettingsGroup(title = stringResource(R.string.appearance)) {
        SettingsPicker(title = stringResource(R.string.theme)) {
            items(themes, key = { it.value }) { option ->
                SelectableTile(
                    selected = settings.theme == option.value,
                    onClick = { onAction(SettingsAction.OnThemeChange(option.value)) },
                    label = stringResource(option.label),
                    containerColor = option.background
                ) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = null,
                        tint = option.iconColor
                    )
                }
            }
        }
        SettingsPicker(title = stringResource(R.string.palette)) {
            items(CutePaletteStyle.all, key = { it }) { palette ->
                PaletteTile(
                    paletteStyle = palette,
                    selected = palette == settings.paletteStyle,
                    isDark = isDark,
                    isAmoled = settings.theme == CuteTheme.AMOLED,
                    onClick = { onAction(SettingsAction.OnPaletteStyleChange(palette)) }
                )
            }
        }
        SettingsPicker(title = stringResource(R.string.font)) {
            item {
                FontTile(
                    selected = !settings.useSystemFont,
                    onClick = { onAction(SettingsAction.OnUseSystemFontChange(false)) },
                    icon = R.drawable.match_case,
                    label = R.string.font_default
                )
            }
            item {
                FontTile(
                    selected = settings.useSystemFont,
                    onClick = { onAction(SettingsAction.OnUseSystemFontChange(true)) },
                    icon = R.drawable.system_font,
                    label = R.string.font_system
                )
            }
        }
    }
}

@Composable
private fun FontTile(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes icon: Int,
    @StringRes label: Int
) {
    SelectableTile(
        selected = selected,
        onClick = onClick,
        label = stringResource(label)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun NotesSettings(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit
) {
    SettingsGroup(title = stringResource(R.string.notes)) {
        SettingsChoice(
            title = stringResource(R.string.default_open_mode),
            description = stringResource(R.string.default_open_mode_desc),
            active = settings.defaultOpenMode,
            options = listOf(
                SettingOption(OpenMode.VIEW, R.string.view_mode),
                SettingOption(OpenMode.EDIT, R.string.edit_mode)
            ),
            onSelect = { onAction(SettingsAction.OnDefaultOpenModeChange(it)) }
        )
        SettingsChoice(
            title = stringResource(R.string.notes_layout),
            active = settings.notesLayout,
            options = listOf(
                SettingOption(NotesLayout.GRID, R.string.grid_view),
                SettingOption(NotesLayout.LIST, R.string.list_view)
            ),
            onSelect = { onAction(SettingsAction.OnNotesLayoutChange(it)) }
        )
    }
}

@Composable
private fun EditorSettings(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit
) {
    SettingsGroup(title = stringResource(R.string.editor)) {
        SettingsSlider(
            title = stringResource(R.string.editor_font_size),
            value = settings.editorFontSize,
            onValueChange = { onAction(SettingsAction.OnEditorFontSizeChange(it)) },
            unit = stringResource(R.string.unit_sp),
            valueRange = AppSettings.MIN_EDITOR_FONT_SIZE.toFloat()..AppSettings.MAX_EDITOR_FONT_SIZE.toFloat()
        )
        SettingsSwitch(
            title = stringResource(R.string.markdown_toolbar),
            description = stringResource(R.string.markdown_toolbar_desc),
            icon = R.drawable.text_fields,
            checked = settings.showMarkdownToolbar,
            onCheckedChange = { onAction(SettingsAction.OnShowMarkdownToolbarChange(it)) }
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    AzukiTheme {
        SettingsScreen(state = SettingsState(), onNavigateBack = {}, onAction = {})
    }
}
