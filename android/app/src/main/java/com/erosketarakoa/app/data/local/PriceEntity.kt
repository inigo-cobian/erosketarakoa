package com.erosketarakoa.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An observed price for a [ProductLinkEntity]. Money is stored as integer cents, never float.
 */
@Entity(
    tableName = "prices",
    indices = [Index("linkId")],
)
data class PriceEntity(
    @PrimaryKey
    val id: String,
    val linkId: String,
    /** Price in integer cents (e.g. 199 = 1.99 EUR). */
    val priceCents: Long,
    val currency: String = "EUR",
    val observedAt: Long,
    val source: String,
    val updatedAt: Long = 0,
)
