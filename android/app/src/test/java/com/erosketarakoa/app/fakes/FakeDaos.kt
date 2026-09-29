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

    override suspend fun maxPosition(listId: String): Int? =
        state.value.values.filter { it.listId == listId && !it.isDeleted }.maxOfOrNull { it.position }

    override suspend fun setPosition(id: String, position: Int, updatedAt: Long) {
        state.value[id]?.let { upsert(it.copy(position = position, updatedAt = updatedAt)) }
    }

    override suspend fun updateFields(
        id: String,
        name: String,
        quantity: Int,
        unit: String?,
        supermarkets: String,
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
                    unit = unit,
                    supermarkets = supermarkets,
                    category = category,
                    notes = notes,
                    icon = icon,
                    updatedAt = updatedAt,
                ),
            )
        }
    }

    override suspend fun restoreByList(listId: String, updatedAt: Long) {
        state.value = state.value.mapValues { (_, v) ->
            if (v.listId == listId) v.copy(isDeleted = false, updatedAt = updatedAt) else v
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

class FakeProductLinkDao : com.erosketarakoa.app.data.local.ProductLinkDao {
    private val state = MutableStateFlow<Map<String, com.erosketarakoa.app.data.local.ProductLinkEntity>>(emptyMap())

    override fun observeByItem(itemId: String): Flow<List<com.erosketarakoa.app.data.local.ProductLinkEntity>> =
        state.map { m -> m.values.filter { it.itemId == itemId && !it.isDeleted }.sortedBy { it.store } }

    override suspend fun getByItem(itemId: String): List<com.erosketarakoa.app.data.local.ProductLinkEntity> =
        state.value.values.filter { it.itemId == itemId && !it.isDeleted }.sortedBy { it.store }

    override suspend fun getAllActive(): List<com.erosketarakoa.app.data.local.ProductLinkEntity> =
        state.value.values.filter { !it.isDeleted }

    override suspend fun getById(id: String): com.erosketarakoa.app.data.local.ProductLinkEntity? = state.value[id]

    override suspend fun upsert(link: com.erosketarakoa.app.data.local.ProductLinkEntity) {
        state.value = state.value + (link.id to link)
    }

    override suspend fun upsertAll(links: List<com.erosketarakoa.app.data.local.ProductLinkEntity>) {
        state.value = state.value + links.associateBy { it.id }
    }

    override suspend fun softDelete(id: String, updatedAt: Long) {
        state.value[id]?.let { upsert(it.copy(isDeleted = true, updatedAt = updatedAt)) }
    }

    override suspend fun clear() {
        state.value = emptyMap()
    }
}

class FakePriceDao(
    private val links: FakeProductLinkDao = FakeProductLinkDao(),
) : com.erosketarakoa.app.data.local.PriceDao {

    private val state = MutableStateFlow<Map<String, com.erosketarakoa.app.data.local.PriceEntity>>(emptyMap())

    override fun observeByLink(linkId: String): Flow<List<com.erosketarakoa.app.data.local.PriceEntity>> =
        state.map { m -> m.values.filter { it.linkId == linkId }.sortedBy { it.observedAt } }

    override suspend fun getByLink(linkId: String): List<com.erosketarakoa.app.data.local.PriceEntity> =
        state.value.values.filter { it.linkId == linkId }.sortedBy { it.observedAt }

    override fun observeByItem(itemId: String): Flow<List<com.erosketarakoa.app.data.local.PriceEntity>> =
        state.map { m ->
            val linkIds = links.getByItem(itemId).map { it.id }.toSet()
            m.values.filter { it.linkId in linkIds }.sortedBy { it.observedAt }
        }

    override suspend fun getById(id: String): com.erosketarakoa.app.data.local.PriceEntity? = state.value[id]

    override suspend fun countMatching(linkId: String, observedAt: Long, priceCents: Long): Int =
        state.value.values.count {
            it.linkId == linkId && it.observedAt == observedAt && it.priceCents == priceCents
        }

    override suspend fun upsert(price: com.erosketarakoa.app.data.local.PriceEntity) {
        state.value = state.value + (price.id to price)
    }

    override suspend fun upsertAll(prices: List<com.erosketarakoa.app.data.local.PriceEntity>) {
        state.value = state.value + prices.associateBy { it.id }
    }

    override suspend fun delete(id: String) {
        state.value = state.value - id
    }

    override suspend fun clear() {
        state.value = emptyMap()
    }
}

/**
 * Configurable PriceApi for JVM tests. Defaults to empty responses; set [itemPricesResponse] /
 * [bargainsResponse], or set [failWith] to simulate an offline/network error.
 */
class FakePriceApi : com.erosketarakoa.app.data.remote.PriceApi {
    var failWith: Throwable? = null
    var searchResponse = com.erosketarakoa.app.data.remote.SearchResponse(emptyList())
    var itemPricesResponse: com.erosketarakoa.app.data.remote.ItemPricesResponse? = null
    var bargainsResponse = com.erosketarakoa.app.data.remote.BargainsResponse(0, emptyList())

    override suspend fun searchProducts(q: String?, ean: String?): com.erosketarakoa.app.data.remote.SearchResponse {
        failWith?.let { throw it }
        return searchResponse
    }

    override suspend fun createLink(itemId: String, body: com.erosketarakoa.app.data.remote.LinkRequestDto) =
        com.erosketarakoa.app.data.remote.LinkDto(0, itemId, body.store, body.externalProductId, body.ean)

    override suspend fun itemPrices(itemId: String): com.erosketarakoa.app.data.remote.ItemPricesResponse {
        failWith?.let { throw it }
        return itemPricesResponse ?: com.erosketarakoa.app.data.remote.ItemPricesResponse(itemId, emptyList())
    }

    override suspend fun bargains(since: Long): com.erosketarakoa.app.data.remote.BargainsResponse {
        failWith?.let { throw it }
        return bargainsResponse
    }
}

/** Convenience RemotePriceDataSource backed by [FakePriceApi] for tests. */
fun fakeRemoteDataSource() = com.erosketarakoa.app.data.remote.RemotePriceDataSource(FakePriceApi())

/** Deterministic clock: monotonically increasing time, sequential ids. */
class FakeClock : Clock {
    private var time = 1_000L
    private var counter = 0
    override fun nowMillis(): Long = time++
    override fun newId(): String = "id-${counter++}"
}
