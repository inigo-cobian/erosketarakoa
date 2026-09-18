package com.erosketarakoa.app.ui.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

const val ICON_PICKER_FILTER_TAG = "icon_picker_filter"
const val ICON_PICKER_GRID_TAG = "icon_picker_grid"

/**
 * A searchable grid of bundled OpenMoji icons. [selected] is always a valid hexcode so the
 * "every item has an icon" rule holds; tapping a cell calls [onSelect].
 */
@Composable
fun IconPickerGrid(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val catalog = remember { IconCatalog.fromAssets(context) }
    var query by remember { mutableStateOf("") }
    val entries = remember(query) { catalog.filter(query) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search icons") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ICON_PICKER_FILTER_TAG),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag(ICON_PICKER_GRID_TAG),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(entries, key = { it.hexcode }) { entry ->
                val isSelected = entry.hexcode == selected
                val cellModifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .let {
                        if (isSelected) it.background(MaterialTheme.colorScheme.primaryContainer) else it
                    }
                    .clickable { onSelect(entry.hexcode) }
                    .padding(6.dp)
                    .testTag("icon_cell_${entry.hexcode}")
                OpenMojiIcon(
                    hexcode = entry.hexcode,
                    modifier = cellModifier,
                    contentDescription = entry.keywords.ifBlank { entry.hexcode },
                )
            }
        }
    }
}
