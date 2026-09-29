package com.erosketarakoa.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit client for the backend gateway. DTOs mirror the gateway JSON exactly
 * (money in integer cents, timestamps epoch millis). Kept internal to the data layer —
 * the UI never sees these types (see [RemotePriceDataSource] and ShoppingRepository).
 */

@Serializable
data class ProductDto(
    val store: String,
    val externalProductId: String? = null,
    val ean: String? = null,
    val name: String? = null,
)

@Serializable
data class SearchResponse(val products: List<ProductDto> = emptyList())

@Serializable
data class LinkRequestDto(
    val store: String,
    val externalProductId: String? = null,
    val ean: String? = null,
)

@Serializable
data class LinkDto(
    val id: Int,
    val itemId: String,
    val store: String,
    val externalProductId: String? = null,
    val ean: String? = null,
)

@Serializable
data class ObservationDto(
    val store: String,
    val externalProductId: String? = null,
    val ean: String? = null,
    val name: String? = null,
    val priceCents: Long,
    val currency: String = "EUR",
    val observedAt: Long,
    val source: String,
)

@Serializable
data class LinkPricesDto(
    val link: LinkDto,
    val prices: List<ObservationDto> = emptyList(),
)

@Serializable
data class ItemPricesResponse(
    val itemId: String,
    val links: List<LinkPricesDto> = emptyList(),
)

@Serializable
data class BargainsResponse(
    val since: Long = 0,
    val observations: List<ObservationDto> = emptyList(),
)

interface PriceApi {

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") q: String? = null,
        @Query("ean") ean: String? = null,
    ): SearchResponse

    @POST("items/{id}/links")
    suspend fun createLink(
        @Path("id") itemId: String,
        @Body body: LinkRequestDto,
    ): LinkDto

    @GET("items/{id}/prices")
    suspend fun itemPrices(@Path("id") itemId: String): ItemPricesResponse

    @GET("bargains")
    suspend fun bargains(@Query("since") since: Long = 0): BargainsResponse
}
