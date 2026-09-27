package com.erosketarakoa.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ListEntity::class, ItemEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
    abstract fun itemDao(): ItemDao

    companion object {
        const val NAME = "erosketarako.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN icon TEXT NOT NULL DEFAULT '1F3FA'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                // Seed positions per list from the previous default sort so existing lists keep order.
                db.execSQL(
                    """
                    UPDATE items SET position = (
                        SELECT COUNT(*) FROM items AS o
                        WHERE o.listId = items.listId
                        AND (o.bought < items.bought
                             OR (o.bought = items.bought AND o.name COLLATE NOCASE < items.name COLLATE NOCASE)
                             OR (o.bought = items.bought AND o.name COLLATE NOCASE = items.name COLLATE NOCASE AND o.id < items.id))
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN unit TEXT")
                db.execSQL("ALTER TABLE items ADD COLUMN supermarkets TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lists ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                // Seed positions from the previous alphabetical sort so existing lists keep order.
                db.execSQL(
                    """
                    UPDATE lists SET position = (
                        SELECT COUNT(*) FROM lists AS o
                        WHERE o.name COLLATE NOCASE < lists.name COLLATE NOCASE
                           OR (o.name COLLATE NOCASE = lists.name COLLATE NOCASE AND o.id < lists.id)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
