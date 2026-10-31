package com.example.tracker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.data.CancelRefs
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.PinCheck
import com.example.tracker.data.SaveResult
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.data.db.SettingsEntity
import com.example.tracker.domain.Gender
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.PinHasher
import com.example.tracker.domain.Slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

/** Parent settings > Erase all data: every table emptied, nothing old comes back. */
@RunWith(AndroidJUnit4::class)
class RepositoryEraseTest {

    private lateinit var db: AppDatabase
    private lateinit var clock: TestClock
    private lateinit var repo: TrackerRepository
    private val monday = LocalDate.parse("2026-10-12")
    private val wednesday = monday.plusDays(2)

    private val tables = listOf(
        "patient", "medicine", "medicine_slot", "dose_event", "rewards",
        "dose_award", "perfect_day_award", "streak_milestone_award", "app_settings",
    )

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        clock = TestClock(monday.atTime(7, 0))
        repo = TrackerRepository(db, clock, PinHasher(iterations = 1_000))
    }

    @After
    fun tearDown() = db.close()

    private fun daily(minute: Int) = (1..7).map { Slot(it, minute) }

    private fun counts(): Map<String, Int> = tables.associateWith { table ->
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getInt(0) }
    }

    private suspend fun add(patient: PatientChoice, name: String, slots: List<Slot>): SaveResult.Saved =
        repo.saveMedicine(
            MedicineInput(null, patient, name, slots, startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        ) as SaveResult.Saved

    private suspend fun confirmAt(medicineId: Long, time: LocalDateTime, minute: Int): ConfirmOutcome {
        clock.set(time)
        repo.refreshToday()
        val dose = db.doses().all().single {
            it.medicineId == medicineId && it.scheduledEpochDay == time.toLocalDate().toEpochDay() && it.scheduledMinuteOfDay == minute
        }
        return repo.confirmDose(dose.id)
    }

    /**
     * Yasmin (7) takes Panadol at 08:00 Monday to Wednesday: 3 perfect days and the 3-day
     * milestone, 155 points. Omar (40) misses Vitamin D at 21:00. PIN, language, filter, PIN
     * toggle, rationale flag and one failed PIN try are set. Ends Wednesday 08:00.
     */
    private suspend fun fill(): Pair<SaveResult.Saved, SaveResult.Saved> {
        repo.setPin("1234")
        repo.setLanguage("ar")
        val a = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(480))
        val b = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Vitamin D", daily(1260))
        for (d in 0L..2L) assertTrue(confirmAt(a.medicineId, monday.plusDays(d).atTime(8, 0), 480) is ConfirmOutcome.Confirmed)
        repo.setSelectedPatient(a.patientId)
        repo.setRequirePinForDose(false)
        repo.markNotificationRationaleShown()
        assertEquals(PinCheck.Wrong(4), repo.verifyPin("0000"))
        assertEquals(155, db.rewards().get(a.patientId)!!.totalPoints) // 3 x (10 + 5 + 20) + 50
        return a to b
    }

    @Test
    fun eraseEmptiesEveryTableAndReturnsEveryAlarmToCancel() = runBlocking {
        val (a, b) = fill()
        assertEquals(
            mapOf(
                "patient" to 2, "medicine" to 2, "medicine_slot" to 14, "dose_event" to 6, "rewards" to 2,
                "dose_award" to 3, "perfect_day_award" to 3, "streak_milestone_award" to 1, "app_settings" to 1,
            ),
            counts(),
        )
        val expectedSlots = (db.medicines().slotsFor(a.medicineId) + db.medicines().slotsFor(b.medicineId)).map { it.id }.sorted()
        val expectedDoses = db.doses().all().filter { it.scheduledEpochDay == wednesday.toEpochDay() }.map { it.id }.sorted()
        assertEquals(2, expectedDoses.size)

        val refs = repo.eraseAllData()

        assertEquals(expectedSlots, refs.slotIds.sorted())
        assertEquals(expectedDoses, refs.doseIds.sorted())
        assertEquals(tables.associateWith { 0 }, counts())
        // Language, PIN, lockout, toggle, filter and rationale flag are all back to first launch.
        assertEquals(SettingsEntity.defaults(), repo.settings())
        assertFalse(repo.hasPin())
        assertEquals(PinCheck.NoPin, repo.verifyPin("1234"))
        assertTrue(repo.patients().isEmpty())
    }

    @Test
    fun nothingOldComesBackAfterErase() = runBlocking {
        val (a, b) = fill()
        val oldSlotIds = db.medicines().allSlotSchedules().map { it.slotId }
        val oldDoses = db.doses().all()
        val oldTodayDoseIds = oldDoses.filter { it.scheduledEpochDay == wednesday.toEpochDay() }.map { it.id }

        repo.eraseAllData()

        // Alarms that slipped through the cancel find nothing: no reminder, no follow-up.
        for (id in oldSlotIds) assertNull(repo.onSlotDue(id, wednesday.toEpochDay()))
        for (id in oldTodayDoseIds) assertNull(repo.onRepeatDue(id))

        // Starting again with the same names: new ids, points from zero.
        repo.setPin("5678")
        clock.set(wednesday.atTime(8, 30))
        val c = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(540))
        assertTrue(c.patientId > maxOf(a.patientId, b.patientId))
        assertTrue(db.medicines().slotsFor(c.medicineId).all { it.id > oldSlotIds.max() })
        assertTrue(db.doses().all().all { it.id > oldDoses.maxOf { d -> d.id } })
        for (id in oldSlotIds) assertNull(repo.onSlotDue(id, wednesday.toEpochDay()))

        val first = confirmAt(c.medicineId, wednesday.atTime(9, 0), 540) as ConfirmOutcome.Confirmed
        assertTrue(first.dosePaid)
        assertEquals(35, first.earned) // 10 + 5 on time + 20 perfect day
        assertEquals(35, first.totalPoints)
        assertEquals(1, first.liveStreak)
        assertNull(first.milestone)
        assertEquals(listOf(c.patientId), db.rewards().observeAll().first().map { it.patientId })
    }

    @Test
    fun eraseWithNothingStoredIsHarmless() = runBlocking {
        assertEquals(CancelRefs.NONE, repo.eraseAllData())
        assertEquals(tables.associateWith { 0 }, counts())
        assertEquals(SettingsEntity.defaults(), repo.settings())
    }
}
