package com.example.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The final dose of a dated course (batch 6, R6), from the schedule, with fixed dates. */
class CourseEndTest {

    /** 2026-10-12 is a Monday. */
    private fun day(dayOfMonth: Int): Long = LocalDate.of(2026, 10, dayOfMonth).toEpochDay()

    private fun slot(dayOfWeek: Int, minute: Int, start: Long?, end: Long?, active: Boolean = true) = SlotSpec(
        dayOfWeek = dayOfWeek,
        minuteOfDay = minute,
        validFromEpochDay = start ?: day(1),
        validFromMinuteOfDay = 0,
        startEpochDay = start,
        endEpochDay = end,
        active = active,
    )

    private fun daily(start: Long?, end: Long?, vararg minutes: Int) =
        (1..7).flatMap { d -> minutes.map { m -> slot(d, m, start, end) } }

    @Test
    fun weekdayNumbersAreIso() {
        assertEquals(1, ScheduleRules.isoDayOfWeek(day(12)))
        assertEquals(7, ScheduleRules.isoDayOfWeek(day(18)))
    }

    @Test
    fun aDailyCourseEndsWithTheDoseOnTheEndDay() {
        val course = daily(day(12), day(18), 540)
        assertEquals(day(18) to 540, CourseEnd.lastDose(course))
        assertTrue(CourseEnd.isFinalDose(course, day(18), 540))
        // Every earlier dose is not the end.
        assertFalse(CourseEnd.isFinalDose(course, day(17), 540))
        assertFalse(CourseEnd.isFinalDose(course, day(12), 540))
    }

    @Test
    fun withTwoDosesOnTheLastDayOnlyTheSecondEndsIt() {
        val course = daily(day(12), day(18), 540, 1260)
        assertFalse("09:00 on the last day", CourseEnd.isFinalDose(course, day(18), 540))
        assertTrue("21:00 on the last day", CourseEnd.isFinalDose(course, day(18), 1260))
    }

    @Test
    fun aWeekdayCourseEndsWithItsLastDoseBeforeTheEndDate() {
        // Monday, Wednesday and Friday at 08:00, ending on a Sunday: the Friday dose is the last.
        val course = listOf(1, 3, 5).map { slot(it, 480, day(12), day(18)) }
        assertEquals(day(16) to 480, CourseEnd.lastDose(course))
        assertTrue(CourseEnd.isFinalDose(course, day(16), 480))
        assertFalse(CourseEnd.isFinalDose(course, day(14), 480))
        assertFalse("no dose on the end day itself", CourseEnd.isFinalDose(course, day(18), 480))
    }

    @Test
    fun noEndDateNeverEnds() {
        val ongoing = daily(day(12), null, 540)
        assertNull(CourseEnd.lastDose(ongoing))
        for (d in 12..31) assertFalse(CourseEnd.isFinalDose(ongoing, day(d), 540))
        assertFalse(CourseEnd.celebrates(ongoing, day(18), 540, confirmed = true))
    }

    @Test
    fun anEndDateExtendedLaterMovesTheEnd() {
        val before = daily(day(12), day(18), 540)
        val after = daily(day(12), day(25), 540)
        assertTrue(CourseEnd.isFinalDose(before, day(18), 540))
        assertFalse("the old last dose is no longer final", CourseEnd.isFinalDose(after, day(18), 540))
        assertTrue(CourseEnd.isFinalDose(after, day(25), 540))
    }

    @Test
    fun aMissedFinalDoseNeverCelebrates() {
        val course = daily(day(12), day(18), 540)
        assertFalse(CourseEnd.celebrates(course, day(18), 540, confirmed = false))
        assertTrue(CourseEnd.celebrates(course, day(18), 540, confirmed = true))
    }

    @Test
    fun anotherMedicineAndAnotherPatientAreNotAffected() {
        val courseA = daily(day(12), day(18), 540)
        // The control: patient A's other medicine, ongoing, at the same time on the same day.
        val otherMedicine = daily(day(12), null, 540)
        // Patient B's own course ends later.
        val otherPatient = daily(day(10), day(20), 540)
        assertTrue(CourseEnd.isFinalDose(courseA, day(18), 540))
        assertFalse(CourseEnd.isFinalDose(otherMedicine, day(18), 540))
        assertFalse(CourseEnd.isFinalDose(otherPatient, day(18), 540))
        assertTrue(CourseEnd.isFinalDose(otherPatient, day(20), 540))
    }

    @Test
    fun aCourseWithoutAnyDoseInItsRangeOrAStoppedMedicineHasNoEnd() {
        // Monday only, but the range runs Tuesday to Thursday.
        assertNull(CourseEnd.lastDose(listOf(slot(1, 480, day(13), day(15)))))
        assertNull(CourseEnd.lastDose(daily(day(12), day(18), 540).map { it.copy(active = false) }))
    }
}
