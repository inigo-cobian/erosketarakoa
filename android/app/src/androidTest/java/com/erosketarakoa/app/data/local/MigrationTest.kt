package com.erosketarakoa.app.data.local

import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

        // --- Open with Room v3 applying the real migration. ---
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()
        val item = db.itemDao().getById("i1")!!
        assertEquals("Milk", item.name)
        assertEquals(2, item.quantity)
        assertEquals("Dairy", item.category)
        assertEquals("1F3FA", item.icon)
        db.close()
    }
}
