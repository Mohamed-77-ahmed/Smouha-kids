package com.example.tracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change needs an explicit Migration here (CLAUDE.md 5b). Never destructive: APKs are
 * updated through Drive and a wipe would silently erase every child's streak.
 */
object Migrations {

    /** v2: alarm sound option in settings, on for everyone who updates. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `alarmSoundEnabled` INTEGER NOT NULL DEFAULT 1")
        }
    }

    /** v3: patient gender, nullable; existing patients stay NULL (not set). */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `patient` ADD COLUMN `gender` TEXT")
        }
    }

    /** v4: the celebration sound option in settings, on for everyone who updates (batch 6, approved by the user). */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `celebrationSoundEnabled` INTEGER NOT NULL DEFAULT 1")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
