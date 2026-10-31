package com.example.tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.reminders.Notifications
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The channels Android stores and the reminder each sound style builds: the ringing channel is
 * silent (the app plays the sound), the alarm channel is the batch 1 fallback, the off option keeps
 * the old behavior, v1's channel is gone.
 */
@RunWith(AndroidJUnit4::class)
class NotificationChannelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @get:Rule
    val english = EnglishAppLanguage()

    @Test
    fun storedChannelsAfterAnUpdateFromV1() {
        // Simulates an update from v1: the old channel exists first.
        manager.createNotificationChannel(
            NotificationChannel(Notifications.LEGACY_CHANNEL_ID, "old", NotificationManager.IMPORTANCE_HIGH)
        )
        Notifications.ensureChannel(context, "en")

        val ringing = manager.getNotificationChannel(Notifications.RINGING_CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, ringing.importance)
        assertNull(ringing.sound)

        val alarm = manager.getNotificationChannel(Notifications.ALARM_CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alarm.importance)
        assertEquals(Settings.System.DEFAULT_ALARM_ALERT_URI, alarm.sound)
        assertEquals(AudioAttributes.USAGE_ALARM, alarm.audioAttributes.usage)

        val standard = manager.getNotificationChannel(Notifications.STANDARD_CHANNEL_ID)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, standard.importance)
        assertNotEquals(AudioAttributes.USAGE_ALARM, standard.audioAttributes.usage)
        assertEquals(Settings.System.DEFAULT_NOTIFICATION_URI, standard.sound)

        assertNull(manager.getNotificationChannel(Notifications.LEGACY_CHANNEL_ID))
    }

    @Test
    fun channelDefinitionsAsTheCodeBuildsThem() {
        // Android keeps the sound of a channel that already exists, so check what the code defines too.
        val byId = Notifications.channels(context, "en").associateBy { it.id }
        assertEquals(
            setOf(Notifications.RINGING_CHANNEL_ID, Notifications.ALARM_CHANNEL_ID, Notifications.STANDARD_CHANNEL_ID),
            byId.keys,
        )
        val ringing = byId.getValue(Notifications.RINGING_CHANNEL_ID)
        assertNull(ringing.sound)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, ringing.importance)
        val alarm = byId.getValue(Notifications.ALARM_CHANNEL_ID)
        assertEquals(AudioAttributes.USAGE_ALARM, alarm.audioAttributes.usage)
        assertEquals(Settings.System.DEFAULT_ALARM_ALERT_URI, alarm.sound)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alarm.importance)
        val standard = byId.getValue(Notifications.STANDARD_CHANNEL_ID)
        assertEquals(AudioAttributes.USAGE_NOTIFICATION, standard.audioAttributes.usage)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, standard.importance)
    }

    @Test
    fun eachStyleBuildsItsChannelAndButtons() {
        val content = Notifications.Content(42, doseId = 42L, title = "Medicine time", text = "Yasmin: time for Panadol", language = "en")
        val built = Notifications.Style.entries.associateWith { Notifications.build(context, content, it) }

        assertEquals(Notifications.RINGING_CHANNEL_ID, built.getValue(Notifications.Style.RINGING).channelId)
        assertEquals(Notifications.RINGING_CHANNEL_ID, built.getValue(Notifications.Style.RANG).channelId)
        assertEquals(Notifications.ALARM_CHANNEL_ID, built.getValue(Notifications.Style.ALARM).channelId)
        assertEquals(Notifications.STANDARD_CHANNEL_ID, built.getValue(Notifications.Style.STANDARD).channelId)

        // Only the ringing reminder has "Stop sound" and stops the sound when swiped away.
        val labels = built.mapValues { (_, n) -> n.actions.orEmpty().map { it.title.toString() } }
        assertEquals(listOf("I took it", "Stop sound"), labels.getValue(Notifications.Style.RINGING))
        assertEquals(listOf("I took it"), labels.getValue(Notifications.Style.RANG))
        assertEquals(listOf("I took it"), labels.getValue(Notifications.Style.ALARM))
        assertEquals(listOf("I took it"), labels.getValue(Notifications.Style.STANDARD))
        assertNotNull(built.getValue(Notifications.Style.RINGING).deleteIntent)
        assertNull(built.getValue(Notifications.Style.RANG).deleteIntent)

        // After the sound ended an update must not alert again.
        val rang = built.getValue(Notifications.Style.RANG)
        assertEquals(Notification.FLAG_ONLY_ALERT_ONCE, rang.flags and Notification.FLAG_ONLY_ALERT_ONCE)
        assertEquals(0, built.getValue(Notifications.Style.RINGING).flags and Notification.FLAG_ONLY_ALERT_ONCE)

        assertEquals(Notification.CATEGORY_ALARM, built.getValue(Notifications.Style.RINGING).category)
        assertEquals(Notification.CATEGORY_ALARM, built.getValue(Notifications.Style.ALARM).category)
        assertEquals(Notification.CATEGORY_REMINDER, built.getValue(Notifications.Style.STANDARD).category)
    }

    @Test
    fun theTestReminderHasNoConfirmButton() {
        val test = Notifications.testContent(context, "en")
        val ringing = Notifications.build(context, test, Notifications.Style.RINGING)
        assertEquals(listOf("Stop sound"), ringing.actions.orEmpty().map { it.title.toString() })
    }
}
