@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.notes.presentation.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sosauce.azuki.R
import com.sosauce.azuki.core.ui.SeededTheme
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.helpers.rememberInteractionSource

private const val MAX_VISIBLE_TAGS = 3

@Composable
fun NoteCard(
    note: NoteUi,
    isSelected: Boolean,
    isListLayout: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SeededTheme(seedColor = note.seedColor?.let { Color(it) }) {
        NoteCardContent(
            note = note,
            hasColor = note.seedColor != null,
            isSelected = isSelected,
            isListLayout = isListLayout,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier
        )
    }
}

@Composable
private fun NoteCardContent(
    note: NoteUi,
    hasColor: Boolean,
    isSelected: Boolean,
    isListLayout: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier
) {
    val containerColor = noteContainerColor(hasColor)
    val contentColor = contentColorFor(containerColor)
    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed || isSelected) 0.95f else 1f,
        animationSpec = bouncySpec()
    )
    val corners by animateDpAsState(
        targetValue = if (isSelected) 36.dp else 24.dp,
        animationSpec = bouncySpec()
    )
    val shape = RoundedCornerShape(corners)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(containerColor)
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (note.title.isNotEmpty() || note.isPinned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = note.title.ifEmpty { stringResource(R.string.untitled) },
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee()
                        )
                        if (note.isPinned) {
                            Icon(
                                painter = painterResource(R.drawable.pin_filled),
                                contentDescription = stringResource(R.string.pinned),
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
                if (note.preview.isNotEmpty()) {
                    Text(
                        text = note.preview,
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = contentColor.copy(alpha = 0.75f),
                        maxLines = if (isListLayout) 3 else 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (note.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        note.tags.take(MAX_VISIBLE_TAGS).forEach { tag -> TagPill(tag.name) }
                        if (note.tags.size > MAX_VISIBLE_TAGS) {
                            TagPill("+${note.tags.size - MAX_VISIBLE_TAGS}")
                        }
                    }
                }
                Text(
                    text = note.formattedDate,
                    style = MaterialTheme.typography.bodySmallEmphasized,
                    color = contentColor.copy(alpha = 0.6f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(bouncySpec()),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        MaterialShapes.Cookie9Sided.toShape()
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TagPill(text: String) {
    Surface(
        shape = CircleShape,
        color = LocalContentColor.current.copy(alpha = 0.1f),
        contentColor = LocalContentColor.current
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMediumEmphasized,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
