@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.notes.presentation.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.azuki.R
import com.sosauce.azuki.core.ui.SeededTheme
import com.sosauce.azuki.core.ui.AzukiTheme
import com.sosauce.azuki.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.azuki.core.ui.components.markdown.CuteMarkdown
import com.sosauce.azuki.core.ui.components.markdown.MarkdownParser
import com.sosauce.azuki.core.ui.getItemShape
import com.sosauce.azuki.notes.domain.Tag
import com.sosauce.azuki.notes.presentation.detail.components.MarkdownToolbar
import com.sosauce.azuki.notes.presentation.detail.components.NoteColorDialog
import com.sosauce.azuki.notes.presentation.detail.components.NoteInfoDialog
import com.sosauce.azuki.notes.presentation.detail.components.TagPickerDialog
import com.sosauce.azuki.notes.presentation.detail.components.computeNoteStats
import com.sosauce.nekobites.animations.AnimatedDrawable
import com.sosauce.nekobites.animations.AnimatedDrawableFile
import com.sosauce.nekobites.animations.bouncySpec
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.helpers.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NoteDetailRoot(
    noteId: Long,
    onNavigateBack: () -> Unit,
    viewModel: NoteDetailViewModel = koinViewModel(parameters = { parametersOf(noteId) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NoteDetailEvent.ShareText -> context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                    },
                    null
                )
            )

            is NoteDetailEvent.CopyText -> {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard.primaryClip?.addItem(
                    ClipData.Item(event.text)
                )
                // Android 13+ shows its own confirmation.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            NoteDetailEvent.NoteTrashed -> {
                Toast.makeText(
                    context,
                    resources.getString(R.string.note_moved_to_trash),
                    Toast.LENGTH_SHORT
                ).show()
                onNavigateBack()
            }
        }
    }

    NoteDetailScreen(
        state = state,
        titleState = viewModel.titleState,
        contentState = viewModel.contentState,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction
    )
}

private enum class NoteDialog { COLOR, TAGS, INFO }

@Composable
fun NoteDetailScreen(
    state: NoteDetailState,
    titleState: TextFieldState,
    contentState: TextFieldState,
    onNavigateBack: () -> Unit,
    onAction: (NoteDetailAction) -> Unit
) {
    SeededTheme(seedColor = state.seedColor?.let { Color(it) }) {
        NoteDetailContent(
            state = state,
            titleState = titleState,
            contentState = contentState,
            onNavigateBack = onNavigateBack,
            onAction = onAction
        )
    }
}

@Composable
private fun NoteDetailContent(
    state: NoteDetailState,
    titleState: TextFieldState,
    contentState: TextFieldState,
    onNavigateBack: () -> Unit,
    onAction: (NoteDetailAction) -> Unit
) {
    var openDialog by rememberSaveable { mutableStateOf<NoteDialog?>(null) }


    when (openDialog) {
        NoteDialog.COLOR -> NoteColorDialog(
            seedColor = state.seedColor?.let { Color(it) },
            onColorChange = { color ->
                onAction(NoteDetailAction.OnColorChange(color?.toArgb()))
                openDialog = null
            },
            onDismissRequest = { openDialog = null }
        )

        NoteDialog.TAGS -> TagPickerDialog(
            allTags = state.allTags,
            selectedTagIds = state.selectedTagIds,
            onToggleTag = { onAction(NoteDetailAction.OnToggleTag(it)) },
            onCreateTag = { onAction(NoteDetailAction.OnCreateTag(it)) },
            onDismissRequest = { openDialog = null }
        )

        NoteDialog.INFO -> {
            val stats = remember(contentState.text) {
                computeNoteStats(MarkdownParser.toPlainText(contentState.text.toString()))
            }
            NoteInfoDialog(
                stats = stats,
                createdAt = state.createdAt,
                updatedAt = state.updatedAt,
                onDismissRequest = { openDialog = null }
            )
        }

        null -> Unit
    }

    LoadingBox(isLoading = state.isLoading) {
        Scaffold(
            bottomBar = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 15.dp)
                        .padding(bottom = 8.dp)
                ) {
                    AnimatedVisibility(
                        visible = state.isEditing && state.showMarkdownToolbar,
                        enter = slideInVertically(bouncySpec()) { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        MarkdownToolbar(onFormat = { onAction(NoteDetailAction.OnFormat(it)) })
                    }
                    DetailBottomBar(
                        state = state,
                        onNavigateBack = onNavigateBack,
                        onAction = onAction,
                        onOpenDialog = { openDialog = it }
                    )
                }
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = state.isEditing,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith (fadeOut() + scaleOut(
                        targetScale = 0.96f
                    ))
                },
                modifier = Modifier.fillMaxSize()
            ) { editing ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(
                            top = 20.dp,
                            bottom = paddingValues.calculateBottomPadding() + 20.dp
                        )
                ) {
                    if (editing) {
                        NoteEditor(
                            titleState = titleState,
                            contentState = contentState,
                            fontSize = state.editorFontSize
                        )
                    } else {
                        NoteViewer(
                            state = state,
                            title = titleState.text.toString(),
                            content = contentState.text.toString(),
                            onEditClick = { onAction(NoteDetailAction.OnToggleEditMode) },
                            onTagsClick = { openDialog = NoteDialog.TAGS },
                            onToggleTask = { onAction(NoteDetailAction.OnToggleTask(it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(
    titleState: TextFieldState,
    contentState: TextFieldState,
    fontSize: Int
) {
    val contentFocusRequester = remember { FocusRequester() }
    val textColor = LocalContentColor.current
    val placeholderColor = textColor.copy(alpha = 0.5f)
    val titleStyle = MaterialTheme.typography.headlineMediumEmphasized.copy(color = textColor)
    val contentStyle = MaterialTheme.typography.bodyLarge.copy(
        color = textColor,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.5f).sp
    )

    BasicTextField(
        state = titleState,
        textStyle = titleStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        onKeyboardAction = { contentFocusRequester.requestFocus() },
        modifier = Modifier.fillMaxWidth(),
        decorator = TextFieldDecorator { innerTextField ->
            Placeholder(
                isEmpty = titleState.text.isEmpty(),
                text = stringResource(R.string.title_hint),
                style = titleStyle.copy(color = placeholderColor),
                innerTextField = innerTextField
            )
        }
    )
    BasicTextField(
        state = contentState,
        textStyle = contentStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .focusRequester(contentFocusRequester),
        decorator = TextFieldDecorator { innerTextField ->
            Placeholder(
                isEmpty = contentState.text.isEmpty(),
                text = stringResource(R.string.content_hint),
                style = contentStyle.copy(color = placeholderColor),
                innerTextField = innerTextField
            )
        }
    )
}

@Composable
private fun Placeholder(
    isEmpty: Boolean,
    text: String,
    style: TextStyle,
    innerTextField: @Composable () -> Unit
) {
    Box {
        if (isEmpty) Text(text = text, style = style)
        innerTextField()
    }
}

@Composable
private fun NoteViewer(
    state: NoteDetailState,
    title: String,
    content: String,
    onEditClick: () -> Unit,
    onTagsClick: () -> Unit,
    onToggleTask: (Int) -> Unit
) {
    val secondary = LocalContentColor.current.copy(alpha = 0.7f)

    Text(
        text = title.ifBlank { stringResource(R.string.untitled) },
        style = MaterialTheme.typography.headlineMediumEmphasized,
        color = if (title.isBlank()) secondary else LocalContentColor.current
    )
    if (state.updatedAt.isNotEmpty()) {
        Text(
            text = stringResource(R.string.edited_on, state.updatedAt),
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = secondary,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
        )
    }
    if (state.tags.isNotEmpty()) {
        TagsRow(tags = state.tags, onClick = onTagsClick)
    }
    if (content.isBlank()) {
        Text(
            text = stringResource(R.string.empty_note_tap_to_edit),
            style = MaterialTheme.typography.bodyLargeEmphasized,
            color = secondary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEditClick
                )
        )
    } else {
        CuteMarkdown(
            markdown = content,
            fontSize = state.editorFontSize.sp,
            onToggleTask = onToggleTask,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TagsRow(tags: List<Tag>, onClick: () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                onClick = onClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sell),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class DetailMenuOption(
    @param:DrawableRes val icon: Int,
    @param:StringRes val text: Int,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun DetailBottomBar(
    state: NoteDetailState,
    onNavigateBack: () -> Unit,
    onAction: (NoteDetailAction) -> Unit,
    onOpenDialog: (NoteDialog) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val moreDescription = stringResource(R.string.more_options)
    val toggleDescription =
        stringResource(if (state.isEditing) R.string.view_mode else R.string.edit_mode)

    val menuOptions = listOf(
        DetailMenuOption(
            R.drawable.palette,
            R.string.note_color
        ) { onOpenDialog(NoteDialog.COLOR) },
        DetailMenuOption(R.drawable.sell, R.string.tags) { onOpenDialog(NoteDialog.TAGS) },
        DetailMenuOption(R.drawable.info, R.string.note_info) { onOpenDialog(NoteDialog.INFO) },
        DetailMenuOption(R.drawable.copy, R.string.copy) { onAction(NoteDetailAction.OnCopyClick) },
        DetailMenuOption(
            R.drawable.share,
            R.string.share
        ) { onAction(NoteDetailAction.OnShareClick) },
        DetailMenuOption(R.drawable.delete, R.string.move_to_trash, isDestructive = true) {
            onAction(NoteDetailAction.OnMoveToTrashClick)
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CuteNavigationButtonSurface(onNavigateUp = onNavigateBack)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Row {
                IconButton(
                    onClick = { onAction(NoteDetailAction.OnToggleEditMode) },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.semantics { contentDescription = toggleDescription }
                ) {
                    AnimatedContent(
                        targetState = state.isEditing,
                        transitionSpec = { scaleIn(bouncySpec()) togetherWith scaleOut() }
                    ) { editing ->
                        Icon(
                            painter = painterResource(if (editing) R.drawable.visibility else R.drawable.edit_filled),
                            contentDescription = null
                        )
                    }
                }
                IconButton(
                    onClick = { onAction(NoteDetailAction.OnTogglePin) },
                    shapes = IconButtonDefaults.shapes()
                ) {
                    AnimatedContent(
                        targetState = state.isPinned,
                        transitionSpec = { scaleIn(bouncySpec()) togetherWith scaleOut() }
                    ) { pinned ->
                        Icon(
                            painter = painterResource(if (pinned) R.drawable.pin_filled else R.drawable.unpin),
                            contentDescription = stringResource(if (pinned) R.string.unpin else R.string.pin),
                            tint = if (pinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.semantics { contentDescription = moreDescription }
                    ) {
                        AnimatedDrawable(
                            drawable = AnimatedDrawableFile.MORE_VERT,
                            atEnd = showMenu
                        )
                    }
                    DropdownMenuPopup(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuGroup(shapes = MenuDefaults.groupShapes()) {
                            menuOptions.fastForEachIndexed { index, option ->
                                val tint =
                                    if (option.isDestructive) MaterialTheme.colorScheme.error else LocalContentColor.current
                                DropdownMenuItem(
                                    onClick = {
                                        showMenu = false
                                        option.onClick()
                                    },
                                    shape = MenuDefaults.getItemShape(index, menuOptions.lastIndex),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(option.icon),
                                            contentDescription = null,
                                            tint = tint
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = stringResource(option.text),
                                            color = tint
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NoteDetailScreenPreview() {
    AzukiTheme {
        NoteDetailScreen(
            state = NoteDetailState(
                isLoading = false,
                updatedAt = "Nov 12, 2026, 10:00 AM",
                allTags = listOf(Tag(1, "ideas")),
                selectedTagIds = setOf(1)
            ),
            titleState = TextFieldState("Azuki"),
            contentState = TextFieldState("# Hello\n- [x] Build the app\n- [ ] Ship it\n\n**Bold** and _italic_."),
            onNavigateBack = {},
            onAction = {}
        )
    }
}
