@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.notes.presentation.trash

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.azuki.R
import com.sosauce.azuki.core.ui.AzukiTheme
import com.sosauce.azuki.core.ui.barsContentTransform
import com.sosauce.azuki.core.ui.components.SelectedBarSurface
import com.sosauce.azuki.core.ui.components.SelectionAction
import com.sosauce.azuki.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.azuki.core.ui.selfAlignHorizontally
import com.sosauce.azuki.notes.presentation.shared.NoteCard
import com.sosauce.azuki.notes.presentation.shared.NoteUi
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.nekobites.helpers.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TrashRoot(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is TrashEvent.ShowMessage -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    TrashScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction
    )
}

private enum class TrashDialog { DELETE_SELECTED, EMPTY_TRASH }

@Composable
fun TrashScreen(
    state: TrashState,
    onNavigateBack: () -> Unit,
    onAction: (TrashAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var dialog by rememberSaveable { mutableStateOf<TrashDialog?>(null) }

    BackHandler(enabled = state.isInSelectionMode) {
        onAction(TrashAction.OnClearSelection)
    }

    dialog?.let { current ->
        DeleteForeverDialog(
            text = if (current == TrashDialog.EMPTY_TRASH) {
                stringResource(R.string.empty_trash_confirm)
            } else {
                pluralStringResource(
                    R.plurals.delete_notes_confirm,
                    state.selectedNoteIds.size,
                    state.selectedNoteIds.size
                )
            },
            onConfirm = {
                onAction(
                    if (current == TrashDialog.EMPTY_TRASH) TrashAction.OnEmptyTrash else TrashAction.OnDeleteSelected
                )
                dialog = null
            },
            onDismissRequest = { dialog = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedContent(
                targetState = state.isInSelectionMode,
                transitionSpec = { barsContentTransform },
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier.fillMaxWidth()
            ) { inSelection ->
                if (inSelection) {
                    SelectedBarSurface(
                        modifier = Modifier
                            .selfAlignHorizontally()
                            .padding(bottom = 12.dp),
                        selectedCount = state.selectedNoteIds.size,
                        totalCount = state.notes.size,
                        onClearSelection = { onAction(TrashAction.OnClearSelection) },
                        onToggleSelectAll = { onAction(TrashAction.OnToggleSelectAll) },
                        actions = listOf(
                            SelectionAction(
                                icon = R.drawable.restore_from_trash,
                                label = R.string.restore,
                                onClick = { onAction(TrashAction.OnRestoreSelected) }
                            ),
                            SelectionAction(
                                icon = R.drawable.delete_forever,
                                label = R.string.delete_forever,
                                isDestructive = true,
                                onClick = { dialog = TrashDialog.DELETE_SELECTED }
                            )
                        )
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 15.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CuteNavigationButtonSurface(onNavigateUp = onNavigateBack)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            IconButton(
                                onClick = { dialog = TrashDialog.EMPTY_TRASH },
                                enabled = state.notes.isNotEmpty(),
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete_forever),
                                    contentDescription = stringResource(R.string.empty_trash),
                                    tint = if (state.notes.isNotEmpty()) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LoadingBox(isLoading = state.isLoading) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = statusBarTop + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "header") {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                        Text(
                            text = stringResource(R.string.trash),
                            style = MaterialTheme.typography.displaySmallEmphasized
                        )
                        Text(
                            text = stringResource(R.string.trash_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (state.notes.isEmpty()) {
                    item(key = "empty") {
                        NoXFound(
                            headlineText = R.string.trash_empty,
                            bodyText = R.string.trash_empty_desc,
                            icon = R.drawable.delete
                        )
                    }
                }
                items(items = state.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        isSelected = note.id in state.selectedNoteIds,
                        isListLayout = true,
                        onClick = { onAction(TrashAction.OnNoteClick(note.id)) },
                        onLongClick = { onAction(TrashAction.OnNoteClick(note.id)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteForeverDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                painter = painterResource(R.drawable.delete_forever),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.delete_forever)) },
        text = { Text(text) },
        dismissButton = {
            TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, shapes = ButtonDefaults.shapes()) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Preview
@Composable
private fun TrashScreenPreview() {
    AzukiTheme {
        TrashScreen(
            state = TrashState(
                isLoading = false,
                notes = listOf(
                    NoteUi(
                        1,
                        "Old idea",
                        "Not needed anymore",
                        null,
                        false,
                        emptyList(),
                        "3 days ago"
                    )
                )
            ),
            onNavigateBack = {},
            onAction = {}
        )
    }
}
