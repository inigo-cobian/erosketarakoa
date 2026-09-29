package com.erosketarakoa.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.data.local.SUPERMARKET_OPTIONS
import com.erosketarakoa.app.ui.icon.DEFAULT_ICON
import com.erosketarakoa.app.ui.icon.IconPickerGrid

const val ITEM_NAME_FIELD_TAG = "item_name_field"
const val ITEM_QTY_FIELD_TAG = "item_qty_field"
const val ITEM_SAVE_BUTTON_TAG = "item_save_button"

const val ITEM_TARGET_FIELD_TAG = "item_target_field"
const val ITEM_BARCODE_FIELD_TAG = "item_barcode_field"

data class ItemFormValues(
    val name: String,
    val quantity: Int,
    val unit: String?,
    val supermarkets: List<String>,
    val category: String?,
    val notes: String?,
    val icon: String,
    /** Target price in integer cents; null = no target. */
    val targetPriceCents: Long?,
    val barcode: String?,
    /**
     * Desired store links: store name -> external product reference (may be blank).
     * A store present here means "link to it"; absent means "no link / remove existing".
     */
    val links: Map<String, String>,
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ItemEditorDialog(
    title: String,
    existing: ItemEntity?,
    onConfirm: (ItemFormValues) -> Unit,
    onDismiss: () -> Unit,
    /** Existing store links for this item, store name -> external product reference. */
    existingLinks: Map<String, String> = emptyMap(),
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var quantity by remember { mutableStateOf((existing?.quantity ?: 1).toString()) }
    var unit by remember { mutableStateOf(existing?.unit ?: "") }
    val supermarkets = remember {
        mutableStateListOf<String>().apply { existing?.supermarketList?.let { addAll(it) } }
    }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var icon by remember { mutableStateOf(existing?.icon ?: DEFAULT_ICON) }
    var target by remember { mutableStateOf(com.erosketarakoa.app.data.Money.centsToEuros(existing?.targetPriceCents)) }
    var barcode by remember { mutableStateOf(existing?.barcode ?: "") }
    // Store -> external product reference. Selecting a store below adds it here.
    val linkRefs = remember {
        mutableStateMapOf<String, String>().apply { putAll(existingLinks) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ITEM_NAME_FIELD_TAG),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { new -> quantity = new.filter { it.isDigit() }.take(4) },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ITEM_QTY_FIELD_TAG),
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (e.g. kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text("Supermarkets")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUPERMARKET_OPTIONS.forEach { market ->
                        val selected = market in supermarkets
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) supermarkets.remove(market) else supermarkets.add(market)
                            },
                            label = { Text(market) },
                        )
                    }
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { new -> target = new.filter { it.isDigit() || it == '.' || it == ',' }.take(10) },
                        label = { Text("Target price (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ITEM_TARGET_FIELD_TAG),
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it.filter { c -> c.isLetterOrDigit() } },
                        label = { Text("Barcode / EAN") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(ITEM_BARCODE_FIELD_TAG),
                    )
                }
                Text("Store links")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SUPERMARKET_OPTIONS.forEach { store ->
                        val linked = store in linkRefs
                        FilterChip(
                            selected = linked,
                            onClick = {
                                if (linked) linkRefs.remove(store) else linkRefs[store] = ""
                            },
                            label = { Text(store) },
                        )
                    }
                }
                // A reference field per linked store.
                linkRefs.keys.sorted().forEach { store ->
                    OutlinedTextField(
                        value = linkRefs[store] ?: "",
                        onValueChange = { linkRefs[store] = it },
                        label = { Text("$store product reference") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_link_ref_$store"),
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                )
                Text("Icon")
                IconPickerGrid(
                    selected = icon,
                    onSelect = { icon = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(ITEM_SAVE_BUTTON_TAG),
                onClick = {
                    onConfirm(
                        ItemFormValues(
                            name = name,
                            quantity = quantity.toIntOrNull() ?: 1,
                            unit = unit.ifBlank { null },
                            supermarkets = supermarkets.toList(),
                            category = category.ifBlank { null },
                            notes = notes.ifBlank { null },
                            icon = icon,
                            targetPriceCents = com.erosketarakoa.app.data.Money.parseEurosToCents(target),
                            barcode = barcode.ifBlank { null },
                            links = linkRefs.toMap(),
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
