package com.example.tracker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.PinCheck
import com.example.tracker.data.SaveResult
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.domain.Gender
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.PinHasher
import com.example.tracker.domain.Slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Phase 3: PIN storage, lockout persistence, reset keeping data. */
@RunWith(AndroidJUnit4::class)
class RepositoryPinTest {

    private lateinit var db: AppDatabase
    private lateinit var clock: TestClock
    private lateinit var repo: TrackerRepository
    private val monday = LocalDate.parse("2026-10-12")

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        clock = TestClock(monday.atTime(7, 0))
        repo = TrackerRepository(db, clock, PinHasher(iterations = 1_000))
        runBlocking { repo.setPin("1234") }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun storedValuesNeverContainThePin() = runBlocking {
        val s = repo.settings()
        assertFalse(s.pinHash!!.contains("1234"))
        assertFalse(s.pinSalt!!.contains("1234"))
        assertTrue(s.pinHash!!.startsWith("pbkdf2-sha256:"))
    }

    @Test
    fun fourWrongTriesLeaveOneTryTheFifthLocks() = runBlocking {
        assertEquals(PinCheck.Wrong(4), repo.verifyPin("0000"))
        assertEquals(PinCheck.Wrong(3), repo.verifyPin("0000"))
        assertEquals(PinCheck.Wrong(2), repo.verifyPin("0000"))
        assertEquals(PinCheck.Wrong(1), repo.verifyPin("0000"))
        assertEquals(4, repo.settings().pinFailedAttempts)
        assertNull(repo.settings().pinLockedUntilMillis)
        assertEquals(PinCheck.Locked(60_000L), repo.verifyPin("0000"))
        // Even the right PIN is refused while locked.
        assertEquals(PinCheck.Locked(60_000L), repo.verifyPin("1234"))
    }

    @Test
    fun lockSurvivesARestartWithTheRemainingTime() = runBlocking {
        repeat(5) { repo.verifyPin("9999") }
        clock.set(monday.atTime(7, 0, 20))
        // New repository on the same DB = app restarted after a process kill.
        val restarted = TrackerRepository(db, clock, PinHasher(iterations = 1_000))
        assertEquals(40_000L, restarted.pinLockRemainingMillis())
        assertEquals(PinCheck.Locked(40_000L), restarted.verifyPin("1234"))
        clock.set(monday.atTime(7, 1, 0))
        assertEquals(PinCheck.Ok, restarted.verifyPin("1234"))
        assertEquals(0, repo.settings().pinFailedAttempts)
    }

    @Test
    fun correctPinClearsEarlierFailures() = runBlocking {
        repo.verifyPin("0000")
        repo.verifyPin("0000")
        assertEquals(PinCheck.Ok, repo.verifyPin("1234"))
        assertEquals(0, repo.settings().pinFailedAttempts)
        assertEquals(PinCheck.Wrong(4), repo.verifyPin("0000"))
    }

    @Test
    fun resetKeepsEveryPatientMedicineDosePointAndStreak() = runBlocking {
        val saved = repo.saveMedicine(
            MedicineInput(null, PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", (1..7).map { Slot(it, 8 * 60) }, startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        ) as SaveResult.Saved
        clock.set(monday.atTime(8, 0))
        val dose = db.doses().all().single()
        assertTrue(repo.confirmDose(dose.id) is ConfirmOutcome.Confirmed)
        val before = listOf(
            repo.patients(), db.medicines().all(), db.medicines().slotsFor(saved.medicineId),
            db.doses().all(), db.rewards().observeAll().first(), db.awards().milestones(saved.patientId),
        )
        val oldHash = repo.settings().pinHash

        repo.setPin("5678") // what the reset flow does after the phone lock was confirmed

        val after = listOf(
            repo.patients(), db.medicines().all(), db.medicines().slotsFor(saved.medicineId),
            db.doses().all(), db.rewards().observeAll().first(), db.awards().milestones(saved.patientId),
        )
        assertEquals(before, after)
        assertNotEquals(oldHash, repo.settings().pinHash)
        assertEquals(PinCheck.Wrong(4), repo.verifyPin("1234"))
        assertEquals(PinCheck.Ok, repo.verifyPin("5678"))
    }

    @Test
    fun wrongPinChangesNoDose() = runBlocking {
        repo.saveMedicine(
            MedicineInput(null, PatientChoice.New("Omar", "15", Gender.FEMALE), "Vitamin D", listOf(Slot(1, 9 * 60)), startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        )
        val dosesBefore = db.doses().all()
        val rewardsBefore = db.rewards().observeAll().first()
        assertEquals(PinCheck.Wrong(4), repo.verifyPin("1111"))
        assertEquals(dosesBefore, db.doses().all())
        assertEquals(rewardsBefore, db.rewards().observeAll().first())
        assertEquals("PENDING", db.doses().all().single().status)
    }
}
