@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.notes.presentation.detail.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.azuki.R
import com.sosauce.azuki.notes.presentation.detail.MarkdownFormat

private data class FormatButton(
    val format: MarkdownFormat,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int
)

private val formatButtons = listOf(
    FormatButton(MarkdownFormat.BOLD, R.drawable.format_bold, R.string.format_bold),
    FormatButton(MarkdownFormat.ITALIC, R.drawable.format_italic, R.string.format_italic),
    FormatButton(
        MarkdownFormat.STRIKETHROUGH,
        R.drawable.format_strikethrough,
        R.string.format_strikethrough
    ),
    FormatButton(MarkdownFormat.HEADING, R.drawable.format_h1, R.string.format_heading),
    FormatButton(MarkdownFormat.TASK_LIST, R.drawable.check_box, R.string.format_task_list),
    FormatButton(
        MarkdownFormat.BULLET_LIST,
        R.drawable.format_list_bulleted,
        R.string.format_bullet_list
    ),
    FormatButton(
        MarkdownFormat.NUMBERED_LIST,
        R.drawable.format_list_numbered,
        R.string.format_numbered_list
    ),
    FormatButton(MarkdownFormat.QUOTE, R.drawable.format_quote, R.string.format_quote),
    FormatButton(MarkdownFormat.CODE, R.drawable.code, R.string.format_code),
    FormatButton(MarkdownFormat.LINK, R.drawable.link, R.string.format_link)
)

/** Floating pill of Markdown formatting actions, shown above the bottom bar while editing. */
@Composable
fun MarkdownToolbar(
    onFormat: (MarkdownFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp)
        ) {
            formatButtons.forEach { button ->
                IconButton(
                    onClick = { onFormat(button.format) },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        painter = painterResource(button.icon),
                        contentDescription = stringResource(button.label)
                    )
                }
            }
        }
    }
}
