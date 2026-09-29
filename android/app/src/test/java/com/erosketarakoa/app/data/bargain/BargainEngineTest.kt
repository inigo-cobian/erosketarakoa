package com.erosketarakoa.app.data.bargain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BargainEngineTest {

    private fun link(store: String, vararg prices: Pair<Long, Long>) =
        LinkPrices(
            linkId = "lk-$store",
            store = store,
            prices = prices.map { PriceObservation(it.first, it.second) },
        )

    private fun input(target: Long?, vararg links: LinkPrices) =
        ItemBargainInput("i1", "Milk", target, links.toList())

    private fun kinds(b: ItemBargains) = b.reasons.map { it.kind }.toSet()

    @Test
    fun belowTarget_flagsWhenLatestAtOrUnderTarget() {
        // latest = 150 (observedAt 2), target 150 -> at target flags.
        val result = BargainEngine.evaluate(
            input(150, link("Eroski", 200L to 1L, 150L to 2L)),
        )
        val reason = result.reasons.single { it.kind == BargainKind.BELOW_TARGET }
        assertEquals("Eroski", reason.store)
        assertEquals(150L, reason.priceCents)
    }

    @Test
    fun belowTarget_notFlaggedWhenAboveTarget() {
        val result = BargainEngine.evaluate(
            input(100, link("Eroski", 150L to 1L)),
        )
        assertTrue(result.reasons.none { it.kind == BargainKind.BELOW_TARGET })
    }

    @Test
    fun priceDrop_flagsWhenLatestBelowPrevious() {
        // previous 200 (t1), latest 180 (t2) -> drop.
        val result = BargainEngine.evaluate(
            input(null, link("Lidl", 200L to 1L, 180L to 2L)),
        )
        val reason = result.reasons.single { it.kind == BargainKind.PRICE_DROP }
        assertEquals("Lidl", reason.store)
        assertEquals(180L, reason.priceCents)
    }

    @Test
    fun priceDrop_notFlaggedOnIncreaseOrSingleObservation() {
        val increased = BargainEngine.evaluate(input(null, link("Lidl", 100L to 1L, 120L to 2L)))
        assertTrue(increased.reasons.none { it.kind == BargainKind.PRICE_DROP })
        val single = BargainEngine.evaluate(input(null, link("Lidl", 100L to 1L)))
        assertTrue(single.reasons.none { it.kind == BargainKind.PRICE_DROP })
    }

    @Test
    fun cheapestMarket_needsTwoStores_picksLowestLatest() {
        val result = BargainEngine.evaluate(
            input(
                null,
                link("Eroski", 250L to 5L),
                link("Dia", 199L to 5L),
                link("Lidl", 300L to 5L),
            ),
        )
        val reason = result.reasons.single { it.kind == BargainKind.CHEAPEST_MARKET }
        assertEquals("Dia", reason.store)
        assertEquals(199L, reason.priceCents)
    }

    @Test
    fun cheapestMarket_notFlaggedWithSingleStore() {
        val result = BargainEngine.evaluate(input(null, link("Eroski", 199L to 1L)))
        assertTrue(result.reasons.none { it.kind == BargainKind.CHEAPEST_MARKET })
    }

    @Test
    fun rulesCombine_onItemCanCarryMultipleReasons() {
        // Eroski: drops 250->150 (drop + below-target 150). Dia: 300 latest. Two stores -> cheapest Eroski.
        val result = BargainEngine.evaluate(
            input(
                160,
                link("Eroski", 250L to 1L, 150L to 2L),
                link("Dia", 300L to 2L),
            ),
        )
        assertEquals(
            setOf(BargainKind.BELOW_TARGET, BargainKind.PRICE_DROP, BargainKind.CHEAPEST_MARKET),
            kinds(result),
        )
    }

    @Test
    fun flagged_dropsItemsWithNoReasons() {
        val bargain = input(null, link("Eroski", 100L to 1L, 90L to 2L)) // has a drop
        val boring = ItemBargainInput("i2", "Eggs", null, listOf(link("Eroski", 100L to 1L)))
        val flagged = BargainEngine.flagged(listOf(bargain, boring))
        assertEquals(listOf("i1"), flagged.map { it.itemId })
    }
}
