@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.notes.presentation.detail.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.sosauce.azuki.R
import com.sosauce.azuki.core.ui.components.buttons.CuteToggleChip
import com.sosauce.azuki.core.ui.components.items.CuteListItem
import com.sosauce.azuki.core.ui.components.items.CuteListItemDefaults
import com.sosauce.azuki.notes.domain.Tag
import com.sosauce.nekobites.components.ColorPickerDialog

/**
 * Picks any seed color for a note with the NekoBites color picker. The note's whole scheme is then
 * generated from it. [onColorChange] receives null when the color is removed.
 */
@Composable
fun NoteColorDialog(
    seedColor: Color?,
    onColorChange: (Color?) -> Unit,
    onDismissRequest: () -> Unit
) {
    ColorPickerDialog(
        initialColor = seedColor,
        onDismissRequest = onDismissRequest,
        icon = { Icon(painter = painterResource(R.drawable.palette), contentDescription = null) },
        title = { Text(stringResource(R.string.note_color)) },
        confirmButton = { color ->
            TextButton(onClick = { onColorChange(color) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            if (seedColor != null) {
                TextButton(onClick = { onColorChange(null) }, shapes = ButtonDefaults.shapes()) {
                    Text(
                        text = stringResource(R.string.remove_color),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
fun TagPickerDialog(
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    onToggleTag: (Long) -> Unit,
    onCreateTag: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val newTagState = rememberTextFieldState()
    val createTag = {
        val name = newTagState.text.toString().trim()
        if (name.isNotEmpty()) {
            onCreateTag(name)
            newTagState.clearText()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(painter = painterResource(R.drawable.sell), contentDescription = null) },
        title = { Text(stringResource(R.string.tags)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (allTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_tags_yet),
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allTags.forEach { tag ->
                            CuteToggleChip(
                                selected = tag.id in selectedTagIds,
                                onClick = { onToggleTag(tag.id) },
                                label = tag.name
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        state = newTagState,
                        modifier = Modifier.weight(1f),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        shape = FloatingToolbarDefaults.ContainerShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text(stringResource(R.string.new_tag), maxLines = 1) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        onKeyboardAction = { createTag() }
                    )
                    FilledIconButton(
                        onClick = createTag,
                        enabled = newTagState.text.isNotBlank(),
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = stringResource(R.string.create_tag)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.done))
            }
        }
    )
}

data class NoteStats(
    val words: Int,
    val characters: Int,
    val readingMinutes: Int
)

private val wordRegex = Regex("""\S+""")
private const val WORDS_PER_MINUTE = 200

fun computeNoteStats(plainText: String): NoteStats {
    val words = wordRegex.findAll(plainText).count()
    return NoteStats(
        words = words,
        characters = plainText.length,
        readingMinutes = if (words == 0) 0 else ((words + WORDS_PER_MINUTE - 1) / WORDS_PER_MINUTE)
    )
}

@Composable
fun NoteInfoDialog(
    stats: NoteStats,
    createdAt: String,
    updatedAt: String,
    onDismissRequest: () -> Unit
) {
    val rows = buildList {
        add(stringResource(R.string.words) to stats.words.toString())
        add(stringResource(R.string.characters) to stats.characters.toString())
        add(
            stringResource(R.string.reading_time) to
                    pluralStringResource(
                        R.plurals.minutes,
                        stats.readingMinutes,
                        stats.readingMinutes
                    )
        )
        if (createdAt.isNotEmpty()) add(stringResource(R.string.created) to createdAt)
        if (updatedAt.isNotEmpty()) add(stringResource(R.string.edited) to updatedAt)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(painter = painterResource(R.drawable.info), contentDescription = null) },
        title = { Text(stringResource(R.string.note_info)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rows.fastForEachIndexed { index, (label, value) ->
                    CuteListItem(
                        onClick = null,
                        shape = CuteListItemDefaults.getItemShape(index, rows.size),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        leadingContent = { Box(Modifier.width(4.dp)) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
