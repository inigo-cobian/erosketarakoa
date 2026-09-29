package com.erosketarakoa.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class PriceApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: PriceApi

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PriceApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun searchProducts_parsesProducts_andSendsQuery() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"products":[{"store":"Mercadona","externalProductId":"m1","ean":"841","name":"Leche"}]}""",
            ),
        )
        val products = api.searchProducts(q = "leche", ean = null)
        assertEquals(1, products.products.size)
        assertEquals("Mercadona", products.products.first().store)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.startsWith("/products/search"))
        assertTrue(request.path!!.contains("q=leche"))
    }

    @Test
    fun createLink_postsBody_andParsesLink() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":7,"itemId":"item-1","store":"Eroski","externalProductId":"e1","ean":null}""",
            ),
        )
        val link = api.createLink("item-1", LinkRequestDto(store = "Eroski", externalProductId = "e1"))
        assertEquals(7, link.id)
        assertEquals("Eroski", link.store)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/items/item-1/links", request.path)
        val sent = request.body.readUtf8()
        assertTrue(sent.contains("\"store\":\"Eroski\""))
        assertTrue(sent.contains("\"externalProductId\":\"e1\""))
    }

    @Test
    fun itemPrices_parsesLinksAndHistory() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                {"itemId":"item-1","links":[
                  {"link":{"id":1,"itemId":"item-1","store":"Mercadona","externalProductId":"m1","ean":"841"},
                   "prices":[
                     {"store":"Mercadona","externalProductId":"m1","ean":"841","name":"Leche","priceCents":200,"currency":"EUR","observedAt":100,"source":"open_prices"},
                     {"store":"Mercadona","externalProductId":"m1","ean":"841","name":"Leche","priceCents":180,"currency":"EUR","observedAt":300,"source":"open_prices"}
                   ]}
                ]}
                """.trimIndent(),
            ),
        )
        val resp = api.itemPrices("item-1")
        assertEquals("item-1", resp.itemId)
        assertEquals(1, resp.links.size)
        assertEquals(listOf(200L, 180L), resp.links.first().prices.map { it.priceCents })
        assertEquals("/items/item-1/prices", server.takeRequest().path)
    }

    @Test
    fun bargains_parsesObservations_andSendsSince() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"since":250,"observations":[{"store":"Eroski","priceCents":199,"observedAt":300,"source":"scraper"}]}""",
            ),
        )
        val obs = api.bargains(since = 250)
        assertEquals(1, obs.observations.size)
        assertEquals(199L, obs.observations.first().priceCents)
        assertTrue(server.takeRequest().path!!.contains("since=250"))
    }
}
