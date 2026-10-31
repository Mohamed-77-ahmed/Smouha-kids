package com.example.tracker.domain

/** Follow-up reminder timing. Pure, so the count and times are unit-tested. */
object ReminderRules {
    /** Follow-ups after the first reminder. */
    const val MAX_REPEATS = 3

    /** Catch-up delay when a follow-up is already overdue (for example after a reboot). */
    const val LATE_DELAY_MILLIS = 5_000L

    /**
     * When follow-up number [remindersSent] + 1 is due: first reminder time plus that many
     * intervals. Null once all 3 follow-ups were sent.
     */
    fun repeatDueAt(notifiedAtMillis: Long, remindersSent: Int, intervalMinutes: Int): Long? {
        if (remindersSent >= MAX_REPEATS) return null
        return notifiedAtMillis + (remindersSent + 1) * intervalMinutes * 60_000L
    }

    /** An overdue follow-up fires shortly after now instead of in the past. */
    fun triggerAt(dueMillis: Long, nowMillis: Long): Long = maxOf(dueMillis, nowMillis + LATE_DELAY_MILLIS)
}
