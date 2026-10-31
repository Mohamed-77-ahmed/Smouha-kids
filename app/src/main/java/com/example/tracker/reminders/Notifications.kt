package com.example.tracker.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.example.tracker.data.ReminderInfo
import com.example.tracker.domain.AgeGroup
import com.example.tracker.ui.AppLanguage
import com.example.tracker.ui.Fmt

object Notifications {
    /**
     * Channel settings cannot change after creation, so each sound style has its own channel.
     * Ringing: the reminder while the app plays the alarm sound itself (AlarmSoundService); the
     * channel has no sound, so there is never a double sound.
     * Alarm: the channel itself rings the default alarm sound on the alarm stream. Since batch 2 it
     * is the fallback for when the app's own sound cannot start.
     * Standard: the behavior before v2 (default notification sound), used when alarm sound is off.
     */
    const val RINGING_CHANNEL_ID = "pill_reminders_ringing"
    const val ALARM_CHANNEL_ID = "pill_reminders_alarm"
    const val STANDARD_CHANNEL_ID = "pill_reminders_standard"
    /** The single channel of v1, deleted on first start of v2. */
    const val LEGACY_CHANNEL_ID = "pill_reminders"
    const val TEST_NOTIFICATION_ID = Int.MAX_VALUE

    /** How a reminder's sound is made; decides its channel and its buttons. */
    enum class Style(val channelId: String) {
        /** The app plays the alarm sound now: silent channel, "Stop sound" button, swipe stops it. */
        RINGING(RINGING_CHANNEL_ID),
        /** The app's sound ended: same silent channel, no "Stop sound", and an update does not alert again. */
        RANG(RINGING_CHANNEL_ID),
        /** Fallback: the channel rings the alarm sound. */
        ALARM(ALARM_CHANNEL_ID),
        /** Alarm sound off: the normal notification sound. */
        STANDARD(STANDARD_CHANNEL_ID),
    }

    /**
     * What one reminder says, fixed when it fires (patient name, app language at that moment).
     * The app's sound service gets it through its start intent and shows it in any [Style].
     */
    data class Content(
        val notificationId: Int,
        /** Null for the test reminder. */
        val doseId: Long?,
        val title: String,
        val text: String,
        /** For the button labels; null = the phone's language (no choice stored yet). */
        val language: String?,
    ) {
        fun putInto(intent: Intent): Intent = intent
            .putExtra(EXTRA_ID, notificationId)
            .putExtra(EXTRA_DOSE_ID, doseId ?: NO_DOSE)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_TEXT, text)
            .putExtra(EXTRA_LANGUAGE, language)

        companion object {
            private const val EXTRA_ID = "reminder_id"
            private const val EXTRA_DOSE_ID = "reminder_dose_id"
            private const val EXTRA_TITLE = "reminder_title"
            private const val EXTRA_TEXT = "reminder_text"
            private const val EXTRA_LANGUAGE = "reminder_language"
            private const val NO_DOSE = -1L

            fun from(intent: Intent): Content? {
                if (!intent.hasExtra(EXTRA_ID)) return null
                return Content(
                    notificationId = intent.getIntExtra(EXTRA_ID, TEST_NOTIFICATION_ID),
                    doseId = intent.getLongExtra(EXTRA_DOSE_ID, NO_DOSE).takeIf { it != NO_DOSE },
                    title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                    text = intent.getStringExtra(EXTRA_TEXT).orEmpty(),
                    language = intent.getStringExtra(EXTRA_LANGUAGE),
                )
            }
        }
    }

    fun notificationId(doseId: Long): Int = (doseId and 0x7FFFFFFF).toInt()

    /** Creates the channels (or renames them after a language change) and removes the v1 one. */
    fun ensureChannel(context: Context, language: String?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(channels(context, language))
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
    }

    /** The ringing, alarm and standard channel definitions. Android keeps a channel's sound once created. */
    fun channels(context: Context, language: String?): List<NotificationChannel> {
        val res = AppLanguage.localized(context, AppLanguage.effective(language, context))
        val ringing = NotificationChannel(
            RINGING_CHANNEL_ID,
            res.getString(R.string.channel_name_ringing),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = res.getString(R.string.channel_description_ringing)
            // The app plays the sound itself; a channel sound on top would be a second sound.
            setSound(null, null)
            enableVibration(true)
            enableLights(true)
        }
        val alarm = NotificationChannel(
            ALARM_CHANNEL_ID,
            res.getString(R.string.channel_name_alarm),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = res.getString(R.string.channel_description_alarm)
            setSound(
                Settings.System.DEFAULT_ALARM_ALERT_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            enableLights(true)
        }
        val standard = NotificationChannel(
            STANDARD_CHANNEL_ID,
            res.getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = res.getString(R.string.channel_description)
            enableVibration(true)
            enableLights(true)
        }
        return listOf(ringing, alarm, standard)
    }

    /** Whether reminders can actually be shown (Android 13+ permission, the app switch, the channel). */
    fun canPost(context: Context, channelId: String? = null): Boolean {
        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permissionOk || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (channelId == null) return true
        // A channel the parent turned off in the phone settings shows nothing, so nothing may ring for it.
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(channelId)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Reminder for one dose, always naming the patient, in the app language at this moment. */
    fun content(context: Context, info: ReminderInfo, followUp: Boolean, language: String?): Content {
        val lang = AppLanguage.effective(language, context)
        val res = AppLanguage.localized(context, lang)
        val patient = Fmt.bidi(res, info.patientName)
        val medicine = Fmt.bidi(res, info.dose.medicineName)
        val title: String
        val text: String
        if (followUp) {
            title = res.getString(R.string.notif_repeat_title)
            text = res.getString(R.string.notif_repeat_body, patient, medicine)
        } else {
            title = res.getString(R.string.notif_title)
            text = if (info.ageGroup == AgeGroup.CHILD) {
                res.getString(R.string.notif_body_child, patient, medicine)
            } else {
                res.getString(R.string.notif_body, patient, medicine)
            }
        }
        return Content(notificationId(info.dose.doseId), info.dose.doseId, title, text, lang)
    }

    fun testContent(context: Context, language: String?): Content {
        val lang = AppLanguage.effective(language, context)
        val res = AppLanguage.localized(context, lang)
        return Content(
            TEST_NOTIFICATION_ID,
            doseId = null,
            title = res.getString(R.string.test_notification_title),
            text = res.getString(R.string.test_notification_text),
            language = lang,
        )
    }

    fun build(context: Context, content: Content, style: Style): Notification {
        val res = AppLanguage.localized(context, content.language)
        // A tap opens the dose (or the app for the test reminder) and silences the app's sound.
        val tap = if (content.doseId != null) {
            MainActivity.confirmIntent(context, content.doseId)
        } else {
            Intent(context, MainActivity::class.java)
        }.putExtra(MainActivity.EXTRA_SOUND_ID, content.notificationId)
        val open = PendingIntent.getActivity(
            context,
            content.notificationId,
            tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, style.channelId)
            .setSmallIcon(R.drawable.ic_pill)
            // The Koko rooster next to the text (the small icon in the status bar stays the capsule).
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_large_koko))
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // "Alarm" lets the reminder through Do Not Disturb when the phone allows alarms there.
            .setCategory(if (style == Style.STANDARD) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
        if (content.doseId != null) {
            builder.addAction(R.drawable.ic_check, res.getString(R.string.confirm_button), open)
        }
        when (style) {
            Style.RINGING -> builder
                // No PIN: silencing is harmless and follow-ups still come.
                .addAction(R.drawable.ic_volume_off, res.getString(R.string.stop_sound), AlarmSoundService.stopButtonIntent(context, content.notificationId))
                .setDeleteIntent(AlarmSoundService.dismissedIntent(context, content.notificationId))
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            Style.RANG -> builder.setOnlyAlertOnce(true)
            Style.ALARM, Style.STANDARD -> Unit
        }
        return builder.build()
    }

    fun cancel(context: Context, doseId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(doseId))
    }

    @SuppressLint("MissingPermission") // canPost() checks the permission first
    fun post(context: Context, content: Content, style: Style) {
        if (!canPost(context)) return
        NotificationManagerCompat.from(context).notify(content.notificationId, build(context, content, style))
    }
}
