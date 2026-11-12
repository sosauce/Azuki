@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.core.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.azuki.R
import com.sosauce.nekobites.animations.AnimatedCounter
import com.sosauce.nekobites.components.Spacer
import com.sosauce.nekobites.helpers.rememberInteractionSource

data class SelectionAction(
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

/** Bottom bar shown in selection mode: count/clear + select all on top, connected actions below. */
@Composable
fun SelectedBarSurface(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    actions: List<SelectionAction>,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColorFor(containerColor)
    )
    val allSelected = selectedCount == totalCount

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(0.85f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onClearSelection,
                shapes = ButtonDefaults.shapes(),
                colors = buttonColors
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.clear_selection)
                )
                Spacer(5.dp)
                AnimatedCounter(
                    count = selectedCount,
                    textStyle = MaterialTheme.typography.labelLargeEmphasized
                )
            }
            Button(
                onClick = onToggleSelectAll,
                shapes = ButtonDefaults.shapes(),
                colors = buttonColors
            ) {
                AnimatedContent(targetState = allSelected) { all ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(if (all) R.drawable.unselect_all else R.drawable.select_all),
                            contentDescription = null
                        )
                        Spacer(5.dp)
                        Text(
                            text = stringResource(if (all) R.string.unselect_all else R.string.select_all),
                            style = MaterialTheme.typography.labelLargeEmphasized
                        )
                    }
                }
            }
        }
        Spacer(5.dp)
        ConnectedActionsGroup(actions = actions)
    }
}

/** Connected button group (50dp outer ends, 4dp joins) of icon actions on surfaceContainer. */
@Composable
fun ConnectedActionsGroup(
    actions: List<SelectionAction>,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColorFor(containerColor)
    )

    ButtonGroup(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        overflowIndicator = {}
    ) {
        actions.fastForEachIndexed { index, action ->
            customItem(
                buttonGroupContent = {
                    val interactionSource = rememberInteractionSource()
                    Button(
                        onClick = action.onClick,
                        interactionSource = interactionSource,
                        shape = connectedShape(index, actions.lastIndex),
                        colors = buttonColors,
                        modifier = Modifier
                            .animateWidth(interactionSource)
                            .weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(action.icon),
                            contentDescription = stringResource(action.label),
                            tint = if (action.isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                menuContent = {}
            )
        }
    }
}

private fun connectedShape(index: Int, lastIndex: Int): Shape = when {
    lastIndex == 0 -> CircleShape
    index == 0 -> RoundedCornerShape(
        topStart = 50.dp,
        bottomStart = 50.dp,
        topEnd = 4.dp,
        bottomEnd = 4.dp
    )

    index == lastIndex -> RoundedCornerShape(
        topStart = 4.dp,
        bottomStart = 4.dp,
        topEnd = 50.dp,
        bottomEnd = 50.dp
    )

    else -> RoundedCornerShape(4.dp)
}
