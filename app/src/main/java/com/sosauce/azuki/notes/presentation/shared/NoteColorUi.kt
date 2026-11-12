package com.sosauce.azuki.notes.presentation.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Container for a note card or the note screen. Call it inside the note's [com.sosauce.azuki.core.ui.SeededTheme]:
 * colored notes use the seeded scheme's primaryContainer, uncolored ones the app's surfaceContainer.
 */
@Composable
fun noteContainerColor(hasColor: Boolean): Color =
    if (hasColor) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
