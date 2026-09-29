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
    private val linkDao = com.erosketarakoa.app.fakes.FakeProductLinkDao()
    private val priceDao = com.erosketarakoa.app.fakes.FakePriceDao(linkDao)
    private val remote = com.erosketarakoa.app.fakes.fakeRemoteDataSource()
    private val repository = ShoppingRepository(listDao, itemDao, linkDao, priceDao, remote, FakeClock())

    private suspend fun newViewModel(): ListDetailViewModel {
        val listId = repository.createList("Groceries")
        return ListDetailViewModel(repository, SavedStateHandle(mapOf("listId" to listId)))
    }

    @Test
    fun addItem_appearsInState() = runTest {
        val vm = newViewModel()
        vm.uiState.test {
            awaitItem() // initial / list loaded
            vm.addItem("Milk", 2, null, emptyList(), "Dairy", "Semi-skimmed", "1F345")
            val state = awaitItem()
            assertEquals(1, state.items.size)
            val item = state.items.first()
            assertEquals("Milk", item.name)
            assertEquals(2, item.quantity)
            assertEquals("Dairy", item.category)
            assertEquals("Semi-skimmed", item.notes)
            assertEquals("1F345", item.icon)
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
        vm.updateItem(itemId, "Whole Milk", 3, null, emptyList(), "Dairy", null, "1F404")
        val updated = itemDao.getById(itemId)!!
        assertEquals("Whole Milk", updated.name)
        assertEquals(3, updated.quantity)
        assertEquals("Dairy", updated.category)
        assertEquals("1F404", updated.icon)
    }

    private fun form(
        name: String = "Milk",
        targetPriceCents: Long? = null,
        barcode: String? = null,
        links: Map<String, String> = emptyMap(),
    ) = com.erosketarakoa.app.ui.detail.ItemFormValues(
        name = name, quantity = 1, unit = null, supermarkets = emptyList(),
        category = null, notes = null, icon = "1F3FA",
        targetPriceCents = targetPriceCents, barcode = barcode, links = links,
    )

    @Test
    fun saveNewItem_persistsTargetBarcodeAndLinks() = runTest {
        val vm = newViewModel()
        vm.saveNewItem(
            form(targetPriceCents = 199, barcode = "8410000000000", links = mapOf("Eroski" to "e-1", "Lidl" to "l-9")),
        )
        val item = itemDao.getAll().first { it.name == "Milk" }
        assertEquals(199L, item.targetPriceCents)
        assertEquals("8410000000000", item.barcode)
        val links = repository.getLinks(item.id)
        assertEquals(setOf("Eroski", "Lidl"), links.map { it.store }.toSet())
        assertEquals("e-1", links.first { it.store == "Eroski" }.externalProductId)
    }

    @Test
    fun saveExistingItem_reconcilesLinks_addUpdateRemove() = runTest {
        val vm = newViewModel()
        val id = repository.addItem(vm.listId, "Milk")
        repository.addLink(id, "Eroski", externalProductId = "old")
        repository.addLink(id, "Dia", externalProductId = "keep")

        // Desired: update Eroski ref, drop Dia, add Lidl.
        vm.saveExistingItem(id, form(links = mapOf("Eroski" to "new", "Lidl" to "l-1")))

        val links = repository.getLinks(id).associateBy { it.store }
        assertEquals(setOf("Eroski", "Lidl"), links.keys)
        assertEquals("new", links["Eroski"]!!.externalProductId)
        assertEquals("l-1", links["Lidl"]!!.externalProductId)
    }

    @Test
    fun setItemPricing_clearsTarget() = runTest {
        val vm = newViewModel()
        val id = repository.addItem(vm.listId, "Milk")
        repository.setItemPricing(id, 500, "abc")
        assertEquals(500L, itemDao.getById(id)!!.targetPriceCents)
        repository.setItemPricing(id, null, null)
        assertEquals(null, itemDao.getById(id)!!.targetPriceCents)
        assertEquals(null, itemDao.getById(id)!!.barcode)
    }

    @Test
    fun removeLink_softDeletesAndHides() = runTest {
        val vm = newViewModel()
        val id = repository.addItem(vm.listId, "Milk")
        val linkId = repository.addLink(id, "Eroski")
        assertEquals(1, repository.getLinks(id).size)
        repository.removeLink(linkId)
        assertTrue(repository.getLinks(id).isEmpty())
    }
}
