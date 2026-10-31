package com.example.tracker.domain

import java.time.LocalDate
import java.time.ZonedDateTime

/** One weekly time: ISO day of week (1 = Monday ... 7 = Sunday) and minute of day (0..1439). */
data class Slot(val dayOfWeek: Int, val minuteOfDay: Int) : Comparable<Slot> {
    override fun compareTo(other: Slot): Int =
        compareValuesBy(this, other, { it.dayOfWeek }, { it.minuteOfDay })

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

/** Everything the schedule math needs to know about one slot of one medicine. */
data class SlotSpec(
    val dayOfWeek: Int,
    val minuteOfDay: Int,
    /** The slot exists from this local day and minute on (when it was added). */
    val validFromEpochDay: Long,
    val validFromMinuteOfDay: Int,
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val active: Boolean,
)

object ScheduleRules {

    val ALL_DAYS: List<Int> = (1..7).toList()

    fun isoDayOfWeek(epochDay: Long): Int = LocalDate.ofEpochDay(epochDay).dayOfWeek.value

    fun inRange(epochDay: Long, startEpochDay: Long?, endEpochDay: Long?): Boolean =
        (startEpochDay == null || epochDay >= startEpochDay) &&
            (endEpochDay == null || epochDay <= endEpochDay)

    /**
     * True when [slot] produces a dose on [epochDay]: right weekday, inside the start/end range,
     * medicine active, and not before the moment the slot was added (a medicine added at 14:00
     * gets no dose for an 08:00 slot that same day).
     */
    fun producesDose(slot: SlotSpec, epochDay: Long): Boolean {
        if (!slot.active) return false
        if (!inRange(epochDay, slot.startEpochDay, slot.endEpochDay)) return false
        if (isoDayOfWeek(epochDay) != slot.dayOfWeek) return false
        if (epochDay < slot.validFromEpochDay) return false
        if (epochDay == slot.validFromEpochDay && slot.minuteOfDay < slot.validFromMinuteOfDay) return false
        return true
    }

    /**
     * Next moment strictly after [now] at which [slot] is due, in [now]'s zone, or null if it
     * never fires again (end date passed or medicine inactive). A slot time that falls into a
     * daylight saving gap is moved forward by the gap, as java.time does.
     */
    fun nextOccurrence(slot: SlotSpec, now: ZonedDateTime): ZonedDateTime? {
        if (!slot.active) return null
        val firstDay = maxOf(
            now.toLocalDate().toEpochDay(),
            slot.startEpochDay ?: Long.MIN_VALUE,
            slot.validFromEpochDay,
        )
        if (slot.endEpochDay != null && firstDay > slot.endEpochDay) return null
        // 8 days cover "today but already past" plus a full week.
        for (offset in 0L..7L) {
            val day = firstDay + offset
            if (!producesDose(slot, day)) continue
            val candidate = LocalDate.ofEpochDay(day)
                .atTime(slot.minuteOfDay / 60, slot.minuteOfDay % 60)
                .atZone(now.zone)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    /** Expands a simple schedule (same times on each chosen day) into slots. */
    fun expand(days: Collection<Int>, minutes: Collection<Int>): Set<Slot> =
        days.flatMap { d -> minutes.map { m -> Slot(d, m) } }.toSortedSet()
}
