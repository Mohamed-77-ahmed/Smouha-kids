package com.example.tracker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.SaveResult
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.data.db.RewardsEntity
import com.example.tracker.domain.ConfirmCheck
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
import java.time.ZoneId

/** Phase 4: every anti-farming invariant (CLAUDE.md 6) and streak rule (6b), exact values. */
@RunWith(AndroidJUnit4::class)
class RepositoryRewardsTest {

    private lateinit var db: AppDatabase
    private lateinit var clock: TestClock
    private lateinit var repo: TrackerRepository
    private val monday = LocalDate.parse("2026-10-12")

    @Before
    fun setUp() {
        db = AppDatabase.inMemory(ApplicationProvider.getApplicationContext())
        clock = TestClock(monday.atTime(7, 0))
        repo = TrackerRepository(db, clock, PinHasher(iterations = 1_000))
    }

    @After
    fun tearDown() = db.close()

    private fun daily(minute: Int) = (1..7).map { Slot(it, minute) }

    private suspend fun add(patient: PatientChoice, name: String, slots: List<Slot>): SaveResult.Saved =
        repo.saveMedicine(
            MedicineInput(null, patient, name, slots, startEpochDay = null, endEpochDay = null, reminderIntervalMinutes = 10)
        ) as SaveResult.Saved

    private suspend fun doseOn(medicineId: Long, day: LocalDate, minute: Int): Long {
        repo.refreshToday()
        return db.doses().all().single {
            it.medicineId == medicineId && it.scheduledEpochDay == day.toEpochDay() && it.scheduledMinuteOfDay == minute
        }.id
    }

    private suspend fun confirmAt(medicineId: Long, time: LocalDateTime, minute: Int): ConfirmOutcome {
        clock.set(time)
        return repo.confirmDose(doseOn(medicineId, time.toLocalDate(), minute))
    }

    private suspend fun rewards(patientId: Long): RewardsEntity = checkNotNull(db.rewards().get(patientId))

    // ---------------------------------------------------------------- section 6

    @Test
    fun confirmingTheSameDoseTwiceAwardsPointsOnce() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        val first = confirmAt(a.medicineId, monday.atTime(8, 0), 480) as ConfirmOutcome.Confirmed
        assertEquals(35, first.earned) // 10 + 5 on time + 20 perfect day
        val doseId = doseOn(a.medicineId, monday, 480)
        assertEquals(ConfirmOutcome.Rejected(ConfirmCheck.NOT_PENDING), repo.confirmDose(doseId))
        assertEquals(35, rewards(a.patientId).totalPoints)
        assertEquals(1, db.awards().doseAwardCount(a.patientId))
    }

    @Test
    fun deletingAndReAddingAMedicineGivesNoExtraPoints() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        val first = confirmAt(a.medicineId, monday.atTime(7, 0), 480) as ConfirmOutcome.Confirmed
        assertEquals(30, first.earned) // 10 + 20, an hour early so no on-time bonus
        clock.set(monday.atTime(7, 10))
        repo.deleteMedicine(a.medicineId)
        clock.set(monday.atTime(7, 20))
        val again = add(PatientChoice.Existing(a.patientId), " panadol ", daily(480))
        val second = confirmAt(again.medicineId, monday.atTime(7, 30), 480) as ConfirmOutcome.Confirmed
        assertFalse(second.dosePaid)
        assertEquals(0, second.earned)
        assertEquals(30, rewards(a.patientId).totalPoints)
    }

    @Test
    fun perfectDayBonusFiresOncePerDay() = runBlocking {
        val p = PatientChoice.New("Omar", "40", Gender.FEMALE)
        val a = add(p, "Morning", daily(480))
        val b = add(PatientChoice.Existing(a.patientId), "Evening", daily(1200))
        assertEquals(15, (confirmAt(a.medicineId, monday.atTime(8, 0), 480) as ConfirmOutcome.Confirmed).earned)
        assertEquals(35, (confirmAt(b.medicineId, monday.atTime(20, 0), 1200) as ConfirmOutcome.Confirmed).earned)
        clock.set(monday.atTime(20, 30))
        val c = add(PatientChoice.Existing(a.patientId), "Night", daily(21 * 60))
        val third = confirmAt(c.medicineId, monday.atTime(21, 0), 1260) as ConfirmOutcome.Confirmed
        assertEquals(15, third.earned) // day complete again, but no second +20
        assertEquals(0, third.perfectDayBonus)
        assertEquals(65, rewards(a.patientId).totalPoints)
        assertEquals(1, db.awards().perfectDayCount(a.patientId))
    }

    @Test
    fun milestoneFiresOncePerRunAndAgainOnlyForANewRun() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        val days = (0L..6L).map { monday.plusDays(it) }
        val earned = mutableListOf<Int>()
        for ((i, day) in days.withIndex()) {
            if (i == 3) continue // Thursday missed
            earned += (confirmAt(a.medicineId, day.atTime(8, 0), 480) as ConfirmOutcome.Confirmed).earned
        }
        assertEquals(listOf(35, 35, 85, 35, 35, 85), earned)
        assertEquals(310, rewards(a.patientId).totalPoints)
        val milestones = db.awards().milestones(a.patientId).map { LocalDate.ofEpochDay(it.streakStartEpochDay) to it.milestone }
        assertEquals(listOf(monday to 3, monday.plusDays(4) to 3), milestones)
    }

    @Test
    fun milestoneIsNotPaidTwiceWhenTheDayCompletesAgainOrIsFinalized() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        for (i in 0L..2L) confirmAt(a.medicineId, monday.plusDays(i).atTime(8, 0), 480)
        assertEquals(155, rewards(a.patientId).totalPoints)
        val wednesday = monday.plusDays(2)
        clock.set(wednesday.atTime(9, 0))
        val extra = add(PatientChoice.Existing(a.patientId), "Vitamin D", daily(1200))
        val again = confirmAt(extra.medicineId, wednesday.atTime(20, 0), 1200) as ConfirmOutcome.Confirmed
        assertEquals(15, again.earned)
        assertNull(again.milestone)
        // Thursday: Wednesday is finalized as PERFECT, nothing paid again.
        clock.set(monday.plusDays(3).atTime(6, 0))
        repo.refreshToday()
        assertEquals(170, rewards(a.patientId).totalPoints)
        assertEquals(1, db.awards().milestones(a.patientId).size)
        assertEquals(3, rewards(a.patientId).currentStreak)
    }

    @Test
    fun doseCanBeConfirmedAt2359ButIsMissedAtMidnight() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Night pill", daily(23 * 60))
        val late = confirmAt(a.medicineId, monday.atTime(23, 59), 1380) as ConfirmOutcome.Confirmed
        assertEquals(30, late.earned) // 59 minutes after: no on-time bonus
        clock.set(monday.plusDays(1).atTime(12, 0))
        val tuesdayDose = doseOn(a.medicineId, monday.plusDays(1), 1380)
        clock.set(monday.plusDays(2).atTime(0, 0)) // Wednesday 00:00, Tuesday's dose not taken
        val result = repo.confirmDose(tuesdayDose)
        assertTrue(result is ConfirmOutcome.Rejected)
        assertEquals("MISSED", db.doses().get(tuesdayDose)!!.status)
        assertEquals(0, db.doses().get(tuesdayDose)!!.pointsAwarded)
        assertEquals(30, rewards(a.patientId).totalPoints)
    }

    @Test
    fun patientAsConfirmationNeverChangesPatientB() = runBlocking {
        val a = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(480))
        val b = add(PatientChoice.New("Yasmin", "7", Gender.FEMALE), "Panadol", daily(480))
        clock.set(monday.atTime(8, 0))
        repo.refreshToday()
        val bRewards = rewards(b.patientId)
        val bDoses = db.doses().all().filter { it.medicineId == b.medicineId }
        confirmAt(a.medicineId, monday.atTime(8, 0), 480)
        assertEquals(35, rewards(a.patientId).totalPoints)
        assertEquals(bRewards, rewards(b.patientId))
        assertEquals(bDoses, db.doses().all().filter { it.medicineId == b.medicineId })
        assertEquals(0, db.awards().perfectDayCount(b.patientId))
        val home = repo.observeHome(monday.toEpochDay()).first()
        assertTrue(home.patients.single { it.patient.id == a.patientId }.todayComplete)
        assertFalse(home.patients.single { it.patient.id == b.patientId }.todayComplete)
    }

    // ---------------------------------------------------------------- section 6b

    @Test
    fun dayWithoutDosesIsNeutralNeverPerfectNeverBreaking() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Weekly", listOf(Slot(1, 480)))
        confirmAt(a.medicineId, monday.atTime(8, 0), 480)
        for (i in 1L..6L) {
            clock.set(monday.plusDays(i).atTime(12, 0))
            repo.refreshToday()
        }
        val sunday = rewards(a.patientId)
        assertEquals(1, sunday.currentStreak)
        assertNull(sunday.brokenStreakPending)
        assertEquals(35, sunday.totalPoints)
        assertEquals(1, db.awards().perfectDayCount(a.patientId))
        val nextMonday = confirmAt(a.medicineId, monday.plusDays(7).atTime(8, 0), 480) as ConfirmOutcome.Confirmed
        assertEquals(2, nextMonday.liveStreak)
        assertEquals(70, rewards(a.patientId).totalPoints)
    }

    @Test
    fun missedMondayBreaksTheStreakOfAMondayOnlyMedicine() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Weekly", listOf(Slot(1, 480)))
        confirmAt(a.medicineId, monday.atTime(8, 0), 480)
        clock.set(monday.plusDays(8).atTime(12, 0)) // next Monday passed without the pill
        repo.refreshToday()
        val r = rewards(a.patientId)
        assertEquals(0, r.currentStreak)
        assertEquals(1, r.bestStreak)
        assertEquals(1, r.brokenStreakPending)
    }

    @Test
    fun missedDayBreaksStreakKeepsBestAndPointsAndAnimatesOnce() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        confirmAt(a.medicineId, monday.atTime(8, 0), 480)
        confirmAt(a.medicineId, monday.plusDays(1).atTime(8, 0), 480)
        clock.set(monday.plusDays(3).atTime(9, 0)) // Wednesday missed
        repo.refreshToday()
        val broken = rewards(a.patientId)
        assertEquals(0, broken.currentStreak)
        assertEquals(2, broken.bestStreak)
        assertEquals(2, broken.brokenStreakPending)
        assertEquals(70, broken.totalPoints)
        repo.acknowledgeStreakBreak(a.patientId)
        assertNull(rewards(a.patientId).brokenStreakPending)
        repo.refreshToday()
        repo.refreshToday()
        assertNull(rewards(a.patientId).brokenStreakPending) // never replays
        assertEquals(70, rewards(a.patientId).totalPoints)
    }

    @Test
    fun severalDaysAwayGiveOneBreakAndOneAnimation() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        for (i in 0L..2L) confirmAt(a.medicineId, monday.plusDays(i).atTime(8, 0), 480)
        clock.set(monday.plusDays(7).atTime(9, 0)) // four days away
        repo.refreshToday()
        val r = rewards(a.patientId)
        assertEquals(3, r.brokenStreakPending)
        assertEquals(3, r.bestStreak)
        assertEquals(0, r.currentStreak)
        assertEquals(4, db.doses().all().count { it.status == "MISSED" })
        repo.acknowledgeStreakBreak(a.patientId)
        clock.set(monday.plusDays(8).atTime(9, 0)) // another missed day
        repo.refreshToday()
        assertNull(rewards(a.patientId).brokenStreakPending)
    }

    @Test
    fun evaluationIsIdempotent() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        confirmAt(a.medicineId, monday.atTime(8, 0), 480)
        clock.set(monday.plusDays(3).atTime(9, 0))
        repo.refreshToday()
        val once = rewards(a.patientId)
        val dosesOnce = db.doses().all()
        repo.refreshToday()
        TrackerRepository(db, clock, PinHasher(iterations = 1_000)).refreshToday()
        assertEquals(once, rewards(a.patientId))
        assertEquals(dosesOnce, db.doses().all())
    }

    @Test
    fun clockMovedBackwardGivesNoNegativeStreakAndNoDoubleBonus() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        confirmAt(a.medicineId, monday.atTime(8, 0), 480)
        confirmAt(a.medicineId, monday.plusDays(1).atTime(8, 0), 480)
        val before = rewards(a.patientId)
        clock.set(monday.atTime(12, 0)) // back to Monday
        repo.refreshToday()
        assertEquals(before, rewards(a.patientId))
        val home = repo.observeHome(monday.toEpochDay()).first().patients.single()
        assertEquals(1, home.liveStreak)
        clock.set(monday.minusDays(3).atTime(12, 0)) // before the patient existed
        repo.refreshToday()
        assertEquals(before, rewards(a.patientId))
        clock.set(monday.plusDays(1).atTime(12, 0)) // forward again
        repo.refreshToday()
        assertEquals(70, rewards(a.patientId).totalPoints)
        assertEquals(2, db.awards().perfectDayCount(a.patientId))
        clock.set(monday.plusDays(2).atTime(12, 0))
        repo.refreshToday()
        assertEquals(2, rewards(a.patientId).currentStreak)
        assertEquals(70, rewards(a.patientId).totalPoints)
    }

    @Test
    fun monthAndYearBoundaryKeepsTheStreak() = runBlocking {
        val dec30 = LocalDate.parse("2026-12-30")
        clock.set(dec30.atTime(7, 0))
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(480))
        val earned = (0L..2L).map { (confirmAt(a.medicineId, dec30.plusDays(it).atTime(8, 0), 480) as ConfirmOutcome.Confirmed).earned }
        assertEquals(listOf(35, 35, 85), earned)
        assertEquals(LocalDate.parse("2027-01-01"), dec30.plusDays(2))
        clock.set(LocalDate.parse("2027-01-02").atTime(7, 0))
        repo.refreshToday()
        assertEquals(3, rewards(a.patientId).currentStreak)
    }

    @Test
    fun cairoDaylightSavingSwitchKeepsTheStreak() = runBlocking {
        // Egypt moves clocks forward at 00:00 on Friday 2026-04-24.
        val wednesday = LocalDate.parse("2026-04-22")
        clock.set(wednesday.atTime(7, 0))
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Panadol", daily(9 * 60))
        val earned = (0L..2L).map { (confirmAt(a.medicineId, wednesday.plusDays(it).atTime(9, 0), 540) as ConfirmOutcome.Confirmed).earned }
        assertEquals(listOf(35, 35, 85), earned)
        clock.set(LocalDate.parse("2026-04-25").atTime(7, 0))
        repo.refreshToday()
        assertEquals(3, rewards(a.patientId).currentStreak)
        assertNull(rewards(a.patientId).brokenStreakPending)
    }

    @Test
    fun timezoneChangeDoesNotBreakOrRepay() = runBlocking {
        val a = add(PatientChoice.New("Omar", "40", Gender.FEMALE), "Night pill", daily(23 * 60))
        confirmAt(a.medicineId, monday.atTime(23, 30), 1380)
        assertEquals(35, rewards(a.patientId).totalPoints)
        clock.setZone(ZoneId.of("Europe/London")) // 21:30 Monday there
        repo.refreshToday()
        assertEquals(35, rewards(a.patientId).totalPoints)
        clock.setZone(ZoneId.of("Asia/Tokyo")) // 05:30 Tuesday there: Monday is finalized
        repo.refreshToday()
        val r = rewards(a.patientId)
        assertEquals(1, r.currentStreak)
        assertEquals(35, r.totalPoints)
        assertNull(r.brokenStreakPending)
        clock.setZone(ZoneId.of("Africa/Cairo")) // back to Monday 23:30: nothing changes
        repo.refreshToday()
        assertEquals(r, rewards(a.patientId))
    }
}
