package com.erosketarakoa.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductLinkDao {

    /** Active (not soft-deleted) links for an item, reactive. */
    @Query("SELECT * FROM product_links WHERE itemId = :itemId AND isDeleted = 0 ORDER BY store ASC")
    fun observeByItem(itemId: String): Flow<List<ProductLinkEntity>>

    @Query("SELECT * FROM product_links WHERE itemId = :itemId AND isDeleted = 0 ORDER BY store ASC")
    suspend fun getByItem(itemId: String): List<ProductLinkEntity>

    @Query("SELECT * FROM product_links WHERE isDeleted = 0")
    suspend fun getAllActive(): List<ProductLinkEntity>

    @Query("SELECT * FROM product_links WHERE id = :id")
    suspend fun getById(id: String): ProductLinkEntity?

    @Upsert
    suspend fun upsert(link: ProductLinkEntity)

    @Upsert
    suspend fun upsertAll(links: List<ProductLinkEntity>)

    @Query("UPDATE product_links SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM product_links")
    suspend fun clear()
}
