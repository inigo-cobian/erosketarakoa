package com.erosketarakoa.app.notification

import com.erosketarakoa.app.data.bargain.ItemBargains

/** Pure notification content, built without any Android dependency so it can be unit-tested. */
data class BargainNotificationContent(val title: String, val body: String)

object BargainSummary {

    /**
     * Build the daily notification content from flagged bargains. Returns null when there is
     * nothing to report (caller should not post a notification in that case).
     */
    fun build(flagged: List<ItemBargains>): BargainNotificationContent? {
        if (flagged.isEmpty()) return null
        val count = flagged.size
        val title = if (count == 1) "1 bargain today" else "$count bargains today"
        // Body: up to three item names, then "and N more".
        val names = flagged.map { it.itemName }
        val shown = names.take(3)
        val body = buildString {
            append(shown.joinToString(", "))
            val remaining = names.size - shown.size
            if (remaining > 0) append(" and $remaining more")
        }
        return BargainNotificationContent(title, body)
    }
}
