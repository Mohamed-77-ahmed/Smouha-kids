package com.example.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RewardsTest {

    // ---------------------------------------------------------------- points

    @Test
    fun dosePointsOnTimeWindowIsThirtyMinutesEitherSideInclusive() {
        val eight = 8 * 60
        val at = { h: Int, m: Int, s: Int -> h * 3600 + m * 60 + s }
        assertEquals(DosePoints(10, 5), PointsRules.dosePoints(eight, at(8, 0, 0)))
        assertEquals(DosePoints(10, 5), PointsRules.dosePoints(eight, at(8, 30, 0)))
        assertEquals(DosePoints(10, 0), PointsRules.dosePoints(eight, at(8, 30, 1)))
        assertEquals(DosePoints(10, 5), PointsRules.dosePoints(eight, at(7, 30, 0)))
        assertEquals(DosePoints(10, 0), PointsRules.dosePoints(eight, at(7, 29, 59)))
        assertEquals(15, PointsRules.dosePoints(eight, at(8, 10, 0)).total)
        assertEquals(10, PointsRules.dosePoints(eight, at(23, 0, 0)).total)
    }

    @Test
    fun levelIsFloorOfPointsOverHundredPlusOne() {
        assertEquals(1, PointsRules.level(0))
        assertEquals(1, PointsRules.level(99))
        assertEquals(2, PointsRules.level(100))
        assertEquals(3, PointsRules.level(250))
        assertEquals(100, PointsRules.pointsToNextLevel(0))
        assertEquals(1, PointsRules.pointsToNextLevel(99))
        assertEquals(100, PointsRules.pointsToNextLevel(100))
        assertEquals(50, PointsRules.pointsToNextLevel(250))
    }

    @Test
    fun milestonesAreExactlyThreeSevenFourteenThirtySixtyHundred() {
        val hits = (1..120).mapNotNull { PointsRules.milestoneFor(it) }
        assertEquals(listOf(3, 7, 14, 30, 60, 100), hits)
        assertNull(PointsRules.milestoneFor(4))
        assertEquals(50, PointsRules.STREAK_MILESTONE_BONUS)
        assertEquals(20, PointsRules.PERFECT_DAY_BONUS)
    }

    // ---------------------------------------------------------------- confirm window

    @Test
    fun doseConfirmableOnlyWhilePendingOnItsOwnDay() {
        val day = LocalDate.parse("2026-10-12").toEpochDay()
        assertEquals(ConfirmCheck.OK, DoseRules.canConfirm(DoseStatus.PENDING, day, day))
        assertEquals(ConfirmCheck.EXPIRED, DoseRules.canConfirm(DoseStatus.PENDING, day, day + 1))
        assertEquals(ConfirmCheck.NOT_YET, DoseRules.canConfirm(DoseStatus.PENDING, day + 1, day))
        assertEquals(ConfirmCheck.NOT_PENDING, DoseRules.canConfirm(DoseStatus.TAKEN, day, day))
        assertEquals(ConfirmCheck.NOT_PENDING, DoseRules.canConfirm(DoseStatus.MISSED, day, day))
    }

    // ---------------------------------------------------------------- streak

    private val d0 = LocalDate.parse("2026-10-01").toEpochDay()
    private fun start() = StreakState(0, null, 0, d0 - 1, null)
    private fun run(state: StreakState, vararg results: DayResult): StreakState =
        results.fold(state) { s, r -> StreakRules.finalizeDay(s, s.lastEvaluatedEpochDay + 1, r) }

    @Test
    fun dayResultFromDoseStatuses() {
        assertEquals(DayResult.NEUTRAL, StreakRules.dayResult(emptyList()))
        assertEquals(DayResult.PERFECT, StreakRules.dayResult(listOf(DoseStatus.TAKEN, DoseStatus.TAKEN)))
        assertEquals(DayResult.MISSED, StreakRules.dayResult(listOf(DoseStatus.TAKEN, DoseStatus.MISSED)))
        assertEquals(DayResult.MISSED, StreakRules.dayResult(listOf(DoseStatus.PENDING)))
        assertFalse(StreakRules.isComplete(emptyList()))
        assertTrue(StreakRules.isComplete(listOf(DoseStatus.TAKEN)))
    }

    @Test
    fun threePerfectDaysMakeAStreakOfThreeFromTheFirstDay() {
        val s = run(start(), DayResult.PERFECT, DayResult.PERFECT, DayResult.PERFECT)
        assertEquals(StreakState(3, d0, 3, d0 + 2, null), s)
    }

    @Test
    fun missedDayBreaksStreakKeepsBestAndQueuesOneAnimation() {
        val s = run(start(), DayResult.PERFECT, DayResult.PERFECT, DayResult.MISSED)
        assertEquals(StreakState(0, null, 2, d0 + 2, 2), s)
    }

    @Test
    fun severalMissedDaysQueueOnlyOneAnimationForTheStreakThatBroke() {
        val s = run(start(), DayResult.PERFECT, DayResult.PERFECT, DayResult.PERFECT, DayResult.MISSED, DayResult.MISSED, DayResult.MISSED)
        assertEquals(0, s.currentStreak)
        assertEquals(3, s.bestStreak)
        assertEquals(3, s.brokenStreakPending)
    }

    @Test
    fun neutralDayNeitherExtendsNorBreaks() {
        val s = run(start(), DayResult.PERFECT, DayResult.NEUTRAL, DayResult.NEUTRAL, DayResult.PERFECT)
        assertEquals(StreakState(2, d0, 2, d0 + 3, null), s)
    }

    @Test
    fun missingWithoutAStreakQueuesNoAnimation() {
        val s = run(start(), DayResult.MISSED, DayResult.MISSED)
        assertNull(s.brokenStreakPending)
    }

    @Test
    fun bestStreakNeverDecreasesAndANewRunHasANewStart() {
        val s = run(start(), DayResult.PERFECT, DayResult.PERFECT, DayResult.PERFECT, DayResult.MISSED, DayResult.PERFECT)
        assertEquals(1, s.currentStreak)
        assertEquals(3, s.bestStreak)
        assertEquals(d0 + 4, s.streakStartEpochDay)
    }

    @Test
    fun monthAndYearBoundaryIsJustTheNextDay() {
        val dec30 = LocalDate.parse("2026-12-30").toEpochDay()
        var s = StreakState(0, null, 0, dec30 - 1, null)
        s = StreakRules.finalizeDay(s, dec30, DayResult.PERFECT)
        s = StreakRules.finalizeDay(s, dec30 + 1, DayResult.PERFECT) // Dec 31
        s = StreakRules.finalizeDay(s, dec30 + 2, DayResult.PERFECT) // Jan 1, 2027
        assertEquals(LocalDate.parse("2027-01-01").toEpochDay(), s.lastEvaluatedEpochDay)
        assertEquals(3, s.currentStreak)
    }

    @Test(expected = IllegalArgumentException::class)
    fun daysCannotBeSkippedOrRepeated() {
        StreakRules.finalizeDay(start(), d0 + 1, DayResult.PERFECT)
    }

    @Test
    fun liveStreakCountsTodayOnlyWhenCompleteAndNotYetFinalized() {
        val s = run(start(), DayResult.PERFECT, DayResult.PERFECT)
        val today = d0 + 2
        assertEquals(3, StreakRules.liveStreak(s, today, todayComplete = true))
        assertEquals(2, StreakRules.liveStreak(s, today, todayComplete = false))
        // Clock moved back to an already finalized day: never counted twice.
        assertEquals(2, StreakRules.liveStreak(s, d0 + 1, todayComplete = true))
        assertFalse(StreakRules.isOpenDay(s, d0 + 1))
    }

    @Test
    fun runStartIsTodayForANewRunOtherwiseTheStoredStart() {
        assertEquals(d0 + 5, StreakRules.runStartIfComplete(start().copy(lastEvaluatedEpochDay = d0 + 4), d0 + 5))
        val s = run(start(), DayResult.PERFECT, DayResult.PERFECT)
        assertEquals(d0, StreakRules.runStartIfComplete(s, d0 + 2))
    }
}
