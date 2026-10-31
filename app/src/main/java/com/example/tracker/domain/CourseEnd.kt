package com.example.tracker.domain

/**
 * The end of a dated course (batch 6, R6). A medicine with an end date reaches its end when its
 * FINAL scheduled dose is confirmed: the last dose occurrence inside its start/end range. Normally
 * that is the dose on the end day; with two doses on the last day only the later one; with a
 * weekday schedule that has no dose on the end day, the last dose before it. No end date: never.
 * Pure: read from the schedule (the same rule that creates the doses), not from the dose list.
 */
object CourseEnd {

    /** (epoch day, minute of day) of the course's last dose; null without an end date or a dose in range. */
    fun lastDose(slots: List<SlotSpec>): Pair<Long, Int>? {
        val end = slots.mapNotNull { it.endEpochDay }.maxOrNull() ?: return null
        // A weekly schedule repeats every 7 days, so the last dose lies in the range's last week.
        for (day in end downTo end - 6) {
            val minute = slots.filter { ScheduleRules.producesDose(it, day) }.maxOfOrNull { it.minuteOfDay }
            if (minute != null) return day to minute
        }
        return null
    }

    /** True when the dose at [epochDay] and [minuteOfDay] is its course's last dose. */
    fun isFinalDose(slots: List<SlotSpec>, epochDay: Long, minuteOfDay: Int): Boolean =
        lastDose(slots) == (epochDay to minuteOfDay)

    /** The course-complete screen: only for a final dose that was really confirmed (a missed one never is). */
    fun celebrates(slots: List<SlotSpec>, epochDay: Long, minuteOfDay: Int, confirmed: Boolean): Boolean =
        confirmed && isFinalDose(slots, epochDay, minuteOfDay)
}
