@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.core.ui.components.buttons

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sosauce.azuki.R
import com.sosauce.azuki.core.ui.selfAlignHorizontally
import com.sosauce.nekobites.animations.AnimatedFab

/** Bottom-left back button. Put it in Scaffold(bottomBar = …). */
@Composable
fun CuteNavigationButton(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) {
    CuteNavigationButtonSurface(
        modifier = modifier
            .selfAlignHorizontally(Alignment.Start)
            .navigationBarsPadding()
            .padding(start = 15.dp),
        onNavigateUp = onNavigateUp
    )
}

/** A [CuteNavigationButton] without any positioning modifier, for custom bottom rows. */
@Composable
fun CuteNavigationButtonSurface(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) {
    AnimatedFab(
        modifier = modifier,
        onClick = onNavigateUp,
        icon = R.drawable.back,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
