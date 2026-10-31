package com.example.tracker

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tracker.data.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The exported v1 schema is still valid; migrations from it are tested in [MigrationTest]. */
@RunWith(AndroidJUnit4::class)
class SchemaTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun exportedVersion1MatchesTheDatabase() {
        helper.createDatabase(DB, 1).use { db ->
            db.query("PRAGMA foreign_keys").use { it.moveToFirst() }
            val tables = db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY name")
                .use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
            assertEquals(
                listOf(
                    "app_settings", "dose_award", "dose_event", "medicine", "medicine_slot",
                    "patient", "perfect_day_award", "rewards", "streak_milestone_award",
                ),
                tables,
            )
        }
        // Validates the on-disk schema against the exported JSON (identity hash and every column).
        helper.runMigrationsAndValidate(DB, 1, true).close()
    }

    private companion object {
        const val DB = "schema-test.db"
    }
}
