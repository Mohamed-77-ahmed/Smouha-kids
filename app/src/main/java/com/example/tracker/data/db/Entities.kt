package com.example.tracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Schema v4 (v2: settings.alarmSoundEnabled, v3: patient.gender, v4: settings.celebrationSoundEnabled). Dates are local epoch days,
 * times are local minutes of day, instants are epoch millis. NULL always means "not set".
 */

@Entity(tableName = "patient")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Age the parent entered, valid on [createdAtEpochDay]. Re-anchored when the age is edited. */
    val ageAtCreation: Int,
    val createdAtEpochDay: Long,
    /** "MALE" or "FEMALE" (Gender), for the avatar only. NULL = not set (patients from before v3). */
    val gender: String? = null,
)

@Entity(
    tableName = "medicine",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("patientId")],
)
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val name: String,
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val active: Boolean,
    /** Minutes between follow-up reminders: 5, 10, 15 or 30. */
    val reminderIntervalMinutes: Int,
)

@Entity(
    tableName = "medicine_slot",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["medicineId", "dayOfWeek", "minuteOfDay"], unique = true)],
)
data class MedicineSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    /** ISO: 1 = Monday ... 7 = Sunday. */
    val dayOfWeek: Int,
    val minuteOfDay: Int,
    /** When the slot was added. No dose is created for an earlier time. */
    val validFromEpochDay: Long,
    val validFromMinuteOfDay: Int,
)

@Entity(
    tableName = "dose_event",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        // A removed slot keeps its past doses (history never changes).
        ForeignKey(
            entity = MedicineSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["medicineId", "scheduledEpochDay", "scheduledMinuteOfDay"], unique = true),
        Index("slotId"),
        Index("scheduledEpochDay"),
    ],
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val slotId: Long?,
    val scheduledEpochDay: Long,
    val scheduledMinuteOfDay: Int,
    /** PENDING, TAKEN or MISSED. */
    val status: String,
    val confirmedAtMillis: Long?,
    val pointsAwarded: Int,
    /** When the first reminder notification was posted, null if never. */
    val notifiedAtMillis: Long?,
    /** Follow-up reminders already posted (0..3). */
    val remindersSent: Int,
)

@Entity(
    tableName = "rewards",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RewardsEntity(
    @PrimaryKey val patientId: Long,
    val totalPoints: Int,
    /** Perfect days in a row up to [lastEvaluatedEpochDay]. Today is added live when complete. */
    val currentStreak: Int,
    val streakStartEpochDay: Long?,
    val bestStreak: Int,
    val lastPerfectDay: Long?,
    /** Last day whose result is final (MISSED doses marked, streak updated). */
    val lastEvaluatedEpochDay: Long,
    /** Length of a broken streak whose animation is still to be shown, null when acknowledged. */
    val brokenStreakPending: Int?,
)

/** Points ledger that outlives medicines, so deleting and re-adding one cannot farm points. */
@Entity(
    tableName = "dose_award",
    primaryKeys = ["patientId", "scheduledEpochDay", "scheduledMinuteOfDay", "medicineKey"],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DoseAwardEntity(
    val patientId: Long,
    val scheduledEpochDay: Long,
    val scheduledMinuteOfDay: Int,
    val medicineKey: String,
    val points: Int,
    val awardedAtMillis: Long,
)

/** One +20 perfect-day bonus per patient per day. */
@Entity(
    tableName = "perfect_day_award",
    primaryKeys = ["patientId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PerfectDayAwardEntity(
    val patientId: Long,
    val epochDay: Long,
    val awardedAtMillis: Long,
)

/** One +50 bonus per milestone per streak run (a run is identified by its first day). */
@Entity(
    tableName = "streak_milestone_award",
    primaryKeys = ["patientId", "streakStartEpochDay", "milestone"],
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StreakMilestoneAwardEntity(
    val patientId: Long,
    val streakStartEpochDay: Long,
    val milestone: Int,
    val awardedAtMillis: Long,
)

@Entity(
    tableName = "app_settings",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["selectedPatientId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("selectedPatientId")],
)
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    /** "ar" or "en", null until chosen on first launch. */
    val languageTag: String?,
    val pinHash: String?,
    val pinSalt: String?,
    val pinFailedAttempts: Int,
    val pinLockedUntilMillis: Long?,
    val requirePinForDose: Boolean,
    /** Home filter. Null = "All". Falls back to null when the patient is deleted. */
    val selectedPatientId: Long?,
    val notificationRationaleShown: Boolean,
    /** Reminders ring on the alarm stream (silent/vibrate does not mute them). Added in v2. */
    @ColumnInfo(defaultValue = "1") val alarmSoundEnabled: Boolean = true,
    /** The short sound of a CHILD or TEEN reward (media volume). Added in v4, on by default. */
    @ColumnInfo(defaultValue = "1") val celebrationSoundEnabled: Boolean = true,
) {
    companion object {
        const val SINGLETON_ID = 1

        fun defaults() = SettingsEntity(
            languageTag = null,
            pinHash = null,
            pinSalt = null,
            pinFailedAttempts = 0,
            pinLockedUntilMillis = null,
            requirePinForDose = true,
            selectedPatientId = null,
            notificationRationaleShown = false,
            alarmSoundEnabled = true,
            celebrationSoundEnabled = true,
        )
    }
}
