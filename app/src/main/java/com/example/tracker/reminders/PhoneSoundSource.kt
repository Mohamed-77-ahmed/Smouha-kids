package com.example.tracker.reminders

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import com.example.tracker.domain.SoundStatus

/** The phone's real values for [SoundStatus]. Read-only; needs no permission. */
class PhoneSoundSource(private val context: Context) : SoundStatus.Source {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)

    override val alarmVolume: Int get() = audio.getStreamVolume(AudioManager.STREAM_ALARM)
    override val alarmVolumeMax: Int get() = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    override val interruptionFilter: Int get() = notifications.currentInterruptionFilter
    override val priorityCategories: Int
        get() {
            // The policy in force (a schedule can differ from the default one), where Android reports it.
            val policy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                notifications.consolidatedNotificationPolicy
            } else {
                notifications.notificationPolicy
            }
            return policy.priorityCategories
        }
    override val ignoresBatteryOptimizations: Boolean
        get() = power.isIgnoringBatteryOptimizations(context.packageName)
}
