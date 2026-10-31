package com.example.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PatientEntity::class,
        MedicineEntity::class,
        MedicineSlotEntity::class,
        DoseEventEntity::class,
        RewardsEntity::class,
        DoseAwardEntity::class,
        PerfectDayAwardEntity::class,
        StreakMilestoneAwardEntity::class,
        SettingsEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patients(): PatientDao
    abstract fun medicines(): MedicineDao
    abstract fun doses(): DoseDao
    abstract fun rewards(): RewardsDao
    abstract fun awards(): AwardDao
    abstract fun settings(): SettingsDao

    companion object {
        const val NAME = "pill_pals.db"

        /**
         * Never add fallbackToDestructiveMigration(): APKs are updated through Drive and a
         * destructive migration would silently wipe every child's streak. Version 2 onward
         * needs an explicit Migration.
         */
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                .build()

        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, AppDatabase::class.java).build()
    }
}
