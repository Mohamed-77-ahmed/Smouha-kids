package com.example.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderRulesTest {

    private val notified = 1_000_000L
    private val minute = 60_000L

    @Test
    fun followUpsComeAtEachIntervalAfterTheFirstReminder() {
        assertEquals(notified + 10 * minute, ReminderRules.repeatDueAt(notified, 0, 10))
        assertEquals(notified + 20 * minute, ReminderRules.repeatDueAt(notified, 1, 10))
        assertEquals(notified + 30 * minute, ReminderRules.repeatDueAt(notified, 2, 10))
    }

    @Test
    fun exactlyThreeFollowUpsThenNone() {
        assertEquals(3, ReminderRules.MAX_REPEATS)
        assertNull(ReminderRules.repeatDueAt(notified, 3, 10))
        assertNull(ReminderRules.repeatDueAt(notified, 4, 10))
    }

    @Test
    fun everyAllowedIntervalIsHonored() {
        for (interval in listOf(5, 10, 15, 30)) {
            assertEquals(notified + 2 * interval * minute, ReminderRules.repeatDueAt(notified, 1, interval))
        }
    }

    @Test
    fun overdueFollowUpFiresFiveSecondsFromNowNotInThePast() {
        val now = notified + 45 * minute
        val due = ReminderRules.repeatDueAt(notified, 1, 10)!!
        assertEquals(now + 5_000L, ReminderRules.triggerAt(due, now))
        // A follow-up still in the future keeps its own time.
        assertEquals(notified + 20 * minute, ReminderRules.triggerAt(notified + 20 * minute, notified + 11 * minute))
    }

    @Test
    fun countResumesFromWhatWasSentSoAKillDoesNotRestartIt() {
        // Two follow-ups were sent before the process died: the next one is the third, not the first.
        assertEquals(notified + 3 * 15 * minute, ReminderRules.repeatDueAt(notified, 2, 15))
    }
}
