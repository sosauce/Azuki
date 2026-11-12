@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sosauce.azuki.core.ui.components.searchbars

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sosauce.azuki.R

/**
 * Floating bottom search bar (90% width, nav-bar + IME padded) with the primary FAB above it.
 *
 * @param sortingMenu shown in the text field while the query is empty, swapped for a clear button otherwise
 * @param actions always-visible trailing actions (overflow menu…)
 * @param moreContent optional second row inside the bar (filter chips…)
 */
@Composable
fun CuteSearchbar(
    textFieldState: TextFieldState,
    modifier: Modifier = Modifier,
    fab: (@Composable () -> Unit)? = null,
    sortingMenu: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    moreContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth(0.9f)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Spacer(Modifier.weight(1f))
            fab?.invoke()
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                moreContent?.invoke(this)
                TextField(
                    state = textFieldState,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    shape = FloatingToolbarDefaults.ContainerShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLargeEmphasized,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_notes),
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedContent(targetState = textFieldState.text.isNotEmpty()) { hasText ->
                                if (hasText) {
                                    IconButton(
                                        onClick = textFieldState::clearText,
                                        shapes = IconButtonDefaults.shapes()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.cancel_filled),
                                            contentDescription = stringResource(R.string.clear_search)
                                        )
                                    }
                                } else {
                                    sortingMenu?.invoke()
                                }
                            }
                            actions?.invoke(this)
                        }
                    }
                )
            }
        }
    }
}
