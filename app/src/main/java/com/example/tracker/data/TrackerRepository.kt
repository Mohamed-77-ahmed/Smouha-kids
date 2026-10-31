package com.example.tracker.data

import androidx.room.withTransaction
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.data.db.DoseAwardEntity
import com.example.tracker.data.db.DoseEventEntity
import com.example.tracker.data.db.DoseRow
import com.example.tracker.data.db.MedicineEntity
import com.example.tracker.data.db.MedicineSlotEntity
import com.example.tracker.data.db.PatientEntity
import com.example.tracker.data.db.PerfectDayAwardEntity
import com.example.tracker.data.db.RewardsEntity
import com.example.tracker.data.db.SettingsEntity
import com.example.tracker.data.db.SlotScheduleRow
import com.example.tracker.data.db.StreakMilestoneAwardEntity
import com.example.tracker.domain.AgeGroup
import com.example.tracker.domain.Ages
import com.example.tracker.domain.ConfirmCheck
import com.example.tracker.domain.CourseEnd
import com.example.tracker.domain.DayResult
import com.example.tracker.domain.DoseRules
import com.example.tracker.domain.DoseStatus
import com.example.tracker.domain.Gender
import com.example.tracker.domain.LockoutState
import com.example.tracker.domain.MedicineDraft
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.PinHasher
import com.example.tracker.domain.PinLockout
import com.example.tracker.domain.PinRules
import com.example.tracker.domain.PointsRules
import com.example.tracker.domain.ReminderRules
import com.example.tracker.domain.ScheduleRules
import com.example.tracker.domain.Slot
import com.example.tracker.domain.SlotSpec
import com.example.tracker.domain.StreakRules
import com.example.tracker.domain.StreakState
import com.example.tracker.domain.Validation
import com.example.tracker.domain.ValidationError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

/** Alarms and notifications to cancel after something was deleted. */
data class CancelRefs(val slotIds: List<Long>, val doseIds: List<Long>) {
    operator fun plus(other: CancelRefs) = CancelRefs(slotIds + other.slotIds, doseIds + other.doseIds)

    companion object {
        val NONE = CancelRefs(emptyList(), emptyList())
    }
}

data class MedicineInput(
    /** Null when adding. */
    val medicineId: Long?,
    val patient: PatientChoice,
    val name: String?,
    val slots: List<Slot>,
    val daysWithoutTimes: Set<Int> = emptySet(),
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val reminderIntervalMinutes: Int,
)

sealed interface SaveResult {
    data class Saved(val medicineId: Long, val patientId: Long, val removed: CancelRefs) : SaveResult
    data class Invalid(val errors: List<ValidationError>) : SaveResult
}

data class MedicineWithSlots(
    val medicine: MedicineEntity,
    val patient: PatientEntity,
    val slots: List<MedicineSlotEntity>,
)

sealed interface PinCheck {
    data object Ok : PinCheck
    data object NoPin : PinCheck
    data class Wrong(val attemptsLeft: Int) : PinCheck
    data class Locked(val remainingMillis: Long) : PinCheck
}

sealed interface ConfirmOutcome {
    data class Confirmed(
        val patientId: Long,
        val patientName: String,
        val ageGroup: AgeGroup,
        /** For the avatar only; null = not set. */
        val gender: Gender?,
        val medicineName: String,
        /** Points the dose was worth (+10, +5 on time). */
        val dosePoints: Int,
        val onTimeBonus: Int,
        /** False when the ledger already paid this dose (deleted and re-added medicine). */
        val dosePaid: Boolean,
        val perfectDayBonus: Int,
        val milestone: Int?,
        val milestoneBonus: Int,
        val liveStreak: Int,
        val totalPoints: Int,
        val dayComplete: Boolean,
    ) : ConfirmOutcome {
        val earned: Int get() = (if (dosePaid) dosePoints + onTimeBonus else 0) + perfectDayBonus + milestoneBonus
    }

    data class Rejected(val reason: ConfirmCheck?) : ConfirmOutcome
}

/** Data a reminder notification needs. */
data class ReminderInfo(
    val dose: DoseRow,
    val patientName: String,
    val ageGroup: AgeGroup,
    /** True when another follow-up should be scheduled after this one. */
    val moreRepeatsLeft: Boolean,
)

data class HomePatient(
    val patient: PatientEntity,
    val age: Int,
    val ageGroup: AgeGroup,
    val totalPoints: Int,
    val level: Int,
    val pointsToNextLevel: Int,
    val liveStreak: Int,
    val bestStreak: Int,
    val doses: List<DoseRow>,
    val takenToday: Int,
    val todayComplete: Boolean,
    val brokenStreakPending: Int?,
)

data class HomeData(
    val todayEpochDay: Long,
    val patients: List<HomePatient>,
    /** Validated filter: null = All, never a deleted patient. */
    val selectedPatientId: Long?,
    val settings: SettingsEntity,
)

class TrackerRepository(
    private val db: AppDatabase,
    private val clock: Clock,
    private val hasher: PinHasher = PinHasher(),
) {
    private val patientDao = db.patients()
    private val medicineDao = db.medicines()
    private val doseDao = db.doses()
    private val rewardsDao = db.rewards()
    private val awardDao = db.awards()
    private val settingsDao = db.settings()

    fun todayEpochDay(): Long = LocalDate.now(clock).toEpochDay()

    fun nowZoned(): ZonedDateTime = ZonedDateTime.now(clock)

    fun nowMillis(): Long = clock.millis()

    private fun nowLocal(): LocalDateTime = LocalDateTime.now(clock)

    private fun nowMinuteOfDay(): Int = nowLocal().toLocalTime().let { it.hour * 60 + it.minute }

    // ---------------------------------------------------------------- settings

    /** One transaction: an erase on another thread cannot remove the row between the insert and the read. */
    suspend fun settings(): SettingsEntity = db.withTransaction {
        settingsDao.get() ?: run {
            settingsDao.insertIfMissing(SettingsEntity.defaults())
            checkNotNull(settingsDao.get())
        }
    }

    fun observeSettings(): Flow<SettingsEntity> =
        settingsDao.observe().map { it ?: SettingsEntity.defaults() }

    private suspend fun updateSettings(change: (SettingsEntity) -> SettingsEntity) {
        db.withTransaction { settingsDao.update(change(settings())) }
    }

    suspend fun setLanguage(tag: String) = updateSettings { it.copy(languageTag = tag) }

    suspend fun setRequirePinForDose(required: Boolean) =
        updateSettings { it.copy(requirePinForDose = required) }

    /** View-only filter. Never touches doses, alarms, points or streaks. */
    suspend fun setSelectedPatient(patientId: Long?) = db.withTransaction {
        val valid = patientId?.takeIf { patientDao.get(it) != null }
        settingsDao.update(settings().copy(selectedPatientId = valid))
    }

    suspend fun setAlarmSound(enabled: Boolean) = updateSettings { it.copy(alarmSoundEnabled = enabled) }

    suspend fun setCelebrationSound(enabled: Boolean) = updateSettings { it.copy(celebrationSoundEnabled = enabled) }

    suspend fun markNotificationRationaleShown() =
        updateSettings { it.copy(notificationRationaleShown = true) }

    // ---------------------------------------------------------------- PIN

    suspend fun hasPin(): Boolean = settings().pinHash != null

    /** Stores a new salted hash. Used for first creation, change and reset; keeps all other data. */
    suspend fun setPin(pin: String) {
        require(PinRules.isValidFormat(pin)) { "PIN format" }
        val salt = hasher.newSalt()
        val hash = withContext(Dispatchers.Default) { hasher.hash(pin, salt) }
        updateSettings {
            it.copy(pinHash = hash, pinSalt = salt, pinFailedAttempts = 0, pinLockedUntilMillis = null)
        }
    }

    suspend fun pinLockRemainingMillis(): Long {
        val s = settings()
        return PinLockout.remainingMillis(LockoutState(s.pinFailedAttempts, s.pinLockedUntilMillis), clock.millis())
    }

    suspend fun verifyPin(pin: String): PinCheck {
        val s = settings()
        val hash = s.pinHash ?: return PinCheck.NoPin
        val salt = s.pinSalt ?: return PinCheck.NoPin
        val nowMillis = clock.millis()
        val lock = LockoutState(s.pinFailedAttempts, s.pinLockedUntilMillis)
        val remaining = PinLockout.remainingMillis(lock, nowMillis)
        if (remaining > 0) return PinCheck.Locked(remaining)
        val ok = withContext(Dispatchers.Default) { hasher.verify(pin, salt, hash) }
        // The lock starts when the wrong PIN is known, after the slow hash, so it lasts the full 60 s.
        val checkedAt = clock.millis()
        val next = if (ok) PinLockout.onSuccess() else PinLockout.onFailure(lock, checkedAt)
        updateSettings { it.copy(pinFailedAttempts = next.failedAttempts, pinLockedUntilMillis = next.lockedUntilMillis) }
        return when {
            ok -> PinCheck.Ok
            next.lockedUntilMillis != null -> PinCheck.Locked(PinLockout.remainingMillis(next, checkedAt))
            else -> PinCheck.Wrong(PinLockout.attemptsLeft(next))
        }
    }

    // ---------------------------------------------------------------- patients

    fun observePatients(): Flow<List<PatientEntity>> = patientDao.observeAll()

    suspend fun patients(): List<PatientEntity> = patientDao.all()

    suspend fun patient(id: Long): PatientEntity? = patientDao.get(id)

    fun currentAge(patient: PatientEntity): Int =
        Ages.currentAge(patient.ageAtCreation, patient.createdAtEpochDay, todayEpochDay())

    suspend fun addPatient(name: String, age: Int, gender: Gender? = null): Long =
        db.withTransaction { insertPatient(name, age, gender) }

    private suspend fun insertPatient(name: String, age: Int, gender: Gender?): Long {
        val cleanName = requireNotNull(Validation.normalizeName(name)) { "empty name" }
        require(age in Ages.MIN_AGE..Ages.MAX_AGE) { "age $age" }
        val today = todayEpochDay()
        val id = patientDao.insert(
            PatientEntity(name = cleanName, ageAtCreation = age, createdAtEpochDay = today, gender = gender?.name)
        )
        rewardsDao.upsert(
            RewardsEntity(
                patientId = id,
                totalPoints = 0,
                currentStreak = 0,
                streakStartEpochDay = null,
                bestStreak = 0,
                lastPerfectDay = null,
                lastEvaluatedEpochDay = today - 1,
                brokenStreakPending = null,
            )
        )
        return id
    }

    /**
     * Editing the age re-anchors it to today, so it keeps growing from the new value. A null
     * [gender] keeps what is stored (an existing patient may stay "not set").
     */
    suspend fun updatePatient(id: Long, name: String, age: Int, gender: Gender? = null): Boolean = db.withTransaction {
        val p = patientDao.get(id) ?: return@withTransaction false
        val cleanName = Validation.normalizeName(name) ?: return@withTransaction false
        if (age !in Ages.MIN_AGE..Ages.MAX_AGE) return@withTransaction false
        val ageChanged = currentAge(p) != age
        patientDao.update(
            p.copy(
                name = cleanName,
                ageAtCreation = if (ageChanged) age else p.ageAtCreation,
                createdAtEpochDay = if (ageChanged) todayEpochDay() else p.createdAtEpochDay,
                gender = gender?.name ?: p.gender,
            )
        )
        true
    }

    /** Deletes only this patient's medicines, slots, doses, rewards and awards (FK cascade). */
    suspend fun deletePatient(id: Long): CancelRefs = db.withTransaction {
        val refs = CancelRefs(
            slotIds = medicineDao.slotIdsForPatient(id),
            doseIds = doseDao.pendingIdsForPatient(id, todayEpochDay()),
        )
        patientDao.delete(id)
        refs
    }

    /**
     * Parent settings > Erase all data: every row of every table goes (patients, medicines, doses,
     * points, streaks, award ledger, PIN, language), in one transaction. Returns the alarms and
     * notifications to cancel. Ids are never reused afterwards (AUTOINCREMENT), so an alarm that
     * slipped through finds nothing and stays silent.
     */
    suspend fun eraseAllData(): CancelRefs {
        val refs = CancelRefs(
            slotIds = medicineDao.allSlotSchedules().map { it.slotId },
            // Follow-ups only run for today's doses.
            doseIds = doseDao.day(todayEpochDay()).map { it.doseId },
        )
        withContext(Dispatchers.IO) { db.clearAllTables() }
        return refs
    }

    // ---------------------------------------------------------------- medicines

    fun observeMedicines(): Flow<List<MedicineEntity>> = medicineDao.observeAll()

    fun observeSlots(): Flow<List<MedicineSlotEntity>> = medicineDao.observeAllSlots()

    suspend fun medicineWithSlots(id: Long): MedicineWithSlots? {
        val m = medicineDao.get(id) ?: return null
        val p = patientDao.get(m.patientId) ?: return null
        return MedicineWithSlots(m, p, medicineDao.slotsFor(id))
    }

    /**
     * Adds or edits a medicine. Edits affect the future only: removed slots lose today's PENDING
     * dose, TAKEN and past doses stay. The patient of an existing medicine never changes.
     */
    suspend fun saveMedicine(input: MedicineInput): SaveResult {
        val draft = MedicineDraft(
            patient = input.patient,
            name = input.name,
            slots = input.slots,
            daysWithoutTimes = input.daysWithoutTimes,
            // A new medicine without a start date starts today, so an end date in the past is an
            // error. An existing medicine whose course already ended can still be edited.
            startEpochDay = input.startEpochDay ?: if (input.medicineId == null) todayEpochDay() else null,
            endEpochDay = input.endEpochDay,
            reminderIntervalMinutes = input.reminderIntervalMinutes,
        )
        val errors = Validation.validateMedicine(draft)
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)
        val name = checkNotNull(Validation.normalizeName(input.name))
        val wanted = input.slots.toSet()

        return db.withTransaction {
            val today = todayEpochDay()
            val nowMinute = nowMinuteOfDay()
            var removed = CancelRefs.NONE
            val medicineId: Long
            val patientId: Long

            if (input.medicineId == null) {
                patientId = when (val p = input.patient) {
                    is PatientChoice.Existing -> patientDao.get(p.patientId)?.id
                        ?: return@withTransaction SaveResult.Invalid(listOf(ValidationError.PATIENT_MISSING))
                    is PatientChoice.New -> insertPatient(
                        checkNotNull(p.name),
                        checkNotNull(Validation.parseAge(p.ageText)),
                        checkNotNull(p.gender),
                    )
                    PatientChoice.None -> return@withTransaction SaveResult.Invalid(listOf(ValidationError.PATIENT_MISSING))
                }
                medicineId = medicineDao.insert(
                    MedicineEntity(
                        patientId = patientId,
                        name = name,
                        startEpochDay = input.startEpochDay,
                        endEpochDay = input.endEpochDay,
                        active = true,
                        reminderIntervalMinutes = input.reminderIntervalMinutes,
                    )
                )
                wanted.forEach { insertSlot(medicineId, it, today, nowMinute) }
            } else {
                val old = medicineDao.get(input.medicineId)
                    ?: return@withTransaction SaveResult.Invalid(listOf(ValidationError.PATIENT_MISSING))
                medicineId = old.id
                patientId = old.patientId
                medicineDao.update(
                    old.copy(
                        name = name,
                        startEpochDay = input.startEpochDay,
                        endEpochDay = input.endEpochDay,
                        reminderIntervalMinutes = input.reminderIntervalMinutes,
                    )
                )
                val oldSlots = medicineDao.slotsFor(medicineId)
                val removedSlots = oldSlots.filter { Slot(it.dayOfWeek, it.minuteOfDay) !in wanted }
                val kept = oldSlots.map { Slot(it.dayOfWeek, it.minuteOfDay) }.toSet()
                val removedSlotIds = removedSlots.map { it.id }
                val removedDoseIds = if (removedSlotIds.isEmpty()) emptyList() else {
                    doseDao.pendingIdsForSlots(removedSlotIds, today)
                }.toMutableList()
                if (!ScheduleRules.inRange(today, input.startEpochDay, input.endEpochDay)) {
                    removedDoseIds += doseDao.pendingIdsForMedicine(medicineId, today)
                }
                val doseIds = removedDoseIds.distinct()
                if (doseIds.isNotEmpty()) doseDao.deleteByIds(doseIds)
                if (removedSlotIds.isNotEmpty()) medicineDao.deleteSlots(removedSlotIds)
                (wanted - kept).forEach { insertSlot(medicineId, it, today, nowMinute) }
                removed = CancelRefs(removedSlotIds, doseIds)
            }

            val rewards = evaluatePatientInTx(patientId, today)
            if (rewards != null && today > rewards.lastEvaluatedEpochDay) ensureDosesForPatient(patientId, today)
            SaveResult.Saved(medicineId, patientId, removed)
        }
    }

    private suspend fun insertSlot(medicineId: Long, slot: Slot, today: Long, nowMinute: Int) {
        medicineDao.insertSlot(
            MedicineSlotEntity(
                medicineId = medicineId,
                dayOfWeek = slot.dayOfWeek,
                minuteOfDay = slot.minuteOfDay,
                validFromEpochDay = today,
                validFromMinuteOfDay = nowMinute,
            )
        )
    }

    /** Deletes the medicine with its slots and doses. Points already earned stay. */
    suspend fun deleteMedicine(id: Long): CancelRefs = db.withTransaction {
        val refs = CancelRefs(
            slotIds = medicineDao.slotIdsForMedicine(id),
            doseIds = doseDao.pendingIdsForMedicine(id, todayEpochDay()),
        )
        medicineDao.delete(id)
        refs
    }

    // ---------------------------------------------------------------- doses and evaluation

    /**
     * Brings every patient up to date: finalizes days that ended (PENDING becomes MISSED, streak
     * and bonuses updated) and makes sure today's doses exist. Idempotent.
     */
    suspend fun refreshToday() {
        settings()
        val today = todayEpochDay()
        for (p in patientDao.all()) {
            db.withTransaction {
                val r = evaluatePatientInTx(p.id, today)
                if (r != null && today > r.lastEvaluatedEpochDay) ensureDosesForPatient(p.id, today)
            }
        }
    }

    /** Creates the doses of [epochDay] for one patient. Running it twice creates nothing new. */
    suspend fun ensureDosesFor(patientId: Long, epochDay: Long) = db.withTransaction {
        ensureDosesForPatient(patientId, epochDay)
    }

    private suspend fun ensureDosesForPatient(patientId: Long, epochDay: Long) {
        for (row in medicineDao.slotSchedulesForPatient(patientId)) {
            if (!ScheduleRules.producesDose(row.toSpec(), epochDay)) continue
            doseDao.insertIgnore(
                DoseEventEntity(
                    medicineId = row.medicineId,
                    slotId = row.slotId,
                    scheduledEpochDay = epochDay,
                    scheduledMinuteOfDay = row.minuteOfDay,
                    status = DoseStatus.PENDING.name,
                    confirmedAtMillis = null,
                    pointsAwarded = 0,
                    notifiedAtMillis = null,
                    remindersSent = 0,
                )
            )
        }
    }

    private suspend fun evaluatePatientInTx(patientId: Long, today: Long): RewardsEntity? {
        val patient = patientDao.get(patientId) ?: return null
        val stored = rewardsDao.get(patientId) ?: RewardsEntity(
            patientId = patientId,
            totalPoints = 0,
            currentStreak = 0,
            streakStartEpochDay = null,
            bestStreak = 0,
            lastPerfectDay = null,
            lastEvaluatedEpochDay = patient.createdAtEpochDay - 1,
            brokenStreakPending = null,
        )
        var state = stored.toState()
        var points = stored.totalPoints
        var lastPerfect = stored.lastPerfectDay
        // A clock moved backward makes this loop empty: no negative streaks, nothing re-paid.
        var day = state.lastEvaluatedEpochDay + 1
        while (day < today) {
            ensureDosesForPatient(patientId, day)
            doseDao.markMissed(patientId, day)
            val result = StreakRules.dayResult(statusesFor(patientId, day))
            state = StreakRules.finalizeDay(state, day, result)
            if (result == DayResult.PERFECT) {
                lastPerfect = day
                if (awardDao.insertPerfectDay(PerfectDayAwardEntity(patientId, day, clock.millis())) != -1L) {
                    points += PointsRules.PERFECT_DAY_BONUS
                }
                val milestone = PointsRules.milestoneFor(state.currentStreak)
                val runStart = state.streakStartEpochDay
                if (milestone != null && runStart != null &&
                    awardDao.insertMilestone(StreakMilestoneAwardEntity(patientId, runStart, milestone, clock.millis())) != -1L
                ) {
                    points += PointsRules.STREAK_MILESTONE_BONUS
                }
            }
            day++
        }
        val updated = stored.copy(
            totalPoints = points,
            currentStreak = state.currentStreak,
            streakStartEpochDay = state.streakStartEpochDay,
            bestStreak = state.bestStreak,
            lastPerfectDay = lastPerfect,
            lastEvaluatedEpochDay = state.lastEvaluatedEpochDay,
            brokenStreakPending = state.brokenStreakPending,
        )
        if (updated != stored || rewardsDao.get(patientId) == null) rewardsDao.upsert(updated)
        return updated
    }

    private suspend fun statusesFor(patientId: Long, epochDay: Long): List<DoseStatus> =
        doseDao.statusesFor(patientId, epochDay).map { DoseStatus.valueOf(it) }

    /**
     * Confirms one dose in a single transaction: dose TAKEN, points, perfect-day bonus and streak
     * milestone together, or nothing at all.
     */
    suspend fun confirmDose(doseId: Long): ConfirmOutcome = db.withTransaction {
        val today = todayEpochDay()
        val first = doseDao.get(doseId) ?: return@withTransaction ConfirmOutcome.Rejected(null)
        val medicine = medicineDao.get(first.medicineId) ?: return@withTransaction ConfirmOutcome.Rejected(null)
        val patient = patientDao.get(medicine.patientId) ?: return@withTransaction ConfirmOutcome.Rejected(null)
        val rewardsBefore = evaluatePatientInTx(patient.id, today) ?: return@withTransaction ConfirmOutcome.Rejected(null)
        // Evaluation may have turned an old dose into MISSED, so read it again.
        val dose = doseDao.get(doseId) ?: return@withTransaction ConfirmOutcome.Rejected(null)
        val check = DoseRules.canConfirm(DoseStatus.valueOf(dose.status), dose.scheduledEpochDay, today)
        if (check != ConfirmCheck.OK) return@withTransaction ConfirmOutcome.Rejected(check)

        val now = nowLocal()
        val nowMillis = clock.millis()
        val points = PointsRules.dosePoints(dose.scheduledMinuteOfDay, now.toLocalTime().toSecondOfDay())
        val paid = awardDao.insertDoseAward(
            DoseAwardEntity(
                patientId = patient.id,
                scheduledEpochDay = dose.scheduledEpochDay,
                scheduledMinuteOfDay = dose.scheduledMinuteOfDay,
                medicineKey = Validation.medicineKey(medicine.name),
                points = points.total,
                awardedAtMillis = nowMillis,
            )
        ) != -1L
        doseDao.update(
            dose.copy(
                status = DoseStatus.TAKEN.name,
                confirmedAtMillis = nowMillis,
                pointsAwarded = if (paid) points.total else 0,
            )
        )

        val state = rewardsBefore.toState()
        val complete = StreakRules.isComplete(statusesFor(patient.id, today))
        var perfectBonus = 0
        var milestone: Int? = null
        var milestoneBonus = 0
        var best = rewardsBefore.bestStreak
        var lastPerfect = rewardsBefore.lastPerfectDay
        if (complete && StreakRules.isOpenDay(state, today)) {
            if (awardDao.insertPerfectDay(PerfectDayAwardEntity(patient.id, today, nowMillis)) != -1L) {
                perfectBonus = PointsRules.PERFECT_DAY_BONUS
            }
            val runLength = state.currentStreak + 1
            val runStart = StreakRules.runStartIfComplete(state, today)
            val reached = PointsRules.milestoneFor(runLength)
            if (reached != null &&
                awardDao.insertMilestone(StreakMilestoneAwardEntity(patient.id, runStart, reached, nowMillis)) != -1L
            ) {
                milestone = reached
                milestoneBonus = PointsRules.STREAK_MILESTONE_BONUS
            }
            best = maxOf(best, runLength)
            lastPerfect = today
        }
        val total = rewardsBefore.totalPoints + (if (paid) points.total else 0) + perfectBonus + milestoneBonus
        rewardsDao.upsert(rewardsBefore.copy(totalPoints = total, bestStreak = best, lastPerfectDay = lastPerfect))

        ConfirmOutcome.Confirmed(
            patientId = patient.id,
            patientName = patient.name,
            ageGroup = AgeGroup.of(currentAge(patient)),
            gender = Gender.fromStored(patient.gender),
            medicineName = medicine.name,
            dosePoints = points.base,
            onTimeBonus = points.onTimeBonus,
            dosePaid = paid,
            perfectDayBonus = perfectBonus,
            milestone = milestone,
            milestoneBonus = milestoneBonus,
            liveStreak = StreakRules.liveStreak(state, today, complete),
            totalPoints = total,
            dayComplete = complete,
        )
    }

    /** The streak-broken animation was shown: never show it again for this break. */
    suspend fun acknowledgeStreakBreak(patientId: Long) = db.withTransaction {
        val r = rewardsDao.get(patientId) ?: return@withTransaction
        if (r.brokenStreakPending != null) rewardsDao.upsert(r.copy(brokenStreakPending = null))
    }

    // ---------------------------------------------------------------- home

    fun observeHome(todayEpochDay: Long): Flow<HomeData> = combine(
        patientDao.observeAll(),
        rewardsDao.observeAll(),
        doseDao.observeDay(todayEpochDay),
        observeSettings(),
    ) { patients, rewards, doses, settings ->
        val byPatient = rewards.associateBy { it.patientId }
        val homePatients = patients.map { p ->
            val own = doses.filter { it.patientId == p.id }
            val statuses = own.map { DoseStatus.valueOf(it.status) }
            val complete = StreakRules.isComplete(statuses)
            val r = byPatient[p.id]
            val state = r?.toState() ?: StreakState(0, null, 0, todayEpochDay - 1, null)
            val live = StreakRules.liveStreak(state, todayEpochDay, complete)
            val points = r?.totalPoints ?: 0
            val age = Ages.currentAge(p.ageAtCreation, p.createdAtEpochDay, todayEpochDay)
            HomePatient(
                patient = p,
                age = age,
                ageGroup = AgeGroup.of(age),
                totalPoints = points,
                level = PointsRules.level(points),
                pointsToNextLevel = PointsRules.pointsToNextLevel(points),
                liveStreak = live,
                bestStreak = maxOf(r?.bestStreak ?: 0, live),
                doses = own,
                takenToday = statuses.count { it == DoseStatus.TAKEN },
                todayComplete = complete,
                brokenStreakPending = r?.brokenStreakPending,
            )
        }
        HomeData(
            todayEpochDay = todayEpochDay,
            patients = homePatients,
            selectedPatientId = settings.selectedPatientId?.takeIf { id -> patients.any { it.id == id } },
            settings = settings,
        )
    }

    // ---------------------------------------------------------------- reminders support

    suspend fun allSlotSchedules(): List<SlotScheduleRow> = medicineDao.allSlotSchedules()

    suspend fun slotSchedule(slotId: Long): SlotScheduleRow? = medicineDao.slotSchedule(slotId)

    suspend fun doseRow(doseId: Long): DoseRow? = doseDao.row(doseId)

    /**
     * True when [doseId] is the FINAL scheduled dose of a course with an end date (batch 6, R6), read
     * from its medicine's current schedule. Reads only: it never changes a dose, a point or a streak.
     */
    suspend fun isFinalDoseOfCourse(doseId: Long): Boolean {
        val dose = doseDao.row(doseId) ?: return false
        val slots = medicineDao.allSlotSchedules().filter { it.medicineId == dose.medicineId }.map { it.toSpec() }
        return CourseEnd.isFinalDose(slots, dose.scheduledEpochDay, dose.scheduledMinuteOfDay)
    }

    /**
     * A slot alarm fired for [occurrenceEpochDay]. Returns what to announce, or null when the
     * dose is already taken, from another day, or was announced before.
     */
    suspend fun onSlotDue(slotId: Long, occurrenceEpochDay: Long): ReminderInfo? = db.withTransaction {
        val slot = medicineDao.slotSchedule(slotId) ?: return@withTransaction null
        val today = todayEpochDay()
        if (occurrenceEpochDay != today) return@withTransaction null
        val rewards = evaluatePatientInTx(slot.patientId, today) ?: return@withTransaction null
        if (today > rewards.lastEvaluatedEpochDay) ensureDosesForPatient(slot.patientId, today)
        val dose = doseDao.day(today).firstOrNull {
            it.medicineId == slot.medicineId && it.scheduledMinuteOfDay == slot.minuteOfDay
        } ?: return@withTransaction null
        if (dose.status != DoseStatus.PENDING.name || dose.notifiedAtMillis != null) return@withTransaction null
        val stored = checkNotNull(doseDao.get(dose.doseId))
        doseDao.update(stored.copy(notifiedAtMillis = clock.millis()))
        reminderInfo(checkNotNull(doseDao.row(dose.doseId)), moreRepeatsLeft = true)
    }

    /** A follow-up alarm fired. Counts it and returns what to announce, or null to stop. */
    suspend fun onRepeatDue(doseId: Long): ReminderInfo? = db.withTransaction {
        val dose = doseDao.get(doseId) ?: return@withTransaction null
        if (dose.status != DoseStatus.PENDING.name) return@withTransaction null
        if (dose.scheduledEpochDay != todayEpochDay()) return@withTransaction null
        if (dose.remindersSent >= MAX_REPEATS) return@withTransaction null
        val sent = dose.remindersSent + 1
        doseDao.update(dose.copy(remindersSent = sent, notifiedAtMillis = dose.notifiedAtMillis ?: clock.millis()))
        reminderInfo(checkNotNull(doseDao.row(doseId)), moreRepeatsLeft = sent < MAX_REPEATS)
    }

    /** Today's doses that are inside a running follow-up sequence (for re-arming after a kill). */
    suspend fun dosesWithRunningRepeats(): List<DoseRow> =
        doseDao.day(todayEpochDay()).filter {
            it.status == DoseStatus.PENDING.name && it.notifiedAtMillis != null && it.remindersSent < MAX_REPEATS
        }

    private suspend fun reminderInfo(row: DoseRow, moreRepeatsLeft: Boolean): ReminderInfo? {
        val patient = patientDao.get(row.patientId) ?: return null
        return ReminderInfo(row, patient.name, AgeGroup.of(currentAge(patient)), moreRepeatsLeft)
    }

    companion object {
        /** Follow-up reminders after the first one. */
        const val MAX_REPEATS = ReminderRules.MAX_REPEATS
    }
}

internal fun SlotScheduleRow.toSpec() = SlotSpec(
    dayOfWeek = dayOfWeek,
    minuteOfDay = minuteOfDay,
    validFromEpochDay = validFromEpochDay,
    validFromMinuteOfDay = validFromMinuteOfDay,
    startEpochDay = startEpochDay,
    endEpochDay = endEpochDay,
    active = active,
)

internal fun RewardsEntity.toState() = StreakState(
    currentStreak = currentStreak,
    streakStartEpochDay = streakStartEpochDay,
    bestStreak = bestStreak,
    lastEvaluatedEpochDay = lastEvaluatedEpochDay,
    brokenStreakPending = brokenStreakPending,
)
