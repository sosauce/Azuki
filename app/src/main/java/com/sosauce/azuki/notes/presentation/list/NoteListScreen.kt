@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)

package com.sosauce.azuki.notes.presentation.list

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.azuki.R
import com.sosauce.azuki.core.preferences.NotesLayout
import com.sosauce.azuki.core.ui.AzukiTheme
import com.sosauce.azuki.core.ui.barsContentTransform
import com.sosauce.azuki.core.ui.components.SelectedBarSurface
import com.sosauce.azuki.core.ui.components.SelectionAction
import com.sosauce.azuki.core.ui.components.buttons.CuteToggleChip
import com.sosauce.azuki.core.ui.components.searchbars.CuteSearchbar
import com.sosauce.azuki.core.ui.selfAlignHorizontally
import com.sosauce.azuki.notes.domain.Tag
import com.sosauce.azuki.notes.presentation.list.components.NotesMoreMenu
import com.sosauce.azuki.notes.presentation.list.components.NotesSortingMenu
import com.sosauce.azuki.notes.presentation.shared.NoteCard
import com.sosauce.azuki.notes.presentation.shared.NoteUi
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.nekobites.helpers.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun NoteListRoot(
    onNavigateToNote: (noteId: Long) -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: NoteListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is NoteListEvent.NavigateToNote -> onNavigateToNote(event.noteId)
            NoteListEvent.NavigateToTrash -> onNavigateToTrash()
            NoteListEvent.NavigateToSettings -> onNavigateToSettings()
            is NoteListEvent.ShareText -> context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                    },
                    null
                )
            )

            is NoteListEvent.NotesTrashed -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = event.message.asString(context),
                    actionLabel = resources.getString(R.string.undo),
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onAction(NoteListAction.OnUndoTrash(event.noteIds))
                }
            }
        }
    }

    NoteListScreen(
        state = state,
        searchState = viewModel.searchState,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction
    )
}

@Composable
fun NoteListScreen(
    state: NoteListState,
    searchState: TextFieldState,
    onAction: (NoteListAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    BackHandler(enabled = state.isInSelectionMode) {
        onAction(NoteListAction.OnClearSelection)
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
                    val allPinned = state.visibleNotes
                        .filter { it.id in state.selectedNoteIds }
                        .all { it.isPinned }
                    SelectedBarSurface(
                        modifier = Modifier
                            .selfAlignHorizontally()
                            .padding(bottom = 12.dp),
                        selectedCount = state.selectedNoteIds.size,
                        totalCount = state.visibleNotes.size,
                        onClearSelection = { onAction(NoteListAction.OnClearSelection) },
                        onToggleSelectAll = { onAction(NoteListAction.OnToggleSelectAll) },
                        actions = listOf(
                            SelectionAction(
                                icon = if (allPinned) R.drawable.unpin else R.drawable.pin_filled,
                                label = if (allPinned) R.string.unpin else R.string.pin,
                                onClick = { onAction(NoteListAction.OnPinSelectedClick) }
                            ),
                            SelectionAction(
                                icon = R.drawable.share,
                                label = R.string.share,
                                onClick = { onAction(NoteListAction.OnShareSelectedClick) }
                            ),
                            SelectionAction(
                                icon = R.drawable.delete_filled,
                                label = R.string.move_to_trash,
                                isDestructive = true,
                                onClick = { onAction(NoteListAction.OnTrashSelectedClick) }
                            )
                        )
                    )
                } else {
                    CuteSearchbar(
                        textFieldState = searchState,
                        modifier = Modifier
                            .selfAlignHorizontally()
                            .padding(bottom = 12.dp),
                        fab = {
                            AnimatedFab(
                                onClick = { onAction(NoteListAction.OnAddNoteClick) },
                                icon = R.drawable.add
                            )
                        },
                        sortingMenu = {
                            NotesSortingMenu(
                                sort = state.sort,
                                ascending = state.sortAscending,
                                onSortChange = { onAction(NoteListAction.OnSortChange(it)) },
                                onDirectionChange = {
                                    onAction(
                                        NoteListAction.OnSortDirectionChange(
                                            it
                                        )
                                    )
                                }
                            )
                        },
                        actions = {
                            NotesMoreMenu(
                                layout = state.layout,
                                onToggleLayout = { onAction(NoteListAction.OnToggleLayoutClick) },
                                onOpenTrash = { onAction(NoteListAction.OnTrashClick) },
                                onOpenSettings = { onAction(NoteListAction.OnSettingsClick) }
                            )
                        },
                        moreContent = if (state.tags.isNotEmpty()) {
                            {
                                TagFilterRow(
                                    tags = state.tags,
                                    selectedTagId = state.selectedTagId,
                                    onSelect = { onAction(NoteListAction.OnTagFilterClick(it)) }
                                )
                            }
                        } else null
                    )
                }
            }
        }
    ) { paddingValues ->
        LoadingBox(isLoading = state.isLoading) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val fullLine = StaggeredGridItemSpan.FullLine

            // Lookahead lets each card animate its bounds when the column count changes,
            // since the grid hands items a fixed width that animateContentSize can't animate.
            LookaheadScope {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (state.layout == NotesLayout.GRID) 2 else 1),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = statusBarTop + 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.visibleNotes.isEmpty()) {
                        item(key = "empty", span = fullLine) {
                            if (state.isFiltering) {
                                NoXFound(
                                    headlineText = R.string.no_results,
                                    bodyText = R.string.no_results_desc,
                                    icon = R.drawable.search
                                )
                            } else {
                                NoXFound(
                                    headlineText = R.string.no_notes,
                                    bodyText = R.string.no_notes_desc,
                                    icon = R.drawable.note
                                )
                            }
                        }
                    }

                    if (state.pinnedNotes.isNotEmpty()) {
                        item(key = "pinned_header", span = fullLine) {
                            SectionHeader(
                                text = stringResource(R.string.pinned) + " (${state.pinnedNotes.count()})",
                                icon = R.drawable.pin_filled
                            )
                        }
                        items(items = state.pinnedNotes, key = { it.id }) { note ->
                            NoteGridItem(
                                note = note,
                                state = state,
                                lookaheadScope = this@LookaheadScope,
                                onAction = onAction
                            )
                        }
                        if (state.otherNotes.isNotEmpty()) {
                            item(key = "others_header", span = fullLine) {
                                SectionHeader(
                                    text = stringResource(R.string.others) + " (${state.otherNotes.count()})",
                                    icon = R.drawable.note
                                )
                            }
                        }
                    }

                    items(items = state.otherNotes, key = { it.id }) { note ->
                        NoteGridItem(
                            note = note,
                            state = state,
                            lookaheadScope = this@LookaheadScope,
                            onAction = onAction
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyStaggeredGridItemScope.NoteGridItem(
    note: NoteUi,
    state: NoteListState,
    lookaheadScope: LookaheadScope,
    onAction: (NoteListAction) -> Unit
) {
    NoteCard(
        note = note,
        isSelected = note.id in state.selectedNoteIds,
        isListLayout = state.layout == NotesLayout.LIST,
        onClick = { onAction(NoteListAction.OnNoteClick(note.id)) },
        onLongClick = { onAction(NoteListAction.OnNoteLongClick(note.id)) },
        modifier = Modifier
            .animateItem(placementSpec = NotePlacementSpec)
            .animateBounds(
                lookaheadScope = lookaheadScope,
                boundsTransform = NoteBoundsTransform
            )
    )
}

private val NotePlacementSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

private val NoteBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = Rect.VisibilityThreshold
    )
}

@Composable
private fun SectionHeader(text: String, icon: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TagFilterRow(
    tags: List<Tag>,
    selectedTagId: Long?,
    onSelect: (Long?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item(key = "all") {
            CuteToggleChip(
                selected = selectedTagId == null,
                onClick = { onSelect(null) },
                label = stringResource(R.string.all_notes),
                modifier = Modifier.animateItem()
            )
        }
        items(items = tags, key = { it.id }) { tag ->
            CuteToggleChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelect(if (selectedTagId == tag.id) null else tag.id) },
                label = tag.name,
                icon = R.drawable.sell,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Preview
@Composable
private fun NoteListScreenPreview() {
    AzukiTheme {
        NoteListScreen(
            state = NoteListState(
                isLoading = false,
                totalNoteCount = 2,
                pinnedNotes = listOf(
                    NoteUi(
                        1,
                        "Groceries",
                        "Milk, eggs, red beans",
                        0xFFB33B4C.toInt(),
                        true,
                        listOf(Tag(1, "home")),
                        "2 min. ago"
                    )
                ),
                otherNotes = listOf(
                    NoteUi(
                        2,
                        "Ideas",
                        "A note app called Azuki",
                        null,
                        false,
                        emptyList(),
                        "Yesterday"
                    )
                ),
                tags = listOf(Tag(1, "home"))
            ),
            searchState = TextFieldState(),
            onAction = {}
        )
    }
}
