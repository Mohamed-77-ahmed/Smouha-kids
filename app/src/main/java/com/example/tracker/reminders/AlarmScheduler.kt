package com.example.tracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.tracker.data.CancelRefs
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.DoseRow
import com.example.tracker.data.db.SlotScheduleRow
import com.example.tracker.data.toSpec
import com.example.tracker.domain.ReminderRules
import com.example.tracker.domain.ScheduleRules
import com.example.tracker.domain.SoundPolicy
import java.time.LocalDate
import java.time.LocalTime

/**
 * One alarm per slot for its next occurrence only; when it fires the next one is booked.
 * Follow-up reminders get their own alarm per dose. Everything is re-armed from the DB on
 * every launch, after boot, time and timezone changes, and after an app update.
 */
class AlarmScheduler(private val context: Context, private val repository: TrackerRepository) {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Idempotent: every slot gets exactly its next occurrence, running follow-ups continue. */
    suspend fun rearmAll() {
        for (row in repository.allSlotSchedules()) scheduleSlot(row)
        for (dose in repository.dosesWithRunningRepeats()) scheduleRepeat(dose)
        scheduleDailyEvaluation()
    }

    fun scheduleSlot(row: SlotScheduleRow) {
        val next = ScheduleRules.nextOccurrence(row.toSpec(), repository.nowZoned())
        if (next == null) {
            cancelSlot(row.slotId)
            return
        }
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_SLOT)
            .putExtra(EXTRA_SLOT_ID, row.slotId)
            .putExtra(EXTRA_OCCURRENCE_DAY, next.toLocalDate().toEpochDay())
        set(next.toInstant().toEpochMilli(), broadcast(ACTION_SLOT, row.slotId, intent))
    }

    fun cancelSlot(slotId: Long) {
        alarmManager.cancel(broadcast(ACTION_SLOT, slotId, Intent(context, ReminderReceiver::class.java).setAction(ACTION_SLOT)))
    }

    /** Next follow-up of a dose that was announced: notifiedAt + (sent + 1) * interval. */
    fun scheduleRepeat(dose: DoseRow) {
        val notifiedAt = dose.notifiedAtMillis ?: return
        val due = ReminderRules.repeatDueAt(notifiedAt, dose.remindersSent, dose.reminderIntervalMinutes) ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REPEAT)
            .putExtra(EXTRA_DOSE_ID, dose.doseId)
        set(ReminderRules.triggerAt(due, repository.nowMillis()), broadcast(ACTION_REPEAT, dose.doseId, intent))
    }

    fun cancelRepeat(doseId: Long) {
        alarmManager.cancel(broadcast(ACTION_REPEAT, doseId, Intent(context, ReminderReceiver::class.java).setAction(ACTION_REPEAT)))
    }

    /** After a dose is confirmed (or deleted): no more follow-ups, no sound and no notification left behind. */
    fun onDoseTaken(doseId: Long) {
        cancelRepeat(doseId)
        AlarmSound.stop(context, SoundPolicy.StopReason.CONFIRMED, Notifications.notificationId(doseId))
        Notifications.cancel(context, doseId)
    }

    fun cancel(refs: CancelRefs) {
        refs.slotIds.forEach { cancelSlot(it) }
        refs.doseIds.forEach { onDoseTaken(it) }
    }

    fun scheduleTest() {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_TEST)
        set(repository.nowMillis() + TEST_DELAY_MILLIS, broadcast(ACTION_TEST, 0, intent))
    }

    /** Finalizes yesterday shortly after midnight even when the app is not opened. */
    fun scheduleDailyEvaluation() {
        val now = repository.nowZoned()
        val next = LocalDate.from(now).plusDays(1).atTime(LocalTime.of(0, 5)).atZone(now.zone)
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_DAILY)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            broadcast(ACTION_DAILY, 0, intent),
        )
    }

    private fun set(triggerAtMillis: Long, operation: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        } else {
            // Without exact-alarm permission Android may deliver late; the home banner says so.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    // Request code from the slot or dose id; the action keeps the kinds apart.
    private fun broadcast(action: String, id: Long, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (id and 0x7FFFFFFF).toInt(),
            intent.setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_SLOT = "com.smouha.kidsreminder.action.SLOT"
        const val ACTION_REPEAT = "com.smouha.kidsreminder.action.REPEAT"
        const val ACTION_TEST = "com.smouha.kidsreminder.action.TEST"
        const val ACTION_DAILY = "com.smouha.kidsreminder.action.DAILY"
        const val EXTRA_SLOT_ID = "slot_id"
        const val EXTRA_OCCURRENCE_DAY = "occurrence_day"
        const val EXTRA_DOSE_ID = "dose_id"
        private const val TEST_DELAY_MILLIS = 10_000L
    }
}
