package com.erosketarakoa.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A shopping item stored locally. Room is the single source of truth.
 * See [ListEntity] for the meaning of updatedAt / isDeleted.
 */
@Entity(
    tableName = "items",
    indices = [Index("listId")],
)
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val listId: String,
    val name: String,
    val quantity: Int = 1,
    /** Free-text unit, e.g. "kg", "bags". Null = unitless count. */
    val unit: String? = null,
    /** Selected supermarkets, comma-joined (e.g. "Eroski,Lidl"). Empty = none. */
    val supermarkets: String = "",
    val category: String? = null,
    val notes: String? = null,
    val bought: Boolean = false,
    /** OpenMoji hexcode (filename stem), e.g. "1F345". Required; defaults to amphora. */
    val icon: String = "1F3FA",
    /** Manual sort order within the list; lower shows first. */
    val position: Int = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
) {
    /** Parsed supermarket tags. */
    val supermarketList: List<String>
        get() = supermarkets.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

/** Supermarket options offered in the item editor. Populate more later. */
val SUPERMARKET_OPTIONS = listOf("Eroski", "Lidl", "Dia")
