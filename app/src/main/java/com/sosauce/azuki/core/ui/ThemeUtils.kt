package com.sosauce.azuki.core.ui

import android.os.Build
import android.view.Window
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.materialkolor.PaletteStyle

/** Theme preference values. Store these strings in DataStore and never rename them once shipped. */
object CuteTheme {
    const val SYSTEM = "SYSTEM"
    const val DARK = "DARK"
    const val LIGHT = "LIGHT"
    const val AMOLED = "AMOLED"
}

/** Wallpaper-derived seed on Android 12+, baseline M3 primary below. */
@Composable
fun rememberSeedColor(isDark: Boolean): Color {
    return if (isDark) anyDarkColorScheme().primary else anyLightColorScheme().primary
}

@Composable
fun anyLightColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else lightColorScheme()
}

@Composable
fun anyDarkColorScheme(): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else darkColorScheme()
}

object SystemUiController {
    fun setSystemBarsColors(window: Window, isLight: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
        }
    }
}

/** Palette style preference values (also used as the tile label). Never rename once shipped. */
object CutePaletteStyle {
    const val TONAL_SPOT = "Tonal spot"
    const val EXPRESSIVE = "Expressive"
    const val VIBRANT = "Vibrant"
    const val FIDELITY = "Fidelity"
    const val NEUTRAL = "Neutral"
    const val MONOCHROME = "Monochrome"
    const val FRUIT_SALAD = "Fruit salad"

    val all = listOf(TONAL_SPOT, EXPRESSIVE, VIBRANT, FIDELITY, NEUTRAL, MONOCHROME, FRUIT_SALAD)
}

fun String.toPaletteStyle(): PaletteStyle = when (this) {
    CutePaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
    CutePaletteStyle.VIBRANT -> PaletteStyle.Vibrant
    CutePaletteStyle.FIDELITY -> PaletteStyle.Fidelity
    CutePaletteStyle.NEUTRAL -> PaletteStyle.Neutral
    CutePaletteStyle.MONOCHROME -> PaletteStyle.Monochrome
    CutePaletteStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
    else -> PaletteStyle.TonalSpot
}

fun isDarkTheme(theme: String, isSystemInDarkTheme: Boolean): Boolean = when (theme) {
    CuteTheme.SYSTEM -> isSystemInDarkTheme
    CuteTheme.DARK, CuteTheme.AMOLED -> true
    else -> false
}
