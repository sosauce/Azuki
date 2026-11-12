package com.sosauce.azuki.core.ui

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/** Centers (or start/end aligns) a floating bar inside its parent. */
fun Modifier.selfAlignHorizontally(
    align: Alignment.Horizontal = Alignment.CenterHorizontally,
    @FloatRange fraction: Float = 1f
): Modifier = fillMaxWidth(fraction).wrapContentWidth(align)

fun Modifier.thenIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier =
    if (condition) this.then(modifier()) else this

/** @param lastIndex last index of the list the DropdownMenuItems are iterated from */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuDefaults.getItemShape(index: Int, lastIndex: Int): Shape = when (index) {
    0 -> leadingItemShape
    lastIndex -> trailingItemShape
    else -> middleItemShape
}
