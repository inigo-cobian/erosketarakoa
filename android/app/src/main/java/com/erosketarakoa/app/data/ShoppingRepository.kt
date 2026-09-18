package com.erosketarakoa.app.data

import com.erosketarakoa.app.data.local.ItemDao
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.data.local.ListDao
import com.erosketarakoa.app.data.local.ListEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local repository. All UI reads and writes go through here; Room is the single source of truth.
 * Creates generate a client-side UUID and timestamp.
 */
@Singleton
class ShoppingRepository @Inject constructor(
    private val listDao: ListDao,
    private val itemDao: ItemDao,
    private val clock: Clock,
) {
    // ---------------- Lists ----------------

    fun observeLists(): Flow<List<ListEntity>> = listDao.observeActiveLists()

    fun observeList(id: String): Flow<ListEntity?> = listDao.observeList(id)

    suspend fun createList(name: String): String {
        val id = clock.newId()
        listDao.upsert(
            ListEntity(
                id = id,
                name = name.trim(),
                updatedAt = clock.nowMillis(),
                isDeleted = false,
            ),
        )
        return id
    }

    suspend fun renameList(id: String, name: String) {
        listDao.rename(id, name.trim(), clock.nowMillis())
    }

    suspend fun deleteList(id: String) {
        val now = clock.nowMillis()
        // Soft-delete the list and cascade the tombstone to its items.
        listDao.softDelete(id, now)
        itemDao.softDeleteByList(id, now)
    }

    // ---------------- Items ----------------

    fun observeItems(listId: String): Flow<List<ItemEntity>> = itemDao.observeActiveItems(listId)

    suspend fun addItem(
        listId: String,
        name: String,
        quantity: Int = 1,
        category: String? = null,
        notes: String? = null,
    ): String {
        val id = clock.newId()
        itemDao.upsert(
            ItemEntity(
                id = id,
                listId = listId,
                name = name.trim(),
                quantity = quantity.coerceAtLeast(1),
                category = category?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null },
                bought = false,
                updatedAt = clock.nowMillis(),
                isDeleted = false,
            ),
        )
        return id
    }

    suspend fun updateItem(
        id: String,
        name: String,
        quantity: Int,
        category: String?,
        notes: String?,
    ) {
        itemDao.updateFields(
            id = id,
            name = name.trim(),
            quantity = quantity.coerceAtLeast(1),
            category = category?.trim()?.ifBlank { null },
            notes = notes?.trim()?.ifBlank { null },
            updatedAt = clock.nowMillis(),
        )
    }

    suspend fun setBought(id: String, bought: Boolean) {
        itemDao.setBought(id, bought, clock.nowMillis())
    }

    suspend fun deleteItem(id: String) {
        itemDao.softDelete(id, clock.nowMillis())
    }
}
