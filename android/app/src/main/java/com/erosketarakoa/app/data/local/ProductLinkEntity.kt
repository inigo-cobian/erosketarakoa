package com.erosketarakoa.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Links an [ItemEntity] to a store (a name from SUPERMARKET_OPTIONS) and an external product
 * reference. `externalProductId` stays null until a backend source populates it (Phase 4).
 */
@Entity(
    tableName = "product_links",
    indices = [Index("itemId")],
)
data class ProductLinkEntity(
    @PrimaryKey
    val id: String,
    val itemId: String,
    /** Store name from [SUPERMARKET_OPTIONS]. A store is a String, not a table (by design). */
    val store: String,
    /** Backend/source product id; null until Phase 4 populates it. */
    val externalProductId: String? = null,
    val ean: String? = null,
    val productName: String? = null,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
