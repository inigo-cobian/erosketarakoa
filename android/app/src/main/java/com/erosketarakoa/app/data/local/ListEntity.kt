package com.erosketarakoa.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A shopping list stored locally. Room is the single source of truth.
 *
 *  - updatedAt: epoch millis of the last local edit, used to order edits.
 *  - isDeleted: soft-delete tombstone so a list can be hidden without losing its items.
 */
@Entity(tableName = "lists")
data class ListEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
)
