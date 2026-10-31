package com.example.tracker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.SaveResult
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.domain.AgeGroup
import com.example.tracker.domain.Gender
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.PinHasher
import com.example.tracker.domain.Slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Phase 2: what the alarm receivers decide, from the DB only. */
@RunWith(AndroidJUnit4::class)
class RepositoryReminderTest {

    private lateinit var db: AppDatabase
    private lateinit var clock: TestClock
    private lateinit var repo: TrackerRepository
    private val monday = LocalDate.parse("2026-10-12")
    private val day get() = monday.toEpochDay()

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        clock = TestClock(monday.atTime(7, 0))
        repo = TrackerRepository(db, clock, PinHasher(iterations = 1_000))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(name: String, age: String, medicine: String, slots: List<Slot>, interval: Int = 10): SaveResult.Saved =
        repo.saveMedicine(
            MedicineInput(null, PatientChoice.New(name, age, Gender.FEMALE), medicine, slots, startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = interval)
        ) as SaveResult.Saved

    private suspend fun slotId(medicineId: Long, minute: Int) =
        db.medicines().slotsFor(medicineId).first { it.dayOfWeek == 1 && it.minuteOfDay == minute }.id

    @Test
    fun slotAnnouncesItsDoseOnceWithPatientNameAndAgeGroup() = runBlocking {
        val a = add("Yasmin", "7", "Panadol", listOf(Slot(1, 8 * 60)))
        clock.set(monday.atTime(8, 0))
        val info = repo.onSlotDue(slotId(a.medicineId, 480), day)
        assertNotNull(info)
        assertEquals("Yasmin", info!!.patientName)
        assertEquals(AgeGroup.CHILD, info.ageGroup)
        assertEquals("Panadol", info.dose.medicineName)
        assertEquals(clock.millis(), info.dose.notifiedAtMillis)
        // The same alarm again (for example re-armed) announces nothing new.
        assertNull(repo.onSlotDue(slotId(a.medicineId, 480), day))
    }

    @Test
    fun exactlyThreeFollowUpsThenSilence() = runBlocking {
        val a = add("Omar", "40", "Panadol", listOf(Slot(1, 8 * 60)), interval = 5)
        clock.set(monday.atTime(8, 0))
        val first = repo.onSlotDue(slotId(a.medicineId, 480), day)!!
        val doseId = first.dose.doseId
        val sent = (1..5).map { repo.onRepeatDue(doseId) }
        assertEquals(listOf(true, true, false), sent.take(3).map { it!!.moreRepeatsLeft })
        assertEquals(listOf(1, 2, 3), sent.take(3).map { it!!.dose.remindersSent })
        assertNull(sent[3])
        assertNull(sent[4])
        assertEquals(3, db.doses().get(doseId)!!.remindersSent)
    }

    @Test
    fun confirmingStopsFollowUps() = runBlocking {
        val a = add("Omar", "40", "Panadol", listOf(Slot(1, 8 * 60)))
        clock.set(monday.atTime(8, 0))
        val doseId = repo.onSlotDue(slotId(a.medicineId, 480), day)!!.dose.doseId
        assertNotNull(repo.onRepeatDue(doseId))
        assertTrue(repo.confirmDose(doseId) is ConfirmOutcome.Confirmed)
        assertNull(repo.onRepeatDue(doseId))
        assertEquals(1, db.doses().get(doseId)!!.remindersSent)
        assertEquals(emptyList<Any>(), repo.dosesWithRunningRepeats())
    }

    @Test
    fun alarmFromAnotherDayAnnouncesNothing() = runBlocking {
        val a = add("Omar", "40", "Panadol", listOf(Slot(1, 8 * 60)))
        clock.set(monday.plusDays(1).atTime(0, 5)) // the Monday alarm arrives after midnight
        assertNull(repo.onSlotDue(slotId(a.medicineId, 480), day))
    }

    @Test
    fun followUpAfterMidnightIsDropped() = runBlocking {
        val a = add("Omar", "40", "Panadol", listOf(Slot(1, 23 * 60 + 50)))
        clock.set(monday.atTime(23, 50))
        val doseId = repo.onSlotDue(slotId(a.medicineId, 1430), day)!!.dose.doseId
        clock.set(monday.plusDays(1).atTime(0, 0))
        assertNull(repo.onRepeatDue(doseId))
    }

    @Test
    fun deletedMedicineNeverAnnounces() = runBlocking {
        val a = add("Omar", "40", "Panadol", listOf(Slot(1, 8 * 60)))
        val slot = slotId(a.medicineId, 480)
        val refs = repo.deleteMedicine(a.medicineId)
        assertEquals(listOf(slot), refs.slotIds)
        clock.set(monday.atTime(8, 0))
        assertNull(repo.onSlotDue(slot, day))
        assertNull(repo.slotSchedule(slot)) // the receiver books nothing next
    }

    @Test
    fun takenEarlyMeansNoReminderAtSlotTime() = runBlocking {
        val a = add("Omar", "40", "Panadol", listOf(Slot(1, 8 * 60)))
        clock.set(monday.atTime(7, 30))
        val dose = db.doses().all().single { it.medicineId == a.medicineId }
        assertTrue(repo.confirmDose(dose.id) is ConfirmOutcome.Confirmed)
        clock.set(monday.atTime(8, 0))
        assertNull(repo.onSlotDue(slotId(a.medicineId, 480), day))
    }

    @Test
    fun runningFollowUpsAreFoundAgainAfterAKillWithTheirCount() = runBlocking {
        val a = add("Omar", "40", "Panadol", listOf(Slot(1, 8 * 60)), interval = 15)
        val b = add("Lina", "15", "Vitamin D", listOf(Slot(1, 9 * 60)), interval = 30)
        clock.set(monday.atTime(8, 0))
        val doseA = repo.onSlotDue(slotId(a.medicineId, 480), day)!!.dose.doseId
        repo.onRepeatDue(doseA)
        repo.onRepeatDue(doseA)
        // Nothing announced for B yet, so it is not running.
        // A fresh repository (new process) reads the same DB.
        val afterKill = TrackerRepository(db, clock, PinHasher(iterations = 1_000))
        val running = afterKill.dosesWithRunningRepeats()
        assertEquals(listOf(doseA), running.map { it.doseId })
        assertEquals(2, running.single().remindersSent)
        assertEquals(15, running.single().reminderIntervalMinutes)
        assertEquals(b.medicineId, db.doses().all().single { it.medicineId == b.medicineId }.medicineId)
    }
}
