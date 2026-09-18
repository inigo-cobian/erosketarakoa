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
    val category: String? = null,
    val notes: String? = null,
    val bought: Boolean = false,
    /** OpenMoji hexcode (filename stem), e.g. "1F345". Required; defaults to amphora. */
    val icon: String = "1F3FA",
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
