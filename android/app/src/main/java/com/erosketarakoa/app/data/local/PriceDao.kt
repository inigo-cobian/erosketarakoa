package com.erosketarakoa.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceDao {

    /** All prices for a link, oldest first (history order). Reactive. */
    @Query("SELECT * FROM prices WHERE linkId = :linkId ORDER BY observedAt ASC")
    fun observeByLink(linkId: String): Flow<List<PriceEntity>>

    @Query("SELECT * FROM prices WHERE linkId = :linkId ORDER BY observedAt ASC")
    suspend fun getByLink(linkId: String): List<PriceEntity>

    /** Prices for every link of an item, oldest first. */
    @Query(
        """
        SELECT prices.* FROM prices
        JOIN product_links ON prices.linkId = product_links.id
        WHERE product_links.itemId = :itemId
        ORDER BY prices.observedAt ASC
        """,
    )
    fun observeByItem(itemId: String): Flow<List<PriceEntity>>

    @Query("SELECT * FROM prices WHERE id = :id")
    suspend fun getById(id: String): PriceEntity?

    /** De-dupe helper: does an identical observation already exist for this link? */
    @Query("SELECT COUNT(*) FROM prices WHERE linkId = :linkId AND observedAt = :observedAt AND priceCents = :priceCents")
    suspend fun countMatching(linkId: String, observedAt: Long, priceCents: Long): Int

    @Upsert
    suspend fun upsert(price: PriceEntity)

    @Upsert
    suspend fun upsertAll(prices: List<PriceEntity>)

    @Query("DELETE FROM prices WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM prices")
    suspend fun clear()
}
