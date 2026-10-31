package com.example.tracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** A slot joined with the medicine fields the schedule needs. */
data class SlotScheduleRow(
    val slotId: Long,
    val medicineId: Long,
    val patientId: Long,
    val dayOfWeek: Int,
    val minuteOfDay: Int,
    val validFromEpochDay: Long,
    val validFromMinuteOfDay: Int,
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val active: Boolean,
)

/** A dose with the names needed to show or announce it. */
data class DoseRow(
    val doseId: Long,
    val medicineId: Long,
    val patientId: Long,
    val medicineName: String,
    val scheduledEpochDay: Long,
    val scheduledMinuteOfDay: Int,
    val status: String,
    val pointsAwarded: Int,
    val notifiedAtMillis: Long?,
    val remindersSent: Int,
    val reminderIntervalMinutes: Int,
)

@Dao
interface PatientDao {
    @Insert
    suspend fun insert(patient: PatientEntity): Long

    @Update
    suspend fun update(patient: PatientEntity)

    @Query("DELETE FROM patient WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT * FROM patient WHERE id = :id")
    suspend fun get(id: Long): PatientEntity?

    @Query("SELECT * FROM patient ORDER BY createdAtEpochDay, id")
    suspend fun all(): List<PatientEntity>

    @Query("SELECT * FROM patient ORDER BY createdAtEpochDay, id")
    fun observeAll(): Flow<List<PatientEntity>>
}

@Dao
interface MedicineDao {
    @Insert
    suspend fun insert(medicine: MedicineEntity): Long

    @Update
    suspend fun update(medicine: MedicineEntity)

    @Query("DELETE FROM medicine WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("SELECT * FROM medicine WHERE id = :id")
    suspend fun get(id: Long): MedicineEntity?

    @Query("SELECT * FROM medicine ORDER BY patientId, name COLLATE NOCASE, id")
    suspend fun all(): List<MedicineEntity>

    @Query("SELECT * FROM medicine ORDER BY patientId, name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicine WHERE patientId = :patientId ORDER BY name COLLATE NOCASE, id")
    suspend fun forPatient(patientId: Long): List<MedicineEntity>

    @Insert
    suspend fun insertSlot(slot: MedicineSlotEntity): Long

    @Query("DELETE FROM medicine_slot WHERE id IN (:slotIds)")
    suspend fun deleteSlots(slotIds: List<Long>): Int

    @Query("SELECT * FROM medicine_slot WHERE medicineId = :medicineId ORDER BY dayOfWeek, minuteOfDay")
    suspend fun slotsFor(medicineId: Long): List<MedicineSlotEntity>

    @Query("SELECT * FROM medicine_slot ORDER BY medicineId, dayOfWeek, minuteOfDay")
    fun observeAllSlots(): Flow<List<MedicineSlotEntity>>

    @Query(
        """
        SELECT s.id AS slotId, s.medicineId AS medicineId, m.patientId AS patientId,
               s.dayOfWeek AS dayOfWeek, s.minuteOfDay AS minuteOfDay,
               s.validFromEpochDay AS validFromEpochDay, s.validFromMinuteOfDay AS validFromMinuteOfDay,
               m.startEpochDay AS startEpochDay, m.endEpochDay AS endEpochDay, m.active AS active
        FROM medicine_slot s JOIN medicine m ON m.id = s.medicineId
        ORDER BY s.id
        """
    )
    suspend fun allSlotSchedules(): List<SlotScheduleRow>

    @Query(
        """
        SELECT s.id AS slotId, s.medicineId AS medicineId, m.patientId AS patientId,
               s.dayOfWeek AS dayOfWeek, s.minuteOfDay AS minuteOfDay,
               s.validFromEpochDay AS validFromEpochDay, s.validFromMinuteOfDay AS validFromMinuteOfDay,
               m.startEpochDay AS startEpochDay, m.endEpochDay AS endEpochDay, m.active AS active
        FROM medicine_slot s JOIN medicine m ON m.id = s.medicineId
        WHERE m.patientId = :patientId
        ORDER BY s.id
        """
    )
    suspend fun slotSchedulesForPatient(patientId: Long): List<SlotScheduleRow>

    @Query(
        """
        SELECT s.id AS slotId, s.medicineId AS medicineId, m.patientId AS patientId,
               s.dayOfWeek AS dayOfWeek, s.minuteOfDay AS minuteOfDay,
               s.validFromEpochDay AS validFromEpochDay, s.validFromMinuteOfDay AS validFromMinuteOfDay,
               m.startEpochDay AS startEpochDay, m.endEpochDay AS endEpochDay, m.active AS active
        FROM medicine_slot s JOIN medicine m ON m.id = s.medicineId
        WHERE s.id = :slotId
        """
    )
    suspend fun slotSchedule(slotId: Long): SlotScheduleRow?

    @Query("SELECT s.id FROM medicine_slot s JOIN medicine m ON m.id = s.medicineId WHERE m.patientId = :patientId")
    suspend fun slotIdsForPatient(patientId: Long): List<Long>

    @Query("SELECT id FROM medicine_slot WHERE medicineId = :medicineId")
    suspend fun slotIdsForMedicine(medicineId: Long): List<Long>
}

@Dao
interface DoseDao {
    /** Returns -1 when the (medicine, day, minute) dose already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(dose: DoseEventEntity): Long

    @Update
    suspend fun update(dose: DoseEventEntity)

    @Query("SELECT * FROM dose_event WHERE id = :id")
    suspend fun get(id: Long): DoseEventEntity?

    @Query("SELECT * FROM dose_event ORDER BY id")
    suspend fun all(): List<DoseEventEntity>

    @Query(
        """
        SELECT d.status FROM dose_event d JOIN medicine m ON m.id = d.medicineId
        WHERE m.patientId = :patientId AND d.scheduledEpochDay = :epochDay
        """
    )
    suspend fun statusesFor(patientId: Long, epochDay: Long): List<String>

    @Query(
        """
        UPDATE dose_event SET status = 'MISSED'
        WHERE status = 'PENDING' AND scheduledEpochDay = :epochDay
          AND medicineId IN (SELECT id FROM medicine WHERE patientId = :patientId)
        """
    )
    suspend fun markMissed(patientId: Long, epochDay: Long): Int

    @Query(
        """
        SELECT id FROM dose_event
        WHERE status = 'PENDING' AND scheduledEpochDay = :epochDay AND slotId IN (:slotIds)
        """
    )
    suspend fun pendingIdsForSlots(slotIds: List<Long>, epochDay: Long): List<Long>

    @Query("SELECT id FROM dose_event WHERE status = 'PENDING' AND scheduledEpochDay = :epochDay AND medicineId = :medicineId")
    suspend fun pendingIdsForMedicine(medicineId: Long, epochDay: Long): List<Long>

    @Query(
        """
        SELECT d.id FROM dose_event d JOIN medicine m ON m.id = d.medicineId
        WHERE d.status = 'PENDING' AND d.scheduledEpochDay = :epochDay AND m.patientId = :patientId
        """
    )
    suspend fun pendingIdsForPatient(patientId: Long, epochDay: Long): List<Long>

    @Query("DELETE FROM dose_event WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query(
        """
        SELECT d.id AS doseId, d.medicineId AS medicineId, m.patientId AS patientId,
               m.name AS medicineName, d.scheduledEpochDay AS scheduledEpochDay,
               d.scheduledMinuteOfDay AS scheduledMinuteOfDay, d.status AS status,
               d.pointsAwarded AS pointsAwarded, d.notifiedAtMillis AS notifiedAtMillis,
               d.remindersSent AS remindersSent, m.reminderIntervalMinutes AS reminderIntervalMinutes
        FROM dose_event d JOIN medicine m ON m.id = d.medicineId
        WHERE d.scheduledEpochDay = :epochDay
        ORDER BY d.scheduledMinuteOfDay, m.name COLLATE NOCASE, d.id
        """
    )
    fun observeDay(epochDay: Long): Flow<List<DoseRow>>

    @Query(
        """
        SELECT d.id AS doseId, d.medicineId AS medicineId, m.patientId AS patientId,
               m.name AS medicineName, d.scheduledEpochDay AS scheduledEpochDay,
               d.scheduledMinuteOfDay AS scheduledMinuteOfDay, d.status AS status,
               d.pointsAwarded AS pointsAwarded, d.notifiedAtMillis AS notifiedAtMillis,
               d.remindersSent AS remindersSent, m.reminderIntervalMinutes AS reminderIntervalMinutes
        FROM dose_event d JOIN medicine m ON m.id = d.medicineId
        WHERE d.scheduledEpochDay = :epochDay
        ORDER BY d.scheduledMinuteOfDay, m.name COLLATE NOCASE, d.id
        """
    )
    suspend fun day(epochDay: Long): List<DoseRow>

    @Query(
        """
        SELECT d.id AS doseId, d.medicineId AS medicineId, m.patientId AS patientId,
               m.name AS medicineName, d.scheduledEpochDay AS scheduledEpochDay,
               d.scheduledMinuteOfDay AS scheduledMinuteOfDay, d.status AS status,
               d.pointsAwarded AS pointsAwarded, d.notifiedAtMillis AS notifiedAtMillis,
               d.remindersSent AS remindersSent, m.reminderIntervalMinutes AS reminderIntervalMinutes
        FROM dose_event d JOIN medicine m ON m.id = d.medicineId
        WHERE d.id = :doseId
        """
    )
    suspend fun row(doseId: Long): DoseRow?
}

@Dao
interface RewardsDao {
    @Query("SELECT * FROM rewards WHERE patientId = :patientId")
    suspend fun get(patientId: Long): RewardsEntity?

    @Upsert
    suspend fun upsert(rewards: RewardsEntity)

    @Query("SELECT * FROM rewards")
    fun observeAll(): Flow<List<RewardsEntity>>
}

@Dao
interface AwardDao {
    /** -1 when this dose was already paid for. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoseAward(award: DoseAwardEntity): Long

    /** -1 when this day's bonus was already paid. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPerfectDay(award: PerfectDayAwardEntity): Long

    /** -1 when this milestone was already paid for this streak run. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMilestone(award: StreakMilestoneAwardEntity): Long

    @Query("SELECT COUNT(*) FROM dose_award WHERE patientId = :patientId")
    suspend fun doseAwardCount(patientId: Long): Int

    @Query("SELECT COUNT(*) FROM perfect_day_award WHERE patientId = :patientId")
    suspend fun perfectDayCount(patientId: Long): Int

    @Query("SELECT * FROM streak_milestone_award WHERE patientId = :patientId ORDER BY streakStartEpochDay, milestone")
    suspend fun milestones(patientId: Long): List<StreakMilestoneAwardEntity>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<SettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(settings: SettingsEntity): Long

    @Update
    suspend fun update(settings: SettingsEntity)
}
