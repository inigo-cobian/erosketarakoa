package com.erosketarakoa.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {

    /** Active (not soft-deleted) lists, newest updates first, reactive. */
    @Query("SELECT * FROM lists WHERE isDeleted = 0 ORDER BY position ASC")
    fun observeActiveLists(): Flow<List<ListEntity>>

    @Query("SELECT MAX(position) FROM lists WHERE isDeleted = 0")
    suspend fun maxPosition(): Int?

    @Query("UPDATE lists SET position = :position, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPosition(id: String, position: Int, updatedAt: Long)

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observeList(id: String): Flow<ListEntity?>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun getById(id: String): ListEntity?

    @Query("SELECT * FROM lists")
    suspend fun getAll(): List<ListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ListEntity)

    @Upsert
    suspend fun upsert(list: ListEntity)

    @Upsert
    suspend fun upsertAll(lists: List<ListEntity>)

    @Query("UPDATE lists SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, name: String, updatedAt: Long)

    @Query("UPDATE lists SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("DELETE FROM lists")
    suspend fun clear()
}
