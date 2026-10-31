package com.example.tracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.tracker.PillPalsApp
import com.example.tracker.app
import com.example.tracker.data.ReminderInfo
import com.example.tracker.domain.DoseStatus
import kotlinx.coroutines.launch

/** Handles every alarm of the app. Reads all state from the DB, so it works after process death. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.app
        app.appScope.launch {
            try {
                handle(app, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(app: PillPalsApp, intent: Intent) {
        val repository = app.repository
        val alarms = app.alarms
        val settings = repository.settings()
        val language = settings.languageTag
        val alarmSound = settings.alarmSoundEnabled
        when (intent.action) {
            AlarmScheduler.ACTION_SLOT -> {
                val slotId = intent.getLongExtra(AlarmScheduler.EXTRA_SLOT_ID, -1)
                val day = intent.getLongExtra(AlarmScheduler.EXTRA_OCCURRENCE_DAY, Long.MIN_VALUE)
                repository.onSlotDue(slotId, day)?.let { info ->
                    announce(app, info, followUp = false, language = language, alarmSound = alarmSound)
                    alarms.scheduleRepeat(info.dose)
                }
                // Book the next occurrence of this slot (null when the slot was deleted).
                repository.slotSchedule(slotId)?.let { alarms.scheduleSlot(it) }
            }
            AlarmScheduler.ACTION_REPEAT -> {
                val doseId = intent.getLongExtra(AlarmScheduler.EXTRA_DOSE_ID, -1)
                repository.onRepeatDue(doseId)?.let { info ->
                    announce(app, info, followUp = true, language = language, alarmSound = alarmSound)
                    if (info.moreRepeatsLeft) alarms.scheduleRepeat(info.dose)
                }
            }
            AlarmScheduler.ACTION_TEST -> {
                Notifications.ensureChannel(app, language)
                AlarmSound.announce(app, Notifications.testContent(app, language), dosePending = true, alarmSoundOn = alarmSound)
            }
            AlarmScheduler.ACTION_DAILY -> {
                repository.refreshToday()
                alarms.rearmAll()
            }
        }
    }

    /** First reminder and follow-ups: the app's alarm sound, or the channel the setting picks (CLAUDE.md 7). */
    private fun announce(app: PillPalsApp, info: ReminderInfo, followUp: Boolean, language: String?, alarmSound: Boolean) {
        Notifications.ensureChannel(app, language)
        AlarmSound.announce(
            app,
            Notifications.content(app, info, followUp, language),
            dosePending = info.dose.status == DoseStatus.PENDING.name,
            alarmSoundOn = alarmSound,
        )
    }
}
