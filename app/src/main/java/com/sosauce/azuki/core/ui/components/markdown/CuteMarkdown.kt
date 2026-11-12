@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.core.ui.components.markdown

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sosauce.azuki.R
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.helpers.rememberInteractionSource

/**
 * Renders a Markdown subset (see [MarkdownParser]) with the app's Emphasized typography.
 *
 * @param fontSize body text size; headings keep their own scale
 * @param onToggleTask called with the source line index when a task checkbox is tapped, null = read-only
 */
@Composable
fun CuteMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 16.sp,
    onToggleTask: ((lineIndex: Int) -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val inlineStyles = MarkdownInlineStyles(
        linkColor = colors.primary,
        codeBackground = colors.surfaceContainerHighest
    )
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize,
        lineHeight = fontSize * 1.5f,
        color = colors.onSurface
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = parseInlineMarkdown(block.text, inlineStyles),
                    style = headingStyle(block.level),
                    color = colors.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                is MarkdownBlock.Paragraph -> Text(
                    text = parseInlineMarkdown(block.text, inlineStyles),
                    style = bodyStyle
                )

                is MarkdownBlock.Bullet -> ListRow(indent = block.indent) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(24.dp)
                            .height(with(LocalDensity.current) { bodyStyle.lineHeight.toDp() })
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(colors.primary, CircleShape)
                        )
                    }
                    Text(
                        text = parseInlineMarkdown(block.text, inlineStyles),
                        style = bodyStyle,
                        modifier = Modifier.weight(1f)
                    )
                }

                is MarkdownBlock.Numbered -> ListRow(indent = block.indent) {
                    Text(
                        text = "${block.number}.",
                        style = bodyStyle.merge(
                            MaterialTheme.typography.labelLargeEmphasized.copy(
                                fontSize = fontSize
                            )
                        ),
                        color = colors.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = parseInlineMarkdown(block.text, inlineStyles),
                        style = bodyStyle,
                        modifier = Modifier.weight(1f)
                    )
                }

                is MarkdownBlock.Task -> TaskRow(
                    task = block,
                    textStyle = bodyStyle,
                    inlineStyles = inlineStyles,
                    onToggle = onToggleTask?.let { toggle -> { toggle(block.lineIndex) } }
                )

                is MarkdownBlock.Quote -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceContainer)
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(4.dp)
                            .background(colors.primary)
                    )
                    Text(
                        text = parseInlineMarkdown(block.text, inlineStyles),
                        style = bodyStyle.copy(fontStyle = FontStyle.Italic),
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

                is MarkdownBlock.CodeBlock -> Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = block.code,
                        // Code is the one place where a monospace family makes sense.
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize * 0.9f
                        ),
                        color = colors.onSurface,
                        softWrap = false,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(14.dp)
                    )
                }

                MarkdownBlock.Divider -> Box(
                    Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(colors.surfaceContainerHighest, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineMediumEmphasized
    2 -> MaterialTheme.typography.headlineSmallEmphasized
    3 -> MaterialTheme.typography.titleLargeEmphasized
    else -> MaterialTheme.typography.titleMediumEmphasized
}

@Composable
private fun ListRow(
    indent: Int,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 16).dp),
        content = content
    )
}

@Composable
private fun TaskRow(
    task: MarkdownBlock.Task,
    textStyle: TextStyle,
    inlineStyles: MarkdownInlineStyles,
    onToggle: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = rememberInteractionSource()
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = bouncySpec()
    )
    val textColor by animateColorAsState(
        targetValue = if (task.checked) colors.onSurfaceVariant else colors.onSurface
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (task.indent * 16).dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = onToggle != null,
                interactionSource = interactionSource,
                indication = null
            ) { onToggle?.invoke() }
    ) {
        AnimatedContent(
            targetState = task.checked,
            transitionSpec = { scaleIn(bouncySpec()) togetherWith scaleOut() }
        ) { checked ->
            Icon(
                painter = painterResource(if (checked) R.drawable.check_box_filled else R.drawable.check_box_outline_blank),
                contentDescription = null,
                tint = if (checked) colors.primary else colors.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            text = parseInlineMarkdown(task.text, inlineStyles),
            style = textStyle.copy(
                textDecoration = if (task.checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}
