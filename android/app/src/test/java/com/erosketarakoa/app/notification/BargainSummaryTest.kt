package com.erosketarakoa.app.notification

import com.erosketarakoa.app.data.bargain.BargainKind
import com.erosketarakoa.app.data.bargain.BargainReason
import com.erosketarakoa.app.data.bargain.ItemBargains
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BargainSummaryTest {

    private fun bargain(name: String) =
        ItemBargains(name, name, listOf(BargainReason(BargainKind.PRICE_DROP, "Eroski", 100)))

    @Test
    fun emptyList_returnsNull() {
        assertNull(BargainSummary.build(emptyList()))
    }

    @Test
    fun singleBargain_usesSingularTitle() {
        val content = BargainSummary.build(listOf(bargain("Milk")))!!
        assertEquals("1 bargain today", content.title)
        assertEquals("Milk", content.body)
    }

    @Test
    fun multipleBargains_pluralTitleAndAllNames() {
        val content = BargainSummary.build(listOf(bargain("Milk"), bargain("Eggs")))!!
        assertEquals("2 bargains today", content.title)
        assertEquals("Milk, Eggs", content.body)
    }

    @Test
    fun manyBargains_truncatesBodyToThreePlusMore() {
        val content = BargainSummary.build(
            listOf(bargain("A"), bargain("B"), bargain("C"), bargain("D"), bargain("E")),
        )!!
        assertEquals("5 bargains today", content.title)
        assertEquals("A, B, C and 2 more", content.body)
    }
}
