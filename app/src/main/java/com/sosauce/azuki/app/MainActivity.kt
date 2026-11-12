package com.sosauce.azuki.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.azuki.app.navigation.AzukiNavigation
import com.sosauce.azuki.core.preferences.SettingsRepository
import com.sosauce.azuki.core.ui.AzukiTheme
import com.sosauce.azuki.core.ui.toPaletteStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read once up front so the first frame already uses the right theme (tiny local file).
        val initialSettings = runBlocking { settingsRepository.settings.first() }

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialSettings)

            AzukiTheme(
                theme = settings.theme,
                paletteStyle = settings.paletteStyle.toPaletteStyle(),
                useSystemFont = settings.useSystemFont
            ) {
                AzukiNavigation()
            }
        }
    }
}
