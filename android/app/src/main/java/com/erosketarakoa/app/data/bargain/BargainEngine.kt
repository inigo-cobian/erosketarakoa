package com.erosketarakoa.app.data.bargain

/**
 * Pure-Kotlin bargain rules. No Android or Room dependencies so it is trivially unit-testable.
 * This is the ONLY place bargain rules live (the backend serves raw history, never rules).
 *
 * Money is integer cents throughout. Observations are compared by [PriceObservation.observedAt].
 */

/** One observed price for a store link. */
data class PriceObservation(val priceCents: Long, val observedAt: Long)

/** A store link plus its observed price history for one item. */
data class LinkPrices(
    val linkId: String,
    val store: String,
    val prices: List<PriceObservation>,
) {
    /** Latest observation by observedAt, or null if no prices. */
    val latest: PriceObservation? get() = prices.maxByOrNull { it.observedAt }

    /** The observation immediately before [latest], or null if fewer than two. */
    val previous: PriceObservation?
        get() {
            if (prices.size < 2) return null
            val sorted = prices.sortedBy { it.observedAt }
            return sorted[sorted.size - 2]
        }
}

/** Input for one item: its target (cents, nullable) and each linked store's price history. */
data class ItemBargainInput(
    val itemId: String,
    val itemName: String,
    val targetPriceCents: Long?,
    val links: List<LinkPrices>,
)

enum class BargainKind { BELOW_TARGET, PRICE_DROP, CHEAPEST_MARKET }

/** A single reason an item is a bargain, tied to the relevant store. */
data class BargainReason(
    val kind: BargainKind,
    val store: String,
    val priceCents: Long,
)

/** All reasons found for one item. Empty [reasons] = not a bargain. */
data class ItemBargains(
    val itemId: String,
    val itemName: String,
    val reasons: List<BargainReason>,
)

object BargainEngine {

    /** Evaluate one item against all three rules. Rules combine; result may hold multiple reasons. */
    fun evaluate(input: ItemBargainInput): ItemBargains {
        val reasons = mutableListOf<BargainReason>()

        // Rule 1: below-target — any link whose latest price <= target.
        val target = input.targetPriceCents
        if (target != null) {
            input.links.forEach { link ->
                val latest = link.latest ?: return@forEach
                if (latest.priceCents <= target) {
                    reasons += BargainReason(BargainKind.BELOW_TARGET, link.store, latest.priceCents)
                }
            }
        }

        // Rule 2: price-drop — any link whose latest price < its previous observed price.
        input.links.forEach { link ->
            val latest = link.latest ?: return@forEach
            val previous = link.previous ?: return@forEach
            if (latest.priceCents < previous.priceCents) {
                reasons += BargainReason(BargainKind.PRICE_DROP, link.store, latest.priceCents)
            }
        }

        // Rule 3: cheapest-market — only with >= 2 linked stores that have a latest price.
        val withLatest = input.links.mapNotNull { link -> link.latest?.let { link to it } }
        if (withLatest.size >= 2) {
            val cheapest = withLatest.minByOrNull { it.second.priceCents }!!
            reasons += BargainReason(BargainKind.CHEAPEST_MARKET, cheapest.first.store, cheapest.second.priceCents)
        }

        return ItemBargains(input.itemId, input.itemName, reasons)
    }

    /** Evaluate many items, returning only those that carry at least one reason. */
    fun flagged(inputs: List<ItemBargainInput>): List<ItemBargains> =
        inputs.map { evaluate(it) }.filter { it.reasons.isNotEmpty() }
}
