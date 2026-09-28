package com.erosketarakoa.app.data

import com.erosketarakoa.app.data.local.ItemDao
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.data.ListColor
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

    suspend fun createList(name: String, color: ListColor = ListColor.DEFAULT): String {
        val id = clock.newId()
        val nextPosition = (listDao.maxPosition() ?: -1) + 1
        listDao.upsert(
            ListEntity(
                id = id,
                name = name.trim(),
                color = color.name,
                position = nextPosition,
                updatedAt = clock.nowMillis(),
                isDeleted = false,
            ),
        )
        return id
    }

    /** Persist a new manual list order. [orderedIds] is the full list top-to-bottom. */
    suspend fun reorderLists(orderedIds: List<String>) {
        val now = clock.nowMillis()
        orderedIds.forEachIndexed { index, id -> listDao.setPosition(id, index, now) }
    }

    suspend fun renameList(id: String, name: String, color: ListColor) {
        listDao.update(id, name.trim(), color.name, clock.nowMillis())
    }

    suspend fun deleteList(id: String) {
        val now = clock.nowMillis()
        // Soft-delete the list and cascade the tombstone to its items.
        listDao.softDelete(id, now)
        itemDao.softDeleteByList(id, now)
    }

    /** Un-delete a list and its cascaded items, reversing [deleteList]. */
    suspend fun restoreList(list: ListEntity) {
        val now = clock.nowMillis()
        listDao.upsert(list.copy(isDeleted = false, updatedAt = now))
        itemDao.restoreByList(list.id, now)
    }

    // ---------------- Items ----------------

    fun observeItems(listId: String): Flow<List<ItemEntity>> = itemDao.observeActiveItems(listId)

    suspend fun addItem(
        listId: String,
        name: String,
        quantity: Int = 1,
        unit: String? = null,
        supermarkets: List<String> = emptyList(),
        category: String? = null,
        notes: String? = null,
        icon: String = "1F3FA",
    ): String {
        val id = clock.newId()
        val nextPosition = (itemDao.maxPosition(listId) ?: -1) + 1
        itemDao.upsert(
            ItemEntity(
                id = id,
                listId = listId,
                name = name.trim(),
                quantity = quantity.coerceAtLeast(1),
                unit = unit?.trim()?.ifBlank { null },
                supermarkets = supermarkets.joinToString(","),
                category = category?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null },
                bought = false,
                icon = icon.ifBlank { "1F3FA" },
                position = nextPosition,
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
        unit: String?,
        supermarkets: List<String>,
        category: String?,
        notes: String?,
        icon: String = "1F3FA",
    ) {
        itemDao.updateFields(
            id = id,
            name = name.trim(),
            quantity = quantity.coerceAtLeast(1),
            unit = unit?.trim()?.ifBlank { null },
            supermarkets = supermarkets.joinToString(","),
            category = category?.trim()?.ifBlank { null },
            notes = notes?.trim()?.ifBlank { null },
            icon = icon.ifBlank { "1F3FA" },
            updatedAt = clock.nowMillis(),
        )
    }

    suspend fun setBought(id: String, bought: Boolean) {
        itemDao.setBought(id, bought, clock.nowMillis())
    }

    suspend fun deleteItem(id: String) {
        itemDao.softDelete(id, clock.nowMillis())
    }

    /** Un-delete an item, restoring it as it was. */
    suspend fun restoreItem(item: ItemEntity) {
        itemDao.upsert(item.copy(isDeleted = false, updatedAt = clock.nowMillis()))
    }

    /** Persist a new manual order. [orderedIds] is the full list top-to-bottom. */
    suspend fun reorderItems(orderedIds: List<String>) {
        val now = clock.nowMillis()
        orderedIds.forEachIndexed { index, id -> itemDao.setPosition(id, index, now) }
    }
}
