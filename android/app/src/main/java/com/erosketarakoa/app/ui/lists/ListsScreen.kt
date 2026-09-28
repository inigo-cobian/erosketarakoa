package com.erosketarakoa.app.ui.lists

import android.graphics.drawable.shapes.RoundRectShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erosketarakoa.app.data.ListColor
import com.erosketarakoa.app.data.local.ListEntity
import com.erosketarakoa.app.ui.settings.ThemeViewModel
import com.erosketarakoa.app.ui.theme.RoundedHexagonShape
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

const val LISTS_SCREEN_TAG = "lists_screen"
const val ADD_LIST_FAB_TAG = "add_list_fab"
const val LIST_NAME_FIELD_TAG = "list_name_field"
const val ABOUT_ACTION_TAG = "about_action"
const val SETTINGS_ACTION_TAG = "settings_action"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    onOpenList: (String) -> Unit,
    onOpenAbout: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ListsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title by themeViewModel.title.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ListEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        modifier = Modifier
                            .clickable(onClick = onOpenSettings)
                            .testTag(SETTINGS_ACTION_TAG),
                    )
                },
                actions = {
                    IconButton(onClick = onOpenAbout, modifier = Modifier.testTag(ABOUT_ACTION_TAG)) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                },
            )
        },
        floatingActionButton = {
            val rotation by animateFloatAsState(if (showCreate) 180f else 0f, label = "addRotation")
            FloatingActionButton(
                onClick = { showCreate = true },
                modifier = Modifier
                    .testTag(ADD_LIST_FAB_TAG)
                    .rotate(rotation),
                shape = RoundedHexagonShape(cornerRadius = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add list")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(LISTS_SCREEN_TAG),
        ) {
            if (!state.isLoading && state.lists.isEmpty()) {
                EmptyState()
            } else {
                val haptic = LocalHapticFeedback.current
                val lazyListState = rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    viewModel.moveList(from.index, to.index)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(state.lists, key = { it.id }) { list ->
                        ReorderableItem(reorderState, key = list.id) { isDragging ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd) {
                                        viewModel.deleteList(list.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Deleted ${list.name}",
                                                actionLabel = "Undo",
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreList(list)
                                            }
                                        }
                                    }
                                    value == SwipeToDismissBoxValue.StartToEnd
                                },
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromEndToStart = false,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                },
                            ) {
                                Surface(tonalElevation = if (isDragging) 4.dp else 0.dp) {
                                    ListRow(
                                        list = list,
                                        onClick = { onOpenList(list.id) },
                                        onEditColor = { editing = list },
                                        dragModifier = Modifier.longPressDraggableHandle(
                                            onDragStopped = { viewModel.commitReorder() },
                                        ),
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var pendingName by remember { mutableStateOf("") }
        TextInputDialog(
            title = "New list",
            initial = "",
            confirmLabel = "Create",
            initialColor = ListColor.DEFAULT,
            onConfirm = { pendingName = it },
            onConfirmColor = {
                viewModel.createList(pendingName, it)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }

    editing?.let { list ->
        var pendingName by remember(list.id) { mutableStateOf(list.name) }
        TextInputDialog(
            title = "Edit list",
            initial = list.name,
            confirmLabel = "Save",
            initialColor = ListColor.from(list.color),
            onConfirm = { pendingName = it },
            onConfirmColor = {
                viewModel.renameList(list.id, pendingName, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/**
 * Header text color for a list. WHITE and BLACK keep the system theme's font color (so the name
 * stays readable on any background); the swatch still shows the real choice. Other colors tint
 * the text directly.
 */
@Composable
private fun headerColor(color: ListColor): Color =
    if (color.usesThemeText) MaterialTheme.colorScheme.onSurface
    else color.swatch

@Composable
private fun ListRow(
    list: ListEntity,
    onClick: () -> Unit,
    onEditColor: () -> Unit,
    dragModifier: Modifier = Modifier,
) {
    val listColor = ListColor.from(list.color)
    ListItem(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(listColor.swatch)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable { onEditColor() },
            )
        },
        headlineContent = {
            Text(
                list.name,
                color = headerColor(listColor),
            )
        },
        modifier = dragModifier.clickable { onClick() },
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "No lists yet.\nTap + to create your first shopping list.",
            textAlign = TextAlign.Center,
        )
    }
}
