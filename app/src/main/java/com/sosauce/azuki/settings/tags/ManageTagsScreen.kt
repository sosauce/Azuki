@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.settings.tags

import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sosauce.azuki.R
import com.sosauce.azuki.core.ui.AzukiTheme
import com.sosauce.azuki.core.ui.components.buttons.CuteNavigationButtonSurface
import com.sosauce.azuki.core.ui.components.items.CuteListItem
import com.sosauce.azuki.core.ui.components.items.CuteListItemDefaults
import com.sosauce.azuki.notes.domain.Tag
import com.sosauce.nekobites.animations.AnimatedFab
import com.sosauce.nekobites.components.LoadingBox
import com.sosauce.nekobites.components.NoXFound
import com.sosauce.nekobites.components.settings.SettingsIconBadge
import com.sosauce.nekobites.helpers.ObserveAsEvents
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ManageTagsRoot(
    onNavigateBack: () -> Unit,
    viewModel: ManageTagsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ManageTagsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    ManageTagsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onAction = viewModel::onAction
    )
}

private sealed interface TagDialog {
    data object Create : TagDialog
    data class Rename(val tag: Tag) : TagDialog
    data class Delete(val tag: Tag) : TagDialog
}

@Composable
fun ManageTagsScreen(
    state: ManageTagsState,
    onNavigateBack: () -> Unit,
    onAction: (ManageTagsAction) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var dialog by remember { mutableStateOf<TagDialog?>(null) }

    when (val current = dialog) {
        TagDialog.Create -> TagNameDialog(
            title = stringResource(R.string.create_tag),
            initialName = "",
            confirmLabel = stringResource(R.string.create),
            onConfirm = { onAction(ManageTagsAction.OnCreateTag(it)) },
            onDismissRequest = { dialog = null }
        )

        is TagDialog.Rename -> TagNameDialog(
            title = stringResource(R.string.rename_tag),
            initialName = current.tag.name,
            confirmLabel = stringResource(R.string.rename),
            onConfirm = { onAction(ManageTagsAction.OnRenameTag(current.tag.id, it)) },
            onDismissRequest = { dialog = null }
        )

        is TagDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.delete_tag)) },
            text = { Text(stringResource(R.string.delete_tag_confirm, current.tag.name)) },
            dismissButton = {
                TextButton(onClick = { dialog = null }, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(ManageTagsAction.OnDeleteTag(current.tag.id))
                        dialog = null
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )

        null -> Unit
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
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
                AnimatedFab(
                    onClick = { dialog = TagDialog.Create },
                    icon = R.drawable.add
                )
            }
        }
    ) { paddingValues ->
        LoadingBox(isLoading = state.isLoading) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            LazyColumn(
                contentPadding = PaddingValues(
                    start = 11.dp,
                    end = 11.dp,
                    top = statusBarTop + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "header") {
                    Column(
                        modifier = Modifier.padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 12.dp,
                            bottom = 16.dp
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.manage_tags),
                            style = MaterialTheme.typography.displaySmallEmphasized
                        )
                        Text(
                            text = stringResource(R.string.manage_tags_desc),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (state.tags.isEmpty()) {
                    item(key = "empty") {
                        NoXFound(
                            headlineText = R.string.no_tags,
                            bodyText = R.string.no_tags_desc,
                            icon = R.drawable.sell
                        )
                    }
                }
                itemsIndexed(items = state.tags, key = { _, tag -> tag.id }) { index, tag ->
                    CuteListItem(
                        modifier = Modifier.animateItem(),
                        onClick = { dialog = TagDialog.Rename(tag) },
                        shape = CuteListItemDefaults.getItemShape(index, state.tags.size),
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
                        leadingContent = {
                            SettingsIconBadge(
                                icon = R.drawable.sell,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        trailingContent = {
                            IconButton(
                                onClick = { dialog = TagDialog.Delete(tag) },
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = stringResource(R.string.delete_tag),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val nameState = rememberTextFieldState(initialText = initialName)
    val confirm = {
        val name = nameState.text.toString().trim()
        if (name.isNotEmpty()) {
            onConfirm(name)
            onDismissRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(painter = painterResource(R.drawable.sell), contentDescription = null) },
        title = { Text(title) },
        text = {
            TextField(
                state = nameState,
                modifier = Modifier.fillMaxWidth(),
                lineLimits = TextFieldLineLimits.SingleLine,
                shape = FloatingToolbarDefaults.ContainerShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                placeholder = { Text(stringResource(R.string.tag_name), maxLines = 1) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = { confirm() }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = confirm,
                enabled = nameState.text.isNotBlank(),
                shapes = ButtonDefaults.shapes()
            ) {
                Text(confirmLabel)
            }
        }
    )
}

@Preview
@Composable
private fun ManageTagsScreenPreview() {
    AzukiTheme {
        ManageTagsScreen(
            state = ManageTagsState(
                isLoading = false,
                tags = listOf(Tag(1, "home"), Tag(2, "ideas"), Tag(3, "work"))
            ),
            onNavigateBack = {},
            onAction = {}
        )
    }
}
