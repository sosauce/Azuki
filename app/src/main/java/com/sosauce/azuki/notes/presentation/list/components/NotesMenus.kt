@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.notes.presentation.list.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.azuki.R
import com.sosauce.azuki.core.preferences.NotesLayout
import com.sosauce.azuki.core.ui.components.menus.SortingDropdownMenu
import com.sosauce.azuki.core.ui.getItemShape
import com.sosauce.azuki.notes.domain.NoteSort
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile

private data class SortOption(val sort: NoteSort, @param:StringRes val label: Int)

private val sortOptions = listOf(
    SortOption(NoteSort.UPDATED, R.string.sort_date_edited),
    SortOption(NoteSort.CREATED, R.string.sort_date_created),
    SortOption(NoteSort.TITLE, R.string.sort_title)
)

@Composable
fun NotesSortingMenu(
    sort: NoteSort,
    ascending: Boolean,
    onSortChange: (NoteSort) -> Unit,
    onDirectionChange: (Boolean) -> Unit
) {
    SortingDropdownMenu(
        isSortedAscending = ascending,
        onChangeSorting = onDirectionChange
    ) { _ ->
        sortOptions.fastForEachIndexed { index, option ->
            SelectableDropdownMenuItem(
                selected = sort == option.sort,
                onClick = { onSortChange(option.sort) },
                shapes = MenuDefaults.itemShape(index, sortOptions.size),
                selectedLeadingIcon = {
                    Icon(painter = painterResource(R.drawable.check), contentDescription = null)
                },
                text = { Text(stringResource(option.label)) }
            )
        }
    }
}

private data class MenuOption(
    @param:DrawableRes val icon: Int,
    @param:StringRes val text: Int,
    val onClick: () -> Unit
)

@Composable
fun NotesMoreMenu(
    layout: NotesLayout,
    onToggleLayout: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val moreDescription = stringResource(R.string.more_options)
    val options = listOf(
        if (layout == NotesLayout.GRID) {
            MenuOption(R.drawable.view_agenda, R.string.list_view, onToggleLayout)
        } else {
            MenuOption(R.drawable.grid_view, R.string.grid_view, onToggleLayout)
        },
        MenuOption(R.drawable.delete, R.string.trash, onOpenTrash),
        MenuOption(R.drawable.settings_filled, R.string.settings, onOpenSettings)
    )

    Box {
        IconButton(
            onClick = { expanded = !expanded },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.semantics { contentDescription = moreDescription }
        ) {
            AnimatedDrawable(drawable = AnimatedDrawableFile.MORE_VERT, atEnd = expanded)
        }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                options.fastForEachIndexed { index, option ->
                    DropdownMenuItem(
                        onClick = {
                            option.onClick()
                            expanded = false
                        },
                        shape = MenuDefaults.getItemShape(index, options.lastIndex),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(option.icon),
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(option.text)) }
                    )
                }
            }
        }
    }
}
