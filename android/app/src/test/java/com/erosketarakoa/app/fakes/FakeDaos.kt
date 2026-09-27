package com.erosketarakoa.app.fakes

import com.erosketarakoa.app.data.Clock
import com.erosketarakoa.app.data.local.ItemDao
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.data.local.ListDao
import com.erosketarakoa.app.data.local.ListEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory fakes so repository + ViewModel logic can be unit-tested on the JVM. */

class FakeListDao : ListDao {
    private val state = MutableStateFlow<Map<String, ListEntity>>(emptyMap())

    override fun observeActiveLists(): Flow<List<ListEntity>> =
        state.map { m -> m.values.filter { !it.isDeleted }.sortedBy { it.position } }

    override suspend fun maxPosition(): Int? =
        state.value.values.filter { !it.isDeleted }.maxOfOrNull { it.position }

    override suspend fun setPosition(id: String, position: Int, updatedAt: Long) {
        state.value[id]?.let { upsert(it.copy(position = position, updatedAt = updatedAt)) }
    }

    override fun observeList(id: String): Flow<ListEntity?> = state.map { it[id] }

    override suspend fun getById(id: String): ListEntity? = state.value[id]

    override suspend fun getAll(): List<ListEntity> = state.value.values.toList()

    override suspend fun insert(list: ListEntity) = upsert(list)

    override suspend fun upsert(list: ListEntity) {
        state.value = state.value + (list.id to list)
    }

    override suspend fun upsertAll(lists: List<ListEntity>) {
        state.value = state.value + lists.associateBy { it.id }
    }

    override suspend fun rename(id: String, name: String, updatedAt: Long) {
        state.value[id]?.let {
            upsert(it.copy(name = name, updatedAt = updatedAt))
        }
    }

    override suspend fun update(id: String, name: String, color: String, updatedAt: Long) {
        state.value[id]?.let {
            upsert(it.copy(name = name, color = color, updatedAt = updatedAt))
        }
    }

    override suspend fun softDelete(id: String, updatedAt: Long) {
        state.value[id]?.let {
            upsert(it.copy(isDeleted = true, updatedAt = updatedAt))
        }
    }

    override suspend fun clear() {
        state.value = emptyMap()
    }
}

class FakeItemDao : ItemDao {
    private val state = MutableStateFlow<Map<String, ItemEntity>>(emptyMap())

    override fun observeActiveItems(listId: String): Flow<List<ItemEntity>> =
        state.map { m ->
            m.values.filter { it.listId == listId && !it.isDeleted }
                .sortedWith(compareBy({ it.bought }, { it.name.lowercase() }))
        }

    override suspend fun getById(id: String): ItemEntity? = state.value[id]

    override suspend fun getAll(): List<ItemEntity> = state.value.values.toList()

    override suspend fun insert(item: ItemEntity) = upsert(item)

    override suspend fun upsert(item: ItemEntity) {
        state.value = state.value + (item.id to item)
    }

    override suspend fun upsertAll(items: List<ItemEntity>) {
        state.value = state.value + items.associateBy { it.id }
    }

    override suspend fun updateFields(
        id: String,
        name: String,
        quantity: Int,
        category: String?,
        notes: String?,
        icon: String,
        updatedAt: Long,
    ) {
        state.value[id]?.let {
            upsert(
                it.copy(
                    name = name,
                    quantity = quantity,
                    category = category,
                    notes = notes,
                    icon = icon,
                    updatedAt = updatedAt,
                ),
            )
        }
    }

    override suspend fun setBought(id: String, bought: Boolean, updatedAt: Long) {
        state.value[id]?.let {
            upsert(it.copy(bought = bought, updatedAt = updatedAt))
        }
    }

    override suspend fun softDelete(id: String, updatedAt: Long) {
        state.value[id]?.let {
            upsert(it.copy(isDeleted = true, updatedAt = updatedAt))
        }
    }

    override suspend fun softDeleteByList(listId: String, updatedAt: Long) {
        val updated = state.value.mapValues { (_, v) ->
            if (v.listId == listId) v.copy(isDeleted = true, updatedAt = updatedAt) else v
        }
        state.value = updated
    }

    override suspend fun clear() {
        state.value = emptyMap()
    }
}

/** Deterministic clock: monotonically increasing time, sequential ids. */
class FakeClock : Clock {
    private var time = 1_000L
    private var counter = 0
    override fun nowMillis(): Long = time++
    override fun newId(): String = "id-${counter++}"
}
