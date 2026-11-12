package com.sosauce.azuki.core.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.sosauce.nekobites.animations.bouncySpec

/** Use when swapping bottom bars (search bar <-> selection bar, toolbar variants). */
val barsContentTransform = ContentTransform(
    targetContentEnter = slideInVertically(bouncySpec()) { it } + fadeIn(),
    initialContentExit = slideOutVertically(bouncySpec()) { it } + fadeOut(),
    sizeTransform = SizeTransform(clip = false)
)
