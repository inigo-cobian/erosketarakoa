package com.erosketarakoa.app.ui.lists

import android.graphics.drawable.shapes.RoundRectShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erosketarakoa.app.data.local.ListEntity
import com.erosketarakoa.app.ui.theme.RoundedHexagonShape

const val LISTS_SCREEN_TAG = "lists_screen"
const val ADD_LIST_FAB_TAG = "add_list_fab"
const val LIST_NAME_FIELD_TAG = "list_name_field"
const val ABOUT_ACTION_TAG = "about_action"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    onOpenList: (String) -> Unit,
    onOpenAbout: () -> Unit = {},
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ListEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping lists") },
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.lists, key = { it.id }) { list ->
                        ListRow(
                            list = list,
                            onClick = { onOpenList(list.id) },
                            onRename = { editing = list },
                            onDelete = { viewModel.deleteList(list.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = "New list",
            initial = "",
            confirmLabel = "Create",
            onConfirm = {
                viewModel.createList(it)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }

    editing?.let { list ->
        TextInputDialog(
            title = "Rename list",
            initial = list.name,
            confirmLabel = "Save",
            onConfirm = {
                viewModel.renameList(list.id, it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ListRow(
    list: ListEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(list.name) },
        trailingContent = {
            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    modifier = Modifier.clickable { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menuOpen = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        },
        modifier = Modifier.clickable { onClick() },
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
