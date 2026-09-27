package com.erosketarakoa.app.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontStyle
import com.erosketarakoa.app.data.ItemDetail
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.ui.icon.OpenMojiIcon
import com.erosketarakoa.app.ui.settings.ThemeViewModel
import com.erosketarakoa.app.ui.theme.RoundedHexagonShape
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

const val DETAIL_SCREEN_TAG = "detail_screen"
const val ADD_ITEM_FAB_TAG = "add_item_fab"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    onBack: () -> Unit,
    viewModel: ListDetailViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visibleDetails by themeViewModel.visibleDetails.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ItemEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.list?.name ?: "List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            val rotation by animateFloatAsState(if (showAdd) 180f else 0f, label = "addRotation")
            FloatingActionButton(
                onClick = { showAdd = true },
                modifier = Modifier
                    .testTag(ADD_ITEM_FAB_TAG)
                    .rotate(rotation),
                shape = RoundedHexagonShape(cornerRadius = 8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(DETAIL_SCREEN_TAG),
        ) {
            if (!state.isLoading && state.items.isEmpty()) {
                EmptyItems()
            } else {
                val haptic = LocalHapticFeedback.current
                val lazyListState = rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    viewModel.moveItem(from.index, to.index)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(state.items, key = { it.id }) { item ->
                        ReorderableItem(reorderState, key = item.id) { isDragging ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd) {
                                        viewModel.deleteItem(item.id)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Deleted ${item.name}",
                                                actionLabel = "Undo",
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.restoreItem(item)
                                            }
                                        }
                                    }
                                    // Let the item leave the list; the DB flow drives removal.
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
                                    ItemRow(
                                        item = item,
                                        visibleDetails = visibleDetails,
                                        onToggle = { viewModel.toggleBought(item) },
                                        onEdit = { editing = item },
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

    if (showAdd) {
        ItemEditorDialog(
            title = "Add item",
            existing = null,
            onConfirm = { form ->
                viewModel.addItem(form.name, form.quantity, form.unit, form.supermarkets, form.category, form.notes, form.icon)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    editing?.let { item ->
        ItemEditorDialog(
            title = "Edit item",
            existing = item,
            onConfirm = { form ->
                viewModel.updateItem(item.id, form.name, form.quantity, form.unit, form.supermarkets, form.category, form.notes, form.icon)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ItemRow(
    item: ItemEntity,
    visibleDetails: Set<ItemDetail>,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    dragModifier: Modifier = Modifier,
) {
    val subtitle = buildString {
        if (ItemDetail.QUANTITY in visibleDetails) {
            val qtyText = when {
                item.unit != null -> "${item.quantity} ${item.unit}"
                item.quantity > 1 -> "x${item.quantity}"
                else -> ""
            }
            if (qtyText.isNotEmpty()) append(qtyText)
        }
        if (ItemDetail.SUPERMARKETS in visibleDetails) {
            item.supermarketList.takeIf { it.isNotEmpty() }?.let {
                if (isNotEmpty()) append(" · ")
                append(it.joinToString(", "))
            }
        }
        if (ItemDetail.CATEGORY in visibleDetails) {
            item.category?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
        }
        if (ItemDetail.NOTES in visibleDetails) {
            item.notes?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
        }
    }
    ListItem(
        leadingContent = {
            OpenMojiIcon(hexcode = item.icon)
        },
        headlineContent = {
            Text(
                item.name,
                textDecoration = if (item.bought) TextDecoration.LineThrough else TextDecoration.None,
            )
        },
        supportingContent = {
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.MoreVert, contentDescription = "Edit item")
            }
        },
        modifier = dragModifier
            .clickable { onToggle() }
            .alpha(if (item.bought) 0.5f else 1f),
    )
}

@Composable
private fun EmptyItems() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "No items yet.\nTap + to add something to buy.",
            textAlign = TextAlign.Center,
        )
    }
}
