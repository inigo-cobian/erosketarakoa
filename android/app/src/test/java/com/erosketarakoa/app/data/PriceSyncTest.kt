package com.erosketarakoa.app.data

import com.erosketarakoa.app.data.remote.ItemPricesResponse
import com.erosketarakoa.app.data.remote.LinkDto
import com.erosketarakoa.app.data.remote.LinkPricesDto
import com.erosketarakoa.app.data.remote.ObservationDto
import com.erosketarakoa.app.data.remote.RemotePriceDataSource
import com.erosketarakoa.app.fakes.FakeClock
import com.erosketarakoa.app.fakes.FakeItemDao
import com.erosketarakoa.app.fakes.FakeListDao
import com.erosketarakoa.app.fakes.FakePriceApi
import com.erosketarakoa.app.fakes.FakePriceDao
import com.erosketarakoa.app.fakes.FakeProductLinkDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceSyncTest {

    private val listDao = FakeListDao()
    private val itemDao = FakeItemDao()
    private val linkDao = FakeProductLinkDao()
    private val priceDao = FakePriceDao(linkDao)
    private val api = FakePriceApi()
    private val repo = ShoppingRepository(
        listDao, itemDao, linkDao, priceDao, RemotePriceDataSource(api), FakeClock(),
    )

    private suspend fun seedItemWithLink(store: String = "Mercadona", ext: String = "m1"): Pair<String, String> {
        val itemId = repo.addItem("l1", "Milk")
        val linkId = repo.addLink(itemId, store, externalProductId = ext)
        return itemId to linkId
    }

    private fun obs(cents: Long, at: Long, store: String = "Mercadona", ext: String = "m1", ean: String? = null) =
        ObservationDto(store, ext, ean, "Milk", cents, "EUR", at, "open_prices")

    @Test
    fun syncItemPrices_mergesRemoteHistoryIntoCache() = runTest {
        val (itemId, linkId) = seedItemWithLink()
        api.itemPricesResponse = ItemPricesResponse(
            itemId,
            listOf(
                LinkPricesDto(
                    link = LinkDto(1, itemId, "Mercadona", "m1", null),
                    prices = listOf(obs(200, 100), obs(180, 200)),
                ),
            ),
        )
        val inserted = repo.syncItemPrices(itemId)
        assertEquals(2, inserted)
        assertEquals(listOf(200L, 180L), priceDao.getByLink(linkId).map { it.priceCents })
    }

    @Test
    fun syncItemPrices_dedupesIdenticalObservations() = runTest {
        val (itemId, linkId) = seedItemWithLink()
        api.itemPricesResponse = ItemPricesResponse(
            itemId,
            listOf(LinkPricesDto(LinkDto(1, itemId, "Mercadona", "m1", null), listOf(obs(200, 100)))),
        )
        assertEquals(1, repo.syncItemPrices(itemId))
        // Second sync with the same data inserts nothing.
        assertEquals(0, repo.syncItemPrices(itemId))
        assertEquals(1, priceDao.getByLink(linkId).size)
    }

    @Test
    fun syncItemPrices_ignoresUnmatchedRemoteLinks() = runTest {
        val (itemId, _) = seedItemWithLink(store = "Mercadona", ext = "m1")
        // Remote link is a different store/product with no local match.
        api.itemPricesResponse = ItemPricesResponse(
            itemId,
            listOf(LinkPricesDto(LinkDto(9, itemId, "Eroski", "e9", null), listOf(obs(500, 100, store = "Eroski", ext = "e9")))),
        )
        assertEquals(0, repo.syncItemPrices(itemId))
    }

    @Test
    fun syncBargains_mergesMatchingObservations() = runTest {
        val (_, linkId) = seedItemWithLink()
        api.bargainsResponse = com.erosketarakoa.app.data.remote.BargainsResponse(
            0, listOf(obs(150, 300)),
        )
        assertEquals(1, repo.syncBargains())
        assertEquals(150L, priceDao.getByLink(linkId).last().priceCents)
    }

    @Test
    fun syncAndComputeBargains_offlineFallbackUsesCache() = runTest {
        val (_, linkId) = seedItemWithLink()
        // Seed local history so a below-target/drop can be computed offline.
        repo.setItemPricing(itemDao.getAll().first().id, targetPriceCents = 300, barcode = null)
        repo.addPrice(linkId, 250, observedAt = 100, source = "manual")
        api.failWith = RuntimeException("offline")

        // Does not throw; computes over existing cache.
        val flagged = repo.syncAndComputeBargains()
        assertEquals(1, flagged.size) // below-target (250 <= 300)
    }
}
