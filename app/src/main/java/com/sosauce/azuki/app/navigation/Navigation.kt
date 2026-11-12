package com.sosauce.azuki.app.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sosauce.azuki.notes.presentation.detail.NoteDetailRoot
import com.sosauce.azuki.notes.presentation.list.NoteListRoot
import com.sosauce.azuki.notes.presentation.trash.TrashRoot
import com.sosauce.azuki.settings.SettingsRoot
import com.sosauce.azuki.settings.tags.ManageTagsRoot

@Composable
fun AzukiNavigation() {
    val backStack = rememberNavBackStack(Screen.NoteList)

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
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
            entry<Screen.NoteList> {
                NoteListRoot(
                    onNavigateToNote = { backStack.navigate(Screen.NoteDetail(it)) },
                    onNavigateToTrash = { backStack.navigate(Screen.Trash) },
                    onNavigateToSettings = { backStack.navigate(Screen.Settings) }
                )
            }
            entry<Screen.NoteDetail> { key ->
                NoteDetailRoot(
                    noteId = key.noteId,
                    onNavigateBack = backStack::navigateBack
                )
            }
            entry<Screen.Trash> {
                TrashRoot(onNavigateBack = backStack::navigateBack)
            }
            entry<Screen.Settings> {
                SettingsRoot(
                    onNavigateBack = backStack::navigateBack,
                    onNavigateToManageTags = { backStack.navigate(Screen.ManageTags) }
                )
            }
            entry<Screen.ManageTags> {
                ManageTagsRoot(onNavigateBack = backStack::navigateBack)
            }
        }
    )
}

private fun NavBackStack<NavKey>.navigate(screen: Screen) {
    remove(screen)
    add(screen)
}

private fun NavBackStack<NavKey>.navigateBack() {
    if (size > 1) removeLastOrNull()
}
