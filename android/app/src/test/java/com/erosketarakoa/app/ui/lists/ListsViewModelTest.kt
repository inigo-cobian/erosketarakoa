package com.erosketarakoa.app.ui.lists

import app.cash.turbine.test
import com.erosketarakoa.app.MainDispatcherRule
import com.erosketarakoa.app.data.ShoppingRepository
import com.erosketarakoa.app.fakes.FakeClock
import com.erosketarakoa.app.fakes.FakeItemDao
import com.erosketarakoa.app.fakes.FakeListDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val listDao = FakeListDao()
    private val itemDao = FakeItemDao()
    private val repository = ShoppingRepository(listDao, itemDao, FakeClock())

    // Built lazily so the ViewModel (and its viewModelScope) is created after the rule has
    // installed the test main dispatcher.
    private val viewModel by lazy { ListsViewModel(repository) }

    @Test
    fun createList_addsToState() = runTest {
        viewModel.uiState.test {
            assertTrue(awaitItem().lists.isEmpty()) // initial
            viewModel.createList("Groceries")
            val loaded = awaitItem()
            assertEquals(1, loaded.lists.size)
            assertEquals("Groceries", loaded.lists.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun blankName_isIgnored() = runTest {
        viewModel.createList("   ")
        assertTrue(listDao.getAll().isEmpty())
    }

    @Test
    fun deleteList_softDeletes() = runTest {
        val id = repository.createList("Groceries")
        viewModel.deleteList(id)
        viewModel.uiState.test {
            assertTrue(awaitItem().lists.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        val row = listDao.getById(id)!!
        assertTrue(row.isDeleted)
    }

    @Test
    fun renameList_updatesName() = runTest {
        val id = repository.createList("Old")
        viewModel.renameList(id, "New")
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("New", state.lists.first { it.id == id }.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
