package com.erosketarakoa.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ListEntity::class, ItemEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
    abstract fun itemDao(): ItemDao

    companion object {
        const val NAME = "erosketarako.db"
    }
}
