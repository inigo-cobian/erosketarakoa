package com.erosketarakoa.app.data.local

import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies MIGRATION_2_3 preserves existing item data and defaults the new icon column to the
 * amphora. Builds a v2-shaped "items" table by hand (no exported schema needed) since the only
 * change under test is the added column.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dbName = "migration-test.db"

    @After
    fun cleanup() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate2To3_addsIconColumn_andKeepsData() = runBlocking {
        // --- Create a v2 database with the pre-icon items schema and seed a row. ---
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE items (
                                id TEXT NOT NULL PRIMARY KEY,
                                listId TEXT NOT NULL,
                                name TEXT NOT NULL,
                                quantity INTEGER NOT NULL,
                                category TEXT,
                                notes TEXT,
                                bought INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                isDeleted INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL("CREATE INDEX index_items_listId ON items(listId)")
                        db.execSQL(
                            """
                            CREATE TABLE lists (
                                id TEXT NOT NULL PRIMARY KEY,
                                name TEXT NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                isDeleted INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build(),
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO items VALUES ('i1','l1','Milk',2,'Dairy','Semi',0,10,0)",
        )
        helper.close()

        // --- Open with Room applying the real migrations up to the current version. ---
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()
        val item = db.itemDao().getById("i1")!!
        assertEquals("Milk", item.name)
        assertEquals(2, item.quantity)
        assertEquals("Dairy", item.category)
        assertEquals("1F3FA", item.icon)
        db.close()
    }

    @Test
    fun migrate3To4_addsPositionColumn_andSeedsOrderPerList() = runBlocking {
        // --- Create a v3 database (items has icon but no position) and seed rows. ---
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE items (
                                id TEXT NOT NULL PRIMARY KEY,
                                listId TEXT NOT NULL,
                                name TEXT NOT NULL,
                                quantity INTEGER NOT NULL,
                                category TEXT,
                                notes TEXT,
                                bought INTEGER NOT NULL,
                                icon TEXT NOT NULL DEFAULT '1F3FA',
                                updatedAt INTEGER NOT NULL,
                                isDeleted INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                        db.execSQL("CREATE INDEX index_items_listId ON items(listId)")
                        db.execSQL(
                            """
                            CREATE TABLE lists (
                                id TEXT NOT NULL PRIMARY KEY,
                                name TEXT NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                isDeleted INTEGER NOT NULL
                            )
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: androidx.sqlite.db.SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                })
                .build(),
        )
        // Unbought before bought; alphabetical within each group -> expected order: Apple, Milk, Bread.
        helper.writableDatabase.execSQL(
            "INSERT INTO items VALUES ('a','l1','Milk',1,null,null,0,'1F3FA',10,0)",
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO items VALUES ('b','l1','Apple',1,null,null,0,'1F3FA',10,0)",
        )
        helper.writableDatabase.execSQL(
            "INSERT INTO items VALUES ('c','l1','Bread',1,null,null,1,'1F3FA',10,0)",
        )
        helper.close()

        // --- Open with the real migration and check seeded positions. ---
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()
        val ordered = db.itemDao().observeActiveItems("l1").first()
        assertEquals(listOf("Apple", "Milk", "Bread"), ordered.map { it.name })
        assertEquals(listOf(0, 1, 2), ordered.map { it.position })
        db.close()
    }
}
