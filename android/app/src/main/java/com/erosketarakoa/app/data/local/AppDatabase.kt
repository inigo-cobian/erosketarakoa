package com.erosketarakoa.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ListEntity::class, ItemEntity::class, ProductLinkEntity::class, PriceEntity::class],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listDao(): ListDao
    abstract fun itemDao(): ItemDao
    abstract fun productLinkDao(): ProductLinkDao
    abstract fun priceDao(): PriceDao

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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lists ADD COLUMN color TEXT NOT NULL DEFAULT 'WHITE'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN targetPriceCents INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN barcode TEXT")
                db.execSQL(
                    """
                    CREATE TABLE product_links (
                        id TEXT NOT NULL PRIMARY KEY,
                        itemId TEXT NOT NULL,
                        store TEXT NOT NULL,
                        externalProductId TEXT,
                        ean TEXT,
                        productName TEXT,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX index_product_links_itemId ON product_links(itemId)")
                db.execSQL(
                    """
                    CREATE TABLE prices (
                        id TEXT NOT NULL PRIMARY KEY,
                        linkId TEXT NOT NULL,
                        priceCents INTEGER NOT NULL,
                        currency TEXT NOT NULL DEFAULT 'EUR',
                        observedAt INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX index_prices_linkId ON prices(linkId)")
            }
        }
    }
}
