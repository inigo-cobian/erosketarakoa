package com.erosketarakoa.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.erosketarakoa.app.MainDispatcherRule
import com.erosketarakoa.app.data.ShoppingRepository
import com.erosketarakoa.app.fakes.FakeClock
import com.erosketarakoa.app.fakes.FakeItemDao
import com.erosketarakoa.app.fakes.FakeListDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val listDao = FakeListDao()
    private val itemDao = FakeItemDao()
    private val repository = ShoppingRepository(listDao, itemDao, FakeClock())

    private suspend fun newViewModel(): ListDetailViewModel {
        val listId = repository.createList("Groceries")
        return ListDetailViewModel(repository, SavedStateHandle(mapOf("listId" to listId)))
    }

    @Test
    fun addItem_appearsInState() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            awaitItem() // initial / list loaded
            vm.addItem("Milk", 2, "Dairy", "Semi-skimmed")
            val state = awaitItem()
            assertEquals(1, state.items.size)
            val item = state.items.first()
            assertEquals("Milk", item.name)
            assertEquals(2, item.quantity)
            assertEquals("Dairy", item.category)
            assertEquals("Semi-skimmed", item.notes)
            assertFalse(item.bought)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleBought_flipsFlag() = runTest {
        val vm = newViewModel()
        val itemId = repository.addItem(vm.listId, "Milk")
        val item = itemDao.getById(itemId)!!
        vm.toggleBought(item)
        vm.uiState.test {
            // items are sorted bought-last; find our item
            val state = awaitItem()
            val reloaded = state.items.firstOrNull { it.id == itemId }
            assertTrue(reloaded?.bought == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteItem_removesFromActive() = runTest {
        val vm = newViewModel()
        val itemId = repository.addItem(vm.listId, "Eggs")
        vm.deleteItem(itemId)
        vm.uiState.test {
            assertTrue(awaitItem().items.none { it.id == itemId })
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(itemDao.getById(itemId)!!.isDeleted)
    }

    @Test
    fun updateItem_changesFields() = runTest {
        val vm = newViewModel()
        val itemId = repository.addItem(vm.listId, "Milk")
        vm.updateItem(itemId, "Whole Milk", 3, "Dairy", null)
        val updated = itemDao.getById(itemId)!!
        assertEquals("Whole Milk", updated.name)
        assertEquals(3, updated.quantity)
        assertEquals("Dairy", updated.category)
    }
}
