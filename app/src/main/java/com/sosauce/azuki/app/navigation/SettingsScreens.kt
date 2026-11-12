package com.sosauce.azuki.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Keys for the back stack nested inside the settings screen. */
@Serializable
sealed interface SettingsScreens : NavKey {

    @Serializable
    data object Home : SettingsScreens

    @Serializable
    data object LookAndFeel : SettingsScreens

    @Serializable
    data object Notes : SettingsScreens

    @Serializable
    data object Editor : SettingsScreens
}
