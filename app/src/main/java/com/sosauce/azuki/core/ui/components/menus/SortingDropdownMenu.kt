@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.core.ui.components.menus

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sosauce.azuki.R
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.helpers.rememberInteractionSource

/**
 * Sort button opening an expressive menu: [content] holds the sort options (use
 * SelectableDropdownMenuItem), and an ascending/descending toggle pair sits underneath.
 */
@Composable
fun SortingDropdownMenu(
    isSortedAscending: Boolean,
    onChangeSorting: (ascending: Boolean) -> Unit,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sortDescription = stringResource(R.string.sort)

    Box {
        IconButton(
            onClick = { expanded = !expanded },
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.semantics { contentDescription = sortDescription }
        ) {
            AnimatedDrawable(
                drawable = AnimatedDrawableFile.SORT,
                atEnd = expanded
            )
        }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
                content(this) { expanded = false }
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                overflowIndicator = {}
            ) {
                listOf(true, false).forEach { ascending ->
                    customItem(
                        buttonGroupContent = {
                            val interactionSource = rememberInteractionSource()
                            val selected = isSortedAscending == ascending
                            val corners by animateDpAsState(if (selected) 50.dp else 12.dp)
                            FilledIconButton(
                                onClick = { onChangeSorting(ascending) },
                                interactionSource = interactionSource,
                                modifier = Modifier
                                    .animateWidth(interactionSource)
                                    .weight(1f)
                                    .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide)),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (selected) {
                                        MenuDefaults.groupVibrantContainerColor
                                    } else {
                                        MenuDefaults.groupStandardContainerColor
                                    }
                                ),
                                shape = RoundedCornerShape(corners)
                            ) {
                                @DrawableRes val icon =
                                    if (ascending) R.drawable.up else R.drawable.down
                                Icon(
                                    painter = painterResource(icon),
                                    contentDescription = stringResource(
                                        if (ascending) R.string.sort_ascending else R.string.sort_descending
                                    ),
                                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                                )
                            }
                        },
                        menuContent = {}
                    )
                }
            }
        }
    }
}
