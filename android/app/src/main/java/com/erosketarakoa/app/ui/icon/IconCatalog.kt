package com.erosketarakoa.app.ui.icon

import android.content.Context
import org.json.JSONObject

/** One bundled OpenMoji icon: its hexcode (asset filename stem) and searchable keywords. */
data class IconEntry(val hexcode: String, val keywords: String)

const val DEFAULT_ICON = "1F3FA" // amphora

/**
 * The set of icons actually bundled in assets/openmoji/, with keywords loaded from
 * assets/openmoji_keywords.json. Entries are derived from the keyword file (which is generated
 * from the asset list), so the catalog can't drift from the shipped SVGs.
 *
 * Manual keyword additions: pass [overrides] mapping hexcode -> extra space-separated keywords.
 */
class IconCatalog private constructor(private val entries: List<IconEntry>) {

    fun all(): List<IconEntry> = entries

    /** Case-insensitive match over keywords and hexcode. Blank query returns everything. */
    fun filter(query: String): List<IconEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return entries
        return entries.filter { it.hexcode.lowercase().contains(q) || it.keywords.contains(q) }
    }

    companion object {
        fun fromAssets(
            context: Context,
            overrides: Map<String, String> = emptyMap(),
        ): IconCatalog {
            val json = context.assets.open("openmoji_keywords.json")
                .bufferedReader().use { it.readText() }
            val obj = JSONObject(json)
            val entries = obj.keys().asSequence().map { hex ->
                val base = obj.optString(hex)
                val extra = overrides[hex]
                val keywords = if (extra.isNullOrBlank()) base else "$base $extra".trim()
                IconEntry(hexcode = hex, keywords = keywords.lowercase())
            }.sortedBy { it.hexcode }.toList()
            return IconCatalog(entries)
        }

        /** For unit tests: build directly from a hexcode -> keywords map. */
        fun fromMap(map: Map<String, String>): IconCatalog =
            IconCatalog(map.entries.map { IconEntry(it.key, it.value.lowercase()) }.sortedBy { it.hexcode })
    }
}
