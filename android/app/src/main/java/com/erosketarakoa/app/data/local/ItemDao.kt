package com.erosketarakoa.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    /** Active (not soft-deleted) items for a list, reactive. */
    @Query("SELECT * FROM items WHERE listId = :listId AND isDeleted = 0 ORDER BY position ASC")
    fun observeActiveItems(listId: String): Flow<List<ItemEntity>>

    @Query("SELECT MAX(position) FROM items WHERE listId = :listId AND isDeleted = 0")
    suspend fun maxPosition(listId: String): Int?

    @Query("UPDATE items SET position = :position, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPosition(id: String, position: Int, updatedAt: Long)

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): ItemEntity?

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)

    @Upsert
    suspend fun upsert(item: ItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)

    @Query(
        """
        UPDATE items SET name = :name, quantity = :quantity, unit = :unit,
        supermarkets = :supermarkets, category = :category,
        notes = :notes, icon = :icon, updatedAt = :updatedAt WHERE id = :id
        """,
    )
    suspend fun updateFields(
        id: String,
        name: String,
        quantity: Int,
        unit: String?,
        supermarkets: String,
        category: String?,
        notes: String?,
        icon: String,
        updatedAt: Long,
    )

    @Query("UPDATE items SET bought = :bought, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setBought(id: String, bought: Boolean, updatedAt: Long)

    @Query("UPDATE items SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("UPDATE items SET isDeleted = 1, updatedAt = :updatedAt WHERE listId = :listId")
    suspend fun softDeleteByList(listId: String, updatedAt: Long)

    @Query("DELETE FROM items")
    suspend fun clear()
}
