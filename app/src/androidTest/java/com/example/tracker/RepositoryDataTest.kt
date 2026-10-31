package com.example.tracker

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.data.CancelRefs
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.SaveResult
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.data.db.DoseEventEntity
import com.example.tracker.domain.Gender
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.PinHasher
import com.example.tracker.domain.Slot
import com.example.tracker.domain.ValidationError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

/** Phase 1: persistence, patients, schedules, filter. In-memory Room, injected clock. */
@RunWith(AndroidJUnit4::class)
class RepositoryDataTest {

    private lateinit var db: AppDatabase
    private lateinit var clock: TestClock
    private lateinit var repo: TrackerRepository

    // Monday 2026-10-12, 07:00 Cairo time.
    private val monday = LocalDate.parse("2026-10-12")

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        clock = TestClock(monday.atTime(7, 0))
        repo = TrackerRepository(db, clock, PinHasher(iterations = 1_000))
    }

    @After
    fun tearDown() = db.close()

    private fun daily(vararg minutes: Int) = (1..7).flatMap { d -> minutes.map { Slot(d, it) } }

    private suspend fun add(patient: PatientChoice, name: String, slots: List<Slot>, start: Long? = null, end: Long? = null): SaveResult.Saved =
        repo.saveMedicine(
            MedicineInput(null, patient, name, slots, startEpochDay = start, endEpochDay = end, reminderIntervalMinutes = 10)
        ) as SaveResult.Saved

    private suspend fun dosesOf(patientId: Long): List<DoseEventEntity> {
        val meds = db.medicines().forPatient(patientId).map { it.id }.toSet()
        return db.doses().all().filter { it.medicineId in meds }
    }

    @Test
    fun twoPatientsWithTheSameNameStayIndependent() = runBlocking {
        val a = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(8 * 60))
        val b = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Vitamin D", daily(9 * 60))
        assertNotEquals(a.patientId, b.patientId)
        assertEquals(2, repo.patients().count { it.name == "Yasmin" })
        assertEquals(listOf("Panadol"), db.medicines().forPatient(a.patientId).map { it.name })
        assertEquals(listOf("Vitamin D"), db.medicines().forPatient(b.patientId).map { it.name })

        val home = repo.observeHome(monday.toEpochDay()).first()
        val homeA = home.patients.single { it.patient.id == a.patientId }
        val homeB = home.patients.single { it.patient.id == b.patientId }
        assertEquals(listOf("Panadol"), homeA.doses.map { it.medicineName })
        assertEquals(listOf("Vitamin D"), homeB.doses.map { it.medicineName })
    }

    @Test
    fun genderIsStoredAndEditedPerPatient() = runBlocking {
        val girl = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(8 * 60))
        val notSet = repo.addPatient("Omar", 40) // like a patient created before v3
        val control = add(PatientChoice.New("Yasmin", "7", Gender.MALE), "Vitamin D", daily(9 * 60))
        assertEquals("FEMALE", repo.patient(girl.patientId)!!.gender)
        assertNull(repo.patient(notSet)!!.gender)

        // Editing without choosing keeps what is stored; choosing sets it. The control never changes.
        val controlBefore = repo.patient(control.patientId)
        assertTrue(repo.updatePatient(girl.patientId, "Yasmin", 7, gender = null))
        assertEquals("FEMALE", repo.patient(girl.patientId)!!.gender)
        assertTrue(repo.updatePatient(notSet, "Omar", 40, Gender.MALE))
        assertEquals("MALE", repo.patient(notSet)!!.gender)
        assertEquals(controlBefore, repo.patient(control.patientId))
    }

    @Test
    fun deletingOnePatientLeavesTheOtherExact() = runBlocking {
        val a = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(8 * 60))
        val b = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Vitamin D", daily(9 * 60))
        clock.set(monday.atTime(8, 5))
        val doseA = dosesOf(a.patientId).single()
        val doseB = dosesOf(b.patientId).single()
        assertTrue(repo.confirmDose(doseA.id) is ConfirmOutcome.Confirmed)
        assertTrue(repo.confirmDose(doseB.id) is ConfirmOutcome.Confirmed)

        val bMedicinesBefore = db.medicines().forPatient(b.patientId)
        val bSlotsBefore = db.medicines().slotsFor(bMedicinesBefore.single().id)
        val bDosesBefore = dosesOf(b.patientId)
        val bRewardsBefore = db.rewards().get(b.patientId)
        val bAwardsBefore = db.awards().doseAwardCount(b.patientId)

        val refs = repo.deletePatient(a.patientId)

        // Target: everything of A is gone.
        assertNull(repo.patient(a.patientId))
        assertEquals(emptyList<Any>(), db.medicines().forPatient(a.patientId))
        assertEquals(emptyList<Any>(), dosesOf(a.patientId))
        assertNull(db.rewards().get(a.patientId))
        assertEquals(0, db.awards().doseAwardCount(a.patientId))
        assertEquals(7, refs.slotIds.size) // A's daily slots are returned for alarm cancelling
        // Control: B is exactly as before.
        assertEquals(bMedicinesBefore, db.medicines().forPatient(b.patientId))
        assertEquals(bSlotsBefore, db.medicines().slotsFor(bMedicinesBefore.single().id))
        assertEquals(bDosesBefore, dosesOf(b.patientId))
        assertEquals(bRewardsBefore, db.rewards().get(b.patientId))
        assertEquals(bAwardsBefore, db.awards().doseAwardCount(b.patientId))
    }

    @Test
    fun uniqueDoseIndexMakesDuplicatesImpossible() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(10 * 60))
        val existing = dosesOf(a.patientId).single()
        val copy = existing.copy(id = 0)
        assertEquals(-1L, db.doses().insertIgnore(copy))
        assertEquals(1, dosesOf(a.patientId).size)
    }

    @Test
    fun ensureDosesForIsIdempotent() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(10 * 60, 22 * 60))
        val day = monday.toEpochDay()
        repo.ensureDosesFor(a.patientId, day)
        repo.ensureDosesFor(a.patientId, day)
        repo.refreshToday()
        repo.refreshToday()
        assertEquals(listOf(600, 1320), dosesOf(a.patientId).map { it.scheduledMinuteOfDay }.sorted())
    }

    @Test
    fun mondayOnlyMedicineCreatesDosesOnlyOnMondays() = runBlocking {
        val sunday = monday.minusDays(1)
        clock.set(sunday.atTime(7, 0))
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Weekly", listOf(Slot(1, 9 * 60)))
        val control = add(PatientChoice.Existing(a.patientId), "Daily", daily(9 * 60))
        for (offset in 0L..8L) {
            clock.set(sunday.plusDays(offset).atTime(12, 0))
            repo.refreshToday()
        }
        val weeklyDays = db.doses().all().filter { it.medicineId == a.medicineId }.map { LocalDate.ofEpochDay(it.scheduledEpochDay) }
        val dailyDays = db.doses().all().filter { it.medicineId == control.medicineId }.map { it.scheduledEpochDay }
        assertEquals(listOf(monday, monday.plusDays(7)), weeklyDays.sorted())
        assertEquals((0L..8L).map { sunday.toEpochDay() + it }, dailyDays.sorted())
    }

    @Test
    fun medicineOutsideItsDatesGetsNoDoses() = runBlocking {
        val wednesday = monday.plusDays(2).toEpochDay()
        val friday = monday.plusDays(4).toEpochDay()
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Course", daily(9 * 60), start = wednesday, end = friday)
        for (offset in 0L..6L) {
            clock.set(monday.plusDays(offset).atTime(8, 0))
            repo.refreshToday()
        }
        assertEquals(listOf(wednesday, wednesday + 1, friday), dosesOf(a.patientId).map { it.scheduledEpochDay }.sorted())
    }

    @Test
    fun editRemovingSlotsDeletesTodaysPendingKeepsTakenAndOtherMedicines() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(8 * 60, 20 * 60))
        val other = add(PatientChoice.Existing(a.patientId), "Vitamin D", daily(20 * 60))
        clock.set(monday.atTime(8, 10))
        val morning = db.doses().all().single { it.medicineId == a.medicineId && it.scheduledMinuteOfDay == 480 }
        assertTrue(repo.confirmDose(morning.id) is ConfirmOutcome.Confirmed)
        val otherDosesBefore = db.doses().all().filter { it.medicineId == other.medicineId }

        // Keep only a new 12:00 time.
        val result = repo.saveMedicine(
            MedicineInput(a.medicineId, PatientChoice.Existing(a.patientId), "Panadol", daily(12 * 60), startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        ) as SaveResult.Saved

        val doses = db.doses().all().filter { it.medicineId == a.medicineId }
        val taken = doses.single { it.scheduledMinuteOfDay == 480 }
        assertEquals("TAKEN", taken.status)
        assertNull(taken.slotId) // its slot is gone, the history stays
        assertTrue(doses.none { it.scheduledMinuteOfDay == 1200 }) // today's PENDING 20:00 removed
        assertEquals(listOf(720), doses.filter { it.status == "PENDING" }.map { it.scheduledMinuteOfDay })
        assertEquals(14, result.removed.slotIds.size) // 7 days x 2 old times
        assertEquals(1, result.removed.doseIds.size)
        assertEquals(otherDosesBefore, db.doses().all().filter { it.medicineId == other.medicineId })
        assertEquals(listOf(720), db.medicines().slotsFor(a.medicineId).map { it.minuteOfDay }.distinct())
    }

    @Test
    fun medicineAddedAfterItsTimeHasNoDoseThatDay() = runBlocking {
        clock.set(monday.atTime(14, 0))
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(8 * 60, 20 * 60))
        assertEquals(listOf(1200), dosesOf(a.patientId).map { it.scheduledMinuteOfDay })
    }

    @Test
    fun invalidInputIsRejectedAndNothingIsSaved() = runBlocking {
        val noPatient = repo.saveMedicine(
            MedicineInput(null, PatientChoice.None, "Panadol", daily(480), startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        )
        assertEquals(SaveResult.Invalid(listOf(ValidationError.PATIENT_MISSING)), noPatient)
        val badAge = repo.saveMedicine(
            MedicineInput(null, PatientChoice.New("Omar", "121", Gender.FEMALE), "Panadol", daily(480), startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        )
        assertEquals(SaveResult.Invalid(listOf(ValidationError.AGE_INVALID)), badAge)
        val noSlots = repo.saveMedicine(
            MedicineInput(null, PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", emptyList(), startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        )
        assertEquals(SaveResult.Invalid(listOf(ValidationError.NO_SLOTS)), noSlots)
        // A new medicine that would already have ended.
        val ended = repo.saveMedicine(
            MedicineInput(null, PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480), startEpochDay = null, endEpochDay = monday.toEpochDay() - 1, reminderIntervalMinutes = 10)
        )
        assertEquals(SaveResult.Invalid(listOf(ValidationError.END_BEFORE_START)), ended)
        assertEquals(emptyList<Any>(), repo.patients())
        assertEquals(emptyList<Any>(), db.medicines().all())
    }

    @Test
    fun filterIsViewOnly() = runBlocking {
        val a = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(8 * 60))
        val b = add(PatientChoice.New("Omar", "15", Gender.FEMALE), "Vitamin D", daily(9 * 60))
        val dosesBefore = db.doses().all()
        val rewardsBefore = listOf(db.rewards().get(a.patientId), db.rewards().get(b.patientId))

        repo.setSelectedPatient(a.patientId)

        val home = repo.observeHome(monday.toEpochDay()).first()
        assertEquals(a.patientId, home.selectedPatientId)
        assertEquals(dosesBefore, db.doses().all())
        assertEquals(rewardsBefore, listOf(db.rewards().get(a.patientId), db.rewards().get(b.patientId)))
        assertEquals(a.patientId, repo.settings().selectedPatientId) // stored in the DB
    }

    @Test
    fun deletingTheSelectedPatientFallsBackToAll() = runBlocking {
        val a = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(8 * 60))
        add(PatientChoice.New("Omar", "15", Gender.FEMALE), "Vitamin D", daily(9 * 60))
        repo.setSelectedPatient(a.patientId)
        repo.deletePatient(a.patientId)
        assertNull(repo.settings().selectedPatientId)
        assertNull(repo.observeHome(monday.toEpochDay()).first().selectedPatientId)
    }

    @Test
    fun perfectDayStatusIsDerivedButTheBonusStays() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(8 * 60))
        clock.set(monday.atTime(8, 0))
        val dose = dosesOf(a.patientId).single()
        val outcome = repo.confirmDose(dose.id) as ConfirmOutcome.Confirmed
        assertEquals(20, outcome.perfectDayBonus)
        assertTrue(repo.observeHome(monday.toEpochDay()).first().patients.single().todayComplete)
        val pointsAfterPerfect = db.rewards().get(a.patientId)!!.totalPoints
        assertEquals(35, pointsAfterPerfect) // 10 + 5 on time + 20

        // A medicine with a later dose today: status goes back, points never do.
        clock.set(monday.atTime(9, 0))
        add(PatientChoice.Existing(a.patientId), "Vitamin D", daily(21 * 60))
        val home = repo.observeHome(monday.toEpochDay()).first().patients.single()
        assertEquals(false, home.todayComplete)
        assertEquals(1, home.takenToday)
        assertEquals(2, home.doses.size)
        assertEquals(35, db.rewards().get(a.patientId)!!.totalPoints)
    }

    @Test
    fun transactionRollsBackOnFailure() = runBlocking {
        try {
            db.withTransaction {
                repo.addPatient("Ghost", 9)
                throw IllegalStateException("forced failure")
            }
            fail("expected the forced failure")
        } catch (e: IllegalStateException) {
            assertEquals("forced failure", e.message)
        }
        assertEquals(emptyList<Any>(), repo.patients())
        assertEquals(emptyList<Any>(), db.rewards().observeAll().first())
    }

    @Test
    fun foreignKeysAreEnforced() = runBlocking {
        try {
            db.medicines().insert(
                com.example.tracker.data.db.MedicineEntity(patientId = 999, name = "Orphan", startEpochDay = null, endEpochDay = null, active = true, reminderIntervalMinutes = 10)
            )
            fail("a medicine without a patient must be rejected")
        } catch (expected: SQLiteConstraintException) {
            // FK ON: Medicine.patientId must exist.
        }
        assertEquals(emptyList<Any>(), db.medicines().all())
    }

    @Test
    fun deletingAMedicineReturnsItsAlarmsAndKeepsPoints() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(8 * 60))
        clock.set(monday.atTime(8, 0))
        repo.confirmDose(dosesOf(a.patientId).single().id)
        val points = db.rewards().get(a.patientId)!!.totalPoints
        val refs: CancelRefs = repo.deleteMedicine(a.medicineId)
        assertEquals(7, refs.slotIds.size)
        assertEquals(emptyList<Any>(), db.medicines().all())
        assertEquals(points, db.rewards().get(a.patientId)!!.totalPoints)
    }

    @Test
    fun ageGrowsWithTimeAndEditingReanchors() = runBlocking {
        val id = repo.addPatient("Lina", 12)
        clock.set(LocalDateTime.of(2027, 10, 12, 9, 0))
        assertEquals(13, repo.currentAge(repo.patient(id)!!))
        repo.updatePatient(id, "Lina", 10)
        assertEquals(10, repo.currentAge(repo.patient(id)!!))
    }
}
