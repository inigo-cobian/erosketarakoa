package com.erosketarakoa.app.ui.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconCatalogTest {

    private val catalog = IconCatalog.fromMap(
        mapOf(
            "1F345" to "tomato food fruit vegetable red",
            "1F408" to "cat animal feline",
            "1F3FA" to "amphora vase",
        ),
    )

    @Test
    fun emptyQuery_returnsAll() {
        assertEquals(3, catalog.filter("").size)
        assertEquals(3, catalog.filter("   ").size)
    }

    @Test
    fun matchesByKeyword_caseInsensitive() {
        assertEquals(listOf("1F345"), catalog.filter("Tomato").map { it.hexcode })
        assertEquals(listOf("1F408"), catalog.filter("cat").map { it.hexcode })
    }

    @Test
    fun matchesByHexcode() {
        assertEquals(listOf("1F3FA"), catalog.filter("1f3fa").map { it.hexcode })
    }

    @Test
    fun noMatch_returnsEmpty() {
        assertTrue(catalog.filter("zzzzz").isEmpty())
    }

    @Test
    fun all_countMatchesInput() {
        assertEquals(3, catalog.all().size)
    }
}
