package com.sosauce.azuki.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {

    @Serializable
    data object NoteList : Screen

    /** @param noteId 0 opens a new note */
    @Serializable
    data class NoteDetail(val noteId: Long) : Screen

    @Serializable
    data object Trash : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object ManageTags : Screen
}
