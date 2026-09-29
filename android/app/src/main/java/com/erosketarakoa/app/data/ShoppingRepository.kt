package com.erosketarakoa.app.data

import com.erosketarakoa.app.data.local.ItemDao
import com.erosketarakoa.app.data.local.ItemEntity
import com.erosketarakoa.app.data.ListColor
import com.erosketarakoa.app.data.local.ListDao
import com.erosketarakoa.app.data.local.ListEntity
import com.erosketarakoa.app.data.local.PriceDao
import com.erosketarakoa.app.data.local.PriceEntity
import com.erosketarakoa.app.data.local.ProductLinkDao
import com.erosketarakoa.app.data.local.ProductLinkEntity
import com.erosketarakoa.app.data.remote.ProductDto
import com.erosketarakoa.app.data.remote.RemotePriceDataSource
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
    private val linkDao: ProductLinkDao,
    private val priceDao: PriceDao,
    private val remote: RemotePriceDataSource,
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

    /** Set/clear an item's target price (cents) and barcode without touching other fields. */
    suspend fun setItemPricing(id: String, targetPriceCents: Long?, barcode: String?) {
        val item = itemDao.getById(id) ?: return
        itemDao.upsert(
            item.copy(
                targetPriceCents = targetPriceCents,
                barcode = barcode?.trim()?.ifBlank { null },
                updatedAt = clock.nowMillis(),
            ),
        )
    }

    // ---------------- Product links & prices ----------------

    fun observeLinks(itemId: String): Flow<List<ProductLinkEntity>> = linkDao.observeByItem(itemId)

    suspend fun getLinks(itemId: String): List<ProductLinkEntity> = linkDao.getByItem(itemId)

    fun observePrices(itemId: String): Flow<List<PriceEntity>> = priceDao.observeByItem(itemId)

    /** Create a link from an item to a store with an optional external product reference. */
    suspend fun addLink(
        itemId: String,
        store: String,
        externalProductId: String? = null,
        ean: String? = null,
        productName: String? = null,
    ): String {
        val id = clock.newId()
        linkDao.upsert(
            ProductLinkEntity(
                id = id,
                itemId = itemId,
                store = store,
                externalProductId = externalProductId?.trim()?.ifBlank { null },
                ean = ean?.trim()?.ifBlank { null },
                productName = productName?.trim()?.ifBlank { null },
                updatedAt = clock.nowMillis(),
                isDeleted = false,
            ),
        )
        return id
    }

    suspend fun updateLink(
        id: String,
        store: String,
        externalProductId: String?,
        ean: String? = null,
        productName: String? = null,
    ) {
        val link = linkDao.getById(id) ?: return
        linkDao.upsert(
            link.copy(
                store = store,
                externalProductId = externalProductId?.trim()?.ifBlank { null },
                ean = ean?.trim()?.ifBlank { null },
                productName = productName?.trim()?.ifBlank { null },
                updatedAt = clock.nowMillis(),
            ),
        )
    }

    suspend fun removeLink(id: String) {
        linkDao.softDelete(id, clock.nowMillis())
    }

    /**
     * Build [ItemBargainInput]s for every active item that has at least one link, so the pure
     * [com.erosketarakoa.app.data.bargain.BargainEngine] can flag them. Room is the source of truth.
     */
    suspend fun bargainInputs(): List<com.erosketarakoa.app.data.bargain.ItemBargainInput> {
        val items = itemDao.getAll().filter { !it.isDeleted }
        val linksByItem = linkDao.getAllActive().groupBy { it.itemId }
        return items.mapNotNull { item ->
            val links = linksByItem[item.id].orEmpty()
            if (links.isEmpty()) return@mapNotNull null
            val linkPrices = links.map { link ->
                val obs = priceDao.getByLink(link.id).map {
                    com.erosketarakoa.app.data.bargain.PriceObservation(it.priceCents, it.observedAt)
                }
                com.erosketarakoa.app.data.bargain.LinkPrices(link.id, link.store, obs)
            }
            com.erosketarakoa.app.data.bargain.ItemBargainInput(
                itemId = item.id,
                itemName = item.name,
                targetPriceCents = item.targetPriceCents,
                links = linkPrices,
            )
        }
    }

    /** Flagged bargains across all items, computed by the engine over cached prices. */
    suspend fun computeBargains(): List<com.erosketarakoa.app.data.bargain.ItemBargains> =
        com.erosketarakoa.app.data.bargain.BargainEngine.flagged(bargainInputs())

    // ---------------- Remote (gateway) ----------------

    /** Search the gateway for products by name and/or EAN. */
    suspend fun searchRemoteProducts(query: String? = null, ean: String? = null): List<ProductDto> =
        remote.searchProducts(query = query, ean = ean)

    /**
     * Pull `GET /items/{id}/prices` and merge the observations into the Room cache. Each remote
     * link is matched to a local [ProductLinkEntity] by (store, externalProductId or ean); prices
     * for unmatched links are ignored. Returns the number of new observations inserted.
     * De-dupes on (linkId, observedAt, priceCents).
     */
    suspend fun syncItemPrices(itemId: String): Int {
        val response = remote.itemPrices(itemId)
        val localLinks = linkDao.getByItem(itemId)
        var inserted = 0
        for (remoteLink in response.links) {
            val local = matchLink(localLinks, remoteLink.link.store, remoteLink.link.externalProductId, remoteLink.link.ean)
                ?: continue
            inserted += cachePrices(local.id, remoteLink.prices)
        }
        return inserted
    }

    /**
     * Pull `GET /bargains` and merge any observations that match a locally linked product into the
     * cache. Returns the number of new observations inserted. De-duped like [syncItemPrices].
     */
    suspend fun syncBargains(since: Long = 0): Int {
        val observations = remote.bargains(since)
        val allLinks = linkDao.getAllActive()
        var inserted = 0
        for (obs in observations) {
            val local = matchLink(allLinks, obs.store, obs.externalProductId, obs.ean) ?: continue
            if (cacheOne(local.id, obs.priceCents, obs.observedAt, obs.source, obs.currency)) inserted++
        }
        return inserted
    }

    private fun matchLink(
        links: List<ProductLinkEntity>,
        store: String,
        externalProductId: String?,
        ean: String?,
    ): ProductLinkEntity? = links.firstOrNull { link ->
        link.store == store && when {
            externalProductId != null && link.externalProductId != null -> link.externalProductId == externalProductId
            ean != null && link.ean != null -> link.ean == ean
            else -> false
        }
    }

    private suspend fun cachePrices(
        linkId: String,
        prices: List<com.erosketarakoa.app.data.remote.ObservationDto>,
    ): Int {
        var inserted = 0
        for (obs in prices) {
            if (cacheOne(linkId, obs.priceCents, obs.observedAt, obs.source, obs.currency)) inserted++
        }
        return inserted
    }

    /** Insert one observation unless an identical (linkId, observedAt, priceCents) row exists. */
    private suspend fun cacheOne(
        linkId: String,
        priceCents: Long,
        observedAt: Long,
        source: String,
        currency: String,
    ): Boolean {
        if (priceDao.countMatching(linkId, observedAt, priceCents) > 0) return false
        priceDao.upsert(
            PriceEntity(
                id = clock.newId(),
                linkId = linkId,
                priceCents = priceCents,
                currency = currency,
                observedAt = observedAt,
                source = source,
                updatedAt = clock.nowMillis(),
            ),
        )
        return true
    }

    /**
     * Sync bargains from the gateway then compute flagged bargains over the cache. If the fetch
     * fails (offline), skip the sync and compute over whatever is already cached — never throws.
     */
    suspend fun syncAndComputeBargains(): List<com.erosketarakoa.app.data.bargain.ItemBargains> {
        runCatching { syncBargains() }  // best-effort; offline fallback uses existing cache
        return computeBargains()
    }

    /** Record an observed price for a link. Money in cents. Used by manual seeding and sync. */
    suspend fun addPrice(
        linkId: String,
        priceCents: Long,
        observedAt: Long = clock.nowMillis(),
        source: String = "manual",
        currency: String = "EUR",
    ): String {
        val id = clock.newId()
        priceDao.upsert(
            PriceEntity(
                id = id,
                linkId = linkId,
                priceCents = priceCents,
                currency = currency,
                observedAt = observedAt,
                source = source,
                updatedAt = clock.nowMillis(),
            ),
        )
        return id
    }
}
