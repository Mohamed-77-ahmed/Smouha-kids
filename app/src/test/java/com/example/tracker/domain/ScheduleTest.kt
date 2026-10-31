package com.example.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ScheduleTest {

    // 2026-10-12 is a Monday, 2026-10-18 the following Sunday.
    private val monday = LocalDate.parse("2026-10-12").toEpochDay()
    private val week = (0L..6L).map { monday + it }
    private val longAgo = LocalDate.parse("2026-01-01").toEpochDay()

    private fun spec(
        day: Int,
        minute: Int,
        start: Long? = null,
        end: Long? = null,
        validFromDay: Long = longAgo,
        validFromMinute: Int = 0,
        active: Boolean = true,
    ) = SlotSpec(day, minute, validFromDay, validFromMinute, start, end, active)

    private fun dosesOn(slots: List<SlotSpec>, epochDay: Long) =
        slots.filter { ScheduleRules.producesDose(it, epochDay) }.map { it.minuteOfDay }

    @Test
    fun isoWeekdayMondayIsOneSundayIsSeven() {
        assertEquals(1, ScheduleRules.isoDayOfWeek(monday))
        assertEquals(2, ScheduleRules.isoDayOfWeek(monday + 1))
        assertEquals(6, ScheduleRules.isoDayOfWeek(monday + 5))
        assertEquals(7, ScheduleRules.isoDayOfWeek(monday + 6))
    }

    @Test
    fun mondayOnlyMedicineHasDoseOnlyOnMonday_dailyControlHasSeven() {
        val mondayOnly = listOf(spec(1, 8 * 60))
        val daily = (1..7).map { spec(it, 8 * 60) }
        assertEquals(listOf(listOf(480), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
            week.map { dosesOn(mondayOnly, it) })
        assertEquals(List(7) { listOf(480) }, week.map { dosesOn(daily, it) })
    }

    @Test
    fun differentTimesPerDayUseTheirOwnTime() {
        // Saturday 09:00 and Tuesday 21:00.
        val slots = listOf(spec(6, 9 * 60), spec(2, 21 * 60))
        assertEquals(listOf(1260), dosesOn(slots, monday + 1)) // Tuesday
        assertEquals(listOf(540), dosesOn(slots, monday + 5)) // Saturday
        assertEquals(emptyList<Int>(), dosesOn(slots, monday)) // Monday
        assertEquals(emptyList<Int>(), dosesOn(slots, monday + 6)) // Sunday
    }

    @Test
    fun startAndEndDatesLimitDoses() {
        val wednesday = monday + 2
        val friday = monday + 4
        val daily = (1..7).map { spec(it, 600, start = wednesday, end = friday) }
        assertEquals(listOf(0, 0, 1, 1, 1, 0, 0), week.map { dosesOn(daily, it).size })
    }

    @Test
    fun noAlarmBeforeStartAndNoneAfterEnd() {
        val wednesday = monday + 2
        val friday = monday + 4
        val slot = spec(1, 600, start = wednesday, end = friday) // Monday slot, but the range has no Monday
        val now = LocalDate.ofEpochDay(monday).atTime(7, 0).atZone(ZoneOffset.UTC)
        assertNull(ScheduleRules.nextOccurrence(slot, now))

        val fridaySlot = spec(5, 600, start = wednesday, end = friday)
        assertEquals(
            LocalDateTime.of(2026, 10, 16, 10, 0),
            ScheduleRules.nextOccurrence(fridaySlot, now)?.toLocalDateTime(),
        )
        val afterEnd = LocalDate.ofEpochDay(friday).atTime(10, 1).atZone(ZoneOffset.UTC)
        assertNull(ScheduleRules.nextOccurrence(fridaySlot, afterEnd))
    }

    @Test
    fun slotAddedAfterItsTimeSkipsThatDay() {
        // Medicine added Monday 14:00: the 08:00 slot starts next Monday, the 20:00 slot today.
        val morning = spec(1, 8 * 60, validFromDay = monday, validFromMinute = 14 * 60)
        val evening = spec(1, 20 * 60, validFromDay = monday, validFromMinute = 14 * 60)
        assertFalse(ScheduleRules.producesDose(morning, monday))
        assertTrue(ScheduleRules.producesDose(morning, monday + 7))
        assertTrue(ScheduleRules.producesDose(evening, monday))
    }

    @Test
    fun inactiveMedicineNeverProducesDosesOrAlarms() {
        val slot = spec(1, 600, active = false)
        assertFalse(ScheduleRules.producesDose(slot, monday))
        assertNull(ScheduleRules.nextOccurrence(slot, LocalDate.ofEpochDay(monday).atStartOfDay(ZoneOffset.UTC)))
    }

    @Test
    fun nextOccurrenceIsStrictlyAfterNow() {
        val slot = spec(1, 8 * 60)
        val atSlot = LocalDate.ofEpochDay(monday).atTime(8, 0).atZone(ZoneOffset.UTC)
        assertEquals(LocalDateTime.of(2026, 10, 19, 8, 0), ScheduleRules.nextOccurrence(slot, atSlot)?.toLocalDateTime())
        val before = LocalDate.ofEpochDay(monday).atTime(7, 59).atZone(ZoneOffset.UTC)
        assertEquals(LocalDateTime.of(2026, 10, 12, 8, 0), ScheduleRules.nextOccurrence(slot, before)?.toLocalDateTime())
    }

    @Test
    fun cairoSpringForwardGapMovesSlotForwardAndKeepsDailyLocalTime() {
        val cairo = ZoneId.of("Africa/Cairo")
        // 2026-04-24 is the last Friday of April: 00:00 jumps to 01:00.
        val thursdayNight = LocalDateTime.of(2026, 4, 23, 23, 0).atZone(cairo)
        val gapSlot = spec(5, 30) // Friday 00:30 does not exist that night
        val next = ScheduleRules.nextOccurrence(gapSlot, thursdayNight)!!
        assertEquals(LocalDateTime.of(2026, 4, 24, 1, 30), next.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(3), next.offset)

        val nineDaily = spec(5, 9 * 60)
        val thursdayMorning = LocalDateTime.of(2026, 4, 23, 9, 0).atZone(cairo)
        val friday = ScheduleRules.nextOccurrence(nineDaily, thursdayMorning)!!
        assertEquals(LocalDateTime.of(2026, 4, 24, 9, 0), friday.toLocalDateTime())
        // Only 23 real hours between Thursday 09:00 (+02) and Friday 09:00 (+03).
        assertEquals(23L * 3600, friday.toEpochSecond() - thursdayMorning.toEpochSecond())
    }

    @Test
    fun cairoFallBackOverlapFiresOnceAtTheEarlierOffset() {
        val cairo = ZoneId.of("Africa/Cairo")
        // Thursday 2026-10-29 ends at 24:00 +03 and 23:00-24:00 is repeated at +02.
        val slot = spec(4, 23 * 60 + 30)
        val evening = LocalDateTime.of(2026, 10, 29, 20, 0).atZone(cairo)
        val first = ScheduleRules.nextOccurrence(slot, evening)!!
        assertEquals(LocalDateTime.of(2026, 10, 29, 23, 30), first.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(3), first.offset)
        // After it fired, the next one is a week later, not the repeated hour.
        val afterFiring = first.plusSeconds(1)
        assertEquals(LocalDateTime.of(2026, 11, 5, 23, 30), ScheduleRules.nextOccurrence(slot, afterFiring)?.toLocalDateTime())
    }

    @Test
    fun expandBuildsEveryDayTimeCombination() {
        assertEquals(
            setOf(Slot(1, 480), Slot(1, 1200), Slot(3, 480), Slot(3, 1200)),
            ScheduleRules.expand(listOf(1, 3), listOf(480, 1200)),
        )
    }
}
