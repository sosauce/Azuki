@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.core.ui.components.buttons

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sosauce.azuki.R
import com.sosauce.nekobites.animations.bouncySpec

/**
 * Selection toggle: selected = pill + primary with a check, unselected = 12dp + [unselectedColor].
 */
@Composable
fun CuteToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    unselectedColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    val corners by animateDpAsState(
        targetValue = if (selected) 50.dp else 12.dp,
        animationSpec = bouncySpec()
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else unselectedColor
    )

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(corners),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColorFor(if (selected) MaterialTheme.colorScheme.primary else unselectedColor)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 36.dp)
    ) {
        AnimatedVisibility(visible = selected || icon != null) {
            Icon(
                painter = painterResource(
                    if (selected) R.drawable.check else icon ?: R.drawable.check
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLargeEmphasized,
            maxLines = 1
        )
    }
}
