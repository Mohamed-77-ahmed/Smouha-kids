package com.example.tracker

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.data.db.Migrations
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real migrations on realistic v1, v2 and v3 databases (CLAUDE.md 5b): every row of every table
 * survives unchanged, new columns get their documented values. Never destructive.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    /** v1 columns per table: the data that must come through every migration untouched. */
    private val v1Columns = mapOf(
        "patient" to "id, name, ageAtCreation, createdAtEpochDay",
        "medicine" to "id, patientId, name, startEpochDay, endEpochDay, active, reminderIntervalMinutes",
        "medicine_slot" to "id, medicineId, dayOfWeek, minuteOfDay, validFromEpochDay, validFromMinuteOfDay",
        "dose_event" to "id, medicineId, slotId, scheduledEpochDay, scheduledMinuteOfDay, status, confirmedAtMillis, pointsAwarded, notifiedAtMillis, remindersSent",
        "rewards" to "patientId, totalPoints, currentStreak, streakStartEpochDay, bestStreak, lastPerfectDay, lastEvaluatedEpochDay, brokenStreakPending",
        "dose_award" to "patientId, scheduledEpochDay, scheduledMinuteOfDay, medicineKey, points, awardedAtMillis",
        "perfect_day_award" to "patientId, epochDay, awardedAtMillis",
        "streak_milestone_award" to "patientId, streakStartEpochDay, milestone, awardedAtMillis",
        "app_settings" to "id, languageTag, pinHash, pinSalt, pinFailedAttempts, pinLockedUntilMillis, requirePinForDose, selectedPatientId, notificationRationaleShown",
    )

    /** Two patients (one Arabic name), medicines with and without dates, slots, doses in every state, rewards, awards, settings. */
    private fun fillV1(db: SupportSQLiteDatabase) {
        listOf(
            // Named columns: the same rows go into a v1, v2 or v3 table (v3 added gender, NULL here).
            "INSERT INTO patient (${v1Columns.getValue("patient")}) VALUES (1, 'Yasmin', 7, 20370), (2, 'ياسمين', 40, 20372)",
            "INSERT INTO medicine VALUES (1, 1, 'Panadol', NULL, NULL, 1, 10), (2, 2, 'Augmentin 1g', 20370, 20376, 1, 5)",
            "INSERT INTO medicine_slot VALUES (1, 1, 1, 480, 20370, 420), (2, 1, 3, 1260, 20370, 420), (3, 2, 6, 540, 20372, 600)",
            "INSERT INTO dose_event VALUES (1, 1, 1, 20370, 480, 'TAKEN', 1760000000000, 15, 1759990000000, 0), " +
                "(2, 1, 2, 20372, 1260, 'MISSED', NULL, 0, 1760100000000, 3), (3, 2, NULL, 20373, 540, 'PENDING', NULL, 0, NULL, 0)",
            "INSERT INTO rewards VALUES (1, 135, 0, NULL, 3, 20370, 20372, 3), (2, 0, 0, NULL, 0, NULL, 20372, NULL)",
            "INSERT INTO dose_award VALUES (1, 20370, 480, 'panadol', 15, 1760000000000)",
            "INSERT INTO perfect_day_award VALUES (1, 20370, 1760000000000)",
            "INSERT INTO streak_milestone_award VALUES (1, 20368, 3, 1760000000000)",
            "INSERT INTO app_settings (${v1Columns.getValue("app_settings")}) VALUES (1, 'ar', 'pbkdf2-sha256:120000:abc', 'c2FsdA==', 2, 1760000060000, 0, 2, 1)",
        ).forEach { db.execSQL(it) }
    }

    private fun dump(db: SupportSQLiteDatabase): Map<String, List<String>> = v1Columns.mapValues { (table, cols) ->
        db.query("SELECT $cols FROM `$table` ORDER BY 1, 2").use { c ->
            buildList {
                while (c.moveToNext()) add((0 until c.columnCount).joinToString("|") { if (c.isNull(it)) "NULL" else c.getString(it) })
            }
        }
    }

    @Test
    fun v1ToLatestKeepsEveryRowAndTurnsAlarmSoundOn() {
        val before = helper.createDatabase(DB, 1).use { db ->
            fillV1(db)
            dump(db)
        }
        assertEquals(listOf(2, 2, 3, 3, 2, 1, 1, 1, 1), before.values.map { it.size })

        helper.runMigrationsAndValidate(DB, LATEST, true, *Migrations.ALL).use { db ->
            assertEquals(before, dump(db))
            assertEquals(1, setting(db, "alarmSoundEnabled"))
            assertEquals(1, setting(db, "celebrationSoundEnabled"))
            assertEquals(listOf("NULL", "NULL"), genders(db))
        }
    }

    /** v3 to v4 (batch 6): the celebration sound turns on for everyone, nothing else moves. */
    @Test
    fun v3ToV4KeepsEveryRowAndTurnsCelebrationSoundOn() {
        val before = helper.createDatabase(DB3, 3).use { db ->
            fillV1(db)
            db.execSQL("UPDATE app_settings SET alarmSoundEnabled = 0")
            db.execSQL("UPDATE patient SET gender = 'FEMALE' WHERE id = 1")
            dump(db)
        }
        helper.runMigrationsAndValidate(DB3, 4, true, Migrations.MIGRATION_3_4).use { db ->
            assertEquals(before, dump(db))
            assertEquals("the alarm sound choice is kept", 0, setting(db, "alarmSoundEnabled"))
            assertEquals(1, setting(db, "celebrationSoundEnabled"))
            assertEquals(listOf("FEMALE", "NULL"), genders(db))
        }
    }

    private fun setting(db: SupportSQLiteDatabase, column: String): Int =
        db.query("SELECT $column FROM app_settings").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }

    @Test
    fun v2ToV3KeepsEveryRowAndLeavesGenderNotSet() {
        val before = helper.createDatabase(DB2, 2).use { db ->
            fillV1(db)
            db.execSQL("UPDATE app_settings SET alarmSoundEnabled = 0")
            dump(db)
        }
        helper.runMigrationsAndValidate(DB2, 3, true, Migrations.MIGRATION_2_3).use { db ->
            assertEquals(before, dump(db))
            db.query("SELECT alarmSoundEnabled FROM app_settings").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
            assertEquals(listOf("NULL", "NULL"), genders(db))
        }
    }

    private fun genders(db: SupportSQLiteDatabase): List<String> =
        db.query("SELECT gender FROM patient ORDER BY id").use { c ->
            buildList { while (c.moveToNext()) add(if (c.isNull(0)) "NULL" else c.getString(0)) }
        }

    @Test
    fun freshLatestDatabaseMatchesTheExportedSchema() {
        helper.createDatabase(FRESH, LATEST).close()
        helper.runMigrationsAndValidate(FRESH, LATEST, true).close()
    }

    private companion object {
        const val DB = "migration-test.db"
        const val DB2 = "migration-v2-test.db"
        const val DB3 = "migration-v3-test.db"
        const val FRESH = "fresh-test.db"
        const val LATEST = 4
    }
}
