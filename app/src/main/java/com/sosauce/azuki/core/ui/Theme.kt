@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.core.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicMaterialThemeState
import com.sosauce.azuki.R

@Composable
fun AzukiTheme(
    theme: String = CuteTheme.SYSTEM,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    useSystemFont: Boolean = false,
    content: @Composable () -> Unit
) {
    val window = LocalActivity.current?.window
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val isDark = isDarkTheme(theme, isSystemInDarkTheme)
    val seedColor = rememberSeedColor(isDark = isDark)

    val state = rememberDynamicMaterialThemeState(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = theme == CuteTheme.AMOLED,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = paletteStyle
    )

    LaunchedEffect(isDark) {
        if (window == null) return@LaunchedEffect
        SystemUiController.setSystemBarsColors(window = window, isLight = !isDark)
    }

    val themeConfig = ThemeConfig(
        isDark = isDark,
        isAmoled = theme == CuteTheme.AMOLED,
        paletteStyle = paletteStyle
    )

    CompositionLocalProvider(LocalThemeConfig provides themeConfig) {
        DynamicMaterialExpressiveTheme(
            state = state,
            motionScheme = MotionScheme.expressive(),
            typography = if (useSystemFont) MaterialTheme.typography else GoogleSansFlexTypography,
            content = content
        )
    }
}

/** The resolved theme settings, so nested seeded themes (e.g. colored notes) can match the app. */
@Immutable
data class ThemeConfig(
    val isDark: Boolean = false,
    val isAmoled: Boolean = false,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot
)

val LocalThemeConfig = staticCompositionLocalOf { ThemeConfig() }

/**
 * Re-themes [content] with a full MaterialKolor scheme generated from [seedColor], following the app's
 * light/dark, AMOLED and palette style settings. A null seed keeps the current theme.
 */
@Composable
fun SeededTheme(
    seedColor: Color?,
    content: @Composable () -> Unit
) {
    if (seedColor == null) {
        content()
        return
    }
    val config = LocalThemeConfig.current
    val state = rememberDynamicMaterialThemeState(
        seedColor = seedColor,
        isDark = config.isDark,
        isAmoled = config.isAmoled,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = config.paletteStyle
    )
    DynamicMaterialExpressiveTheme(
        state = state,
        motionScheme = MaterialTheme.motionScheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        animate = true,
        content = content
    )
}

val googleSansFlexFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 100f),
            FontVariation.weight(750),
            FontVariation.width(110f)
        )
    )
)

/** For special display text (big numbers, hero words) that needs a custom width at weight 900. */
fun sansFlexCustomWidth(width: Float): FontFamily {
    return FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("ROND", 100f),
                FontVariation.Setting("wght", 900f),
                FontVariation.width(width)
            )
        )
    )
}

val GoogleSansFlexTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = googleSansFlexFontFamily),
        displayMedium = displayMedium.copy(fontFamily = googleSansFlexFontFamily),
        displaySmall = displaySmall.copy(fontFamily = googleSansFlexFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = googleSansFlexFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = googleSansFlexFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = googleSansFlexFontFamily),
        titleLarge = titleLarge.copy(fontFamily = googleSansFlexFontFamily),
        titleMedium = titleMedium.copy(fontFamily = googleSansFlexFontFamily),
        titleSmall = titleSmall.copy(fontFamily = googleSansFlexFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = googleSansFlexFontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = googleSansFlexFontFamily),
        bodySmall = bodySmall.copy(fontFamily = googleSansFlexFontFamily),
        labelLarge = labelLarge.copy(fontFamily = googleSansFlexFontFamily),
        labelMedium = labelMedium.copy(fontFamily = googleSansFlexFontFamily),
        labelSmall = labelSmall.copy(fontFamily = googleSansFlexFontFamily),
        displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = googleSansFlexFontFamily),
        labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = googleSansFlexFontFamily)
    )
}
