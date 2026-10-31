package com.example.tracker.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.tracker.app
import kotlinx.coroutines.launch

/** Alarms are lost on reboot and must move with time or timezone changes and app updates. */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val pending = goAsync()
        val app = context.app
        app.appScope.launch {
            try {
                app.repository.refreshToday()
                app.alarms.rearmAll()
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED = buildSet {
            add(Intent.ACTION_BOOT_COMPLETED)
            add(Intent.ACTION_TIME_CHANGED)
            add(Intent.ACTION_TIMEZONE_CHANGED)
            add(Intent.ACTION_MY_PACKAGE_REPLACED)
            // Exact-alarm permission exists from Android 12 on.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
            }
        }
    }
}
