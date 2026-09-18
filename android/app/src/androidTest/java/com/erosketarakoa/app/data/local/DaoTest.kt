package com.erosketarakoa.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: AppDatabase
    private lateinit var listDao: ListDao
    private lateinit var itemDao: ItemDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        listDao = db.listDao()
        itemDao = db.itemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserveList() = runBlocking {
        val id = UUID.randomUUID().toString()
        listDao.upsert(ListEntity(id = id, name = "Groceries"))
        val lists = listDao.observeActiveLists().first()
        assertEquals(1, lists.size)
        assertEquals("Groceries", lists.first().name)
    }

    @Test
    fun renameUpdatesName() = runBlocking {
        val id = UUID.randomUUID().toString()
        listDao.upsert(ListEntity(id = id, name = "Old"))
        listDao.rename(id, "New", updatedAt = 123L)
        val reloaded = listDao.getById(id)!!
        assertEquals("New", reloaded.name)
        assertEquals(123L, reloaded.updatedAt)
    }

    @Test
    fun softDeleteHidesFromActiveButKeepsRow() = runBlocking {
        val id = UUID.randomUUID().toString()
        listDao.upsert(ListEntity(id = id, name = "Groceries"))
        listDao.softDelete(id, updatedAt = 1L)
        val active = listDao.observeActiveLists().first()
        assertTrue(active.isEmpty())
        val row = listDao.getById(id)!!
        assertTrue(row.isDeleted)
    }

    @Test
    fun observeActiveListsEmitsUpdates() = runBlocking {
        listDao.observeActiveLists().test {
            assertEquals(0, awaitItem().size)
            listDao.upsert(ListEntity(id = "a", name = "First"))
            assertEquals(1, awaitItem().size)
            listDao.upsert(ListEntity(id = "b", name = "Second"))
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun itemCrudAndBoughtToggle() = runBlocking {
        val listId = UUID.randomUUID().toString()
        listDao.upsert(ListEntity(id = listId, name = "Groceries"))
        val itemId = UUID.randomUUID().toString()
        itemDao.upsert(
            ItemEntity(
                id = itemId,
                listId = listId,
                name = "Milk",
                quantity = 2,
                category = "Dairy",
                notes = "Semi",
            ),
        )
        var items = itemDao.observeActiveItems(listId).first()
        assertEquals(1, items.size)
        assertEquals("Milk", items.first().name)

        itemDao.setBought(itemId, true, updatedAt = 5L)
        items = itemDao.observeActiveItems(listId).first()
        assertTrue(items.first().bought)

        itemDao.softDelete(itemId, updatedAt = 6L)
        items = itemDao.observeActiveItems(listId).first()
        assertTrue(items.isEmpty())
        val row = itemDao.getById(itemId)!!
        assertTrue(row.isDeleted)
    }

    @Test
    fun clearRemovesEverything() = runBlocking {
        listDao.upsert(ListEntity(id = "a", name = "A"))
        listDao.clear()
        assertNull(listDao.getById("a"))
        assertFalse(listDao.observeActiveLists().first().isNotEmpty())
    }
}
