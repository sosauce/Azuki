package com.sosauce.azuki.core.presentation

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class StringResource(@StringRes val id: Int, val args: Array<Any> = emptyArray()) : UiText
    class PluralResource(
        @PluralsRes val id: Int,
        val count: Int,
        val args: Array<Any> = arrayOf(count)
    ) : UiText

    @Composable
    fun asString(): String {
        val resources = LocalResources.current
        return when (this) {
            is DynamicString -> value
            is StringResource -> resources.getString(id, *args)
            is PluralResource -> resources.getQuantityString(id, count, *args)
        }
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(id, *args)
        is PluralResource -> context.resources.getQuantityString(id, count, *args)
    }
}
