package com.example.tracker.reminders

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.tracker.domain.SoundPolicy

/**
 * Announces a reminder with the right sound (CLAUDE.md 7) and silences the app's alarm sound.
 * [SoundPolicy] decides; this object only carries the decision out.
 */
object AlarmSound {
    const val TAG = "SmouhaKidsSound"

    /** Failures a test can force, to check the fallback (never silence by accident, never both). */
    enum class Failure { SERVICE_START, START_FOREGROUND }

    /** Test hook only (AlarmSoundTest); always null in the app. Read by [AlarmSoundService] too. */
    @Volatile
    var forcedFailure: Failure? = null

    fun announce(context: Context, content: Notifications.Content, dosePending: Boolean, alarmSoundOn: Boolean) {
        val style = if (alarmSoundOn) Notifications.Style.RINGING else Notifications.Style.STANDARD
        val canPost = Notifications.canPost(context, style.channelId)
        when (SoundPolicy.plan(canPost, dosePending, alarmSoundOn)) {
            SoundPolicy.Plan.NOTHING -> Unit
            SoundPolicy.Plan.STANDARD_CHANNEL -> Notifications.post(context, content, Notifications.Style.STANDARD)
            SoundPolicy.Plan.APP_PLAYS -> if (!startService(context, content)) {
                Notifications.post(context, content, Notifications.Style.ALARM)
            }
        }
    }

    /** True when the service was asked to play; false (with the reason in the log) when Android refused. */
    private fun startService(context: Context, content: Notifications.Content): Boolean = try {
        if (forcedFailure == Failure.SERVICE_START) throw IllegalStateException("start made to fail by a test")
        ContextCompat.startForegroundService(context, AlarmSoundService.playIntent(context, content))
        true
    } catch (e: RuntimeException) {
        // ForegroundServiceStartNotAllowedException (Android 12+), SecurityException, IllegalStateException.
        Log.w(TAG, "Alarm sound service did not start, the alarm channel rings instead: $e")
        false
    }

    /** Silences the app's sound if it plays for reminder [notificationId]. Nothing happens otherwise. */
    fun stop(context: Context, reason: SoundPolicy.StopReason, notificationId: Int) {
        if (AlarmSoundService.playingId != notificationId) return
        try {
            context.startService(AlarmSoundService.stopIntent(context, reason, notificationId))
        } catch (e: IllegalStateException) {
            // Android refuses only when the app is fully in the background, and then no sound plays.
            Log.w(TAG, "Could not reach the alarm sound service: $e")
        }
    }
}
