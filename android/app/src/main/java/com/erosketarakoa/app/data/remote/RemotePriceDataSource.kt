package com.erosketarakoa.app.data.remote

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over [PriceApi]. The repository depends on this, not on Retrofit directly,
 * so the networking library stays isolated in the data layer.
 */
@Singleton
class RemotePriceDataSource @Inject constructor(
    private val api: PriceApi,
) {
    suspend fun searchProducts(query: String? = null, ean: String? = null): List<ProductDto> =
        api.searchProducts(q = query, ean = ean).products

    suspend fun createLink(itemId: String, store: String, externalProductId: String?, ean: String?): LinkDto =
        api.createLink(itemId, LinkRequestDto(store = store, externalProductId = externalProductId, ean = ean))

    suspend fun itemPrices(itemId: String): ItemPricesResponse = api.itemPrices(itemId)

    suspend fun bargains(since: Long = 0): List<ObservationDto> = api.bargains(since).observations
}
