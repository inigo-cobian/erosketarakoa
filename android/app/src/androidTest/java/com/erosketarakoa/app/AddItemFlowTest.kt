package com.erosketarakoa.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.ui.detail.ITEM_NAME_FIELD_TAG
import com.erosketarakoa.app.ui.detail.ITEM_QTY_FIELD_TAG
import com.erosketarakoa.app.ui.detail.ITEM_SAVE_BUTTON_TAG
import com.erosketarakoa.app.ui.detail.ItemEditorDialog
import com.erosketarakoa.app.ui.detail.ItemFormValues
import com.erosketarakoa.app.ui.theme.ErosketarakoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AddItemFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addItem_enterNameAndQuantity_confirmsWithValues() {
        var result: ItemFormValues? = null
        composeRule.setContent {
            ErosketarakoTheme {
                ItemEditorDialog(
                    title = "Add item",
                    existing = null,
                    onConfirm = { result = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag(ITEM_NAME_FIELD_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(ITEM_NAME_FIELD_TAG).performTextInput("Milk")
        composeRule.onNodeWithTag(ITEM_QTY_FIELD_TAG).performTextReplacement("3")
        composeRule.onNodeWithTag(ITEM_SAVE_BUTTON_TAG).performClick()

        assertEquals("Milk", result?.name)
        assertEquals(3, result?.quantity)
    }

    @Test
    fun editItem_prefillsExistingValues() {
        var result: ItemFormValues? = null
        val existing = ItemEntity(
            id = "1",
            listId = "l1",
            name = "Eggs",
            quantity = 6,
            category = "Bakery",
            notes = "Free range",
        )
        composeRule.setContent {
            ErosketarakoTheme {
                ItemEditorDialog(
                    title = "Edit item",
                    existing = existing,
                    onConfirm = { result = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag(ITEM_SAVE_BUTTON_TAG).performClick()
        assertEquals("Eggs", result?.name)
        assertEquals(6, result?.quantity)
        assertEquals("Bakery", result?.category)
    }
}
