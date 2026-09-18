package com.erosketarakoa.app.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.ui.icon.OpenMojiIcon
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

const val DETAIL_SCREEN_TAG = "detail_screen"
const val ADD_ITEM_FAB_TAG = "add_item_fab"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    onBack: () -> Unit,
    viewModel: ListDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ItemEntity?>(null) }

    Scaffold(
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
            FloatingActionButton(
                onClick = { showAdd = true },
                modifier = Modifier.testTag(ADD_ITEM_FAB_TAG),
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
                            Surface(tonalElevation = if (isDragging) 4.dp else 0.dp) {
                                ItemRow(
                                    item = item,
                                    onToggle = { viewModel.toggleBought(item) },
                                    onEdit = { editing = item },
                                    onDelete = { viewModel.deleteItem(item.id) },
                                    dragModifier = Modifier.longPressDraggableHandle(
                                        onDragStopped = { viewModel.commitReorder() },
                                    ),
                                )
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
                viewModel.addItem(form.name, form.quantity, form.category, form.notes, form.icon)
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
                viewModel.updateItem(item.id, form.name, form.quantity, form.category, form.notes, form.icon)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ItemRow(
    item: ItemEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier = Modifier,
) {
    val subtitle = buildString {
        if (item.quantity > 1) append("x${item.quantity}")
        item.category?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
        item.notes?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
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
        supportingContent = { if (subtitle.isNotBlank()) Text(subtitle) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit item")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete item")
                }
            }
        },
        modifier = dragModifier.clickable { onToggle() },
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
