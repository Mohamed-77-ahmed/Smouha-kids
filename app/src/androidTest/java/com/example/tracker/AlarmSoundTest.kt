package com.example.tracker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.Intent
import android.media.AudioManager
import android.os.ParcelFileDescriptor
import android.os.Process
import android.service.notification.StatusBarNotification
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.SaveResult
import com.example.tracker.domain.Gender
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.Slot
import com.example.tracker.reminders.AlarmScheduler
import com.example.tracker.reminders.AlarmSound
import com.example.tracker.reminders.AlarmSoundService
import com.example.tracker.reminders.Notifications
import com.example.tracker.reminders.ReminderReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/**
 * The app's own alarm sound (CLAUDE.md 7), on the test emulator only, with the in-memory DB of
 * [TestApp]. "Playing" means `dumpsys audio` lists a started USAGE_ALARM player of this app's uid.
 *
 * Reminders are fired by sending the alarm's broadcast to [ReminderReceiver] directly. Note: on
 * this Android image, ringer SILENT set through AudioManager also turns on Do Not Disturb
 * (priority, alarms allowed); the test asserts the ringer mode it runs with.
 */
@RunWith(AndroidJUnit4::class)
class AlarmSoundTest {

    private val app = ApplicationProvider.getApplicationContext<PillPalsApp>()
    private val repo get() = app.repository
    private val audio = app.getSystemService(AudioManager::class.java)
    private val notifications = app.getSystemService(NotificationManager::class.java)
    private val uid = Process.myUid()
    private val slotIds = mutableListOf<Long>()
    private val doseIds = mutableListOf<Long>()
    private var minute = 0

    @get:Rule
    val english = EnglishAppLanguage()

    @Before
    fun setUp() = runBlocking {
        // A reminder minute later today (doses for an earlier minute start tomorrow, D11).
        val now = LocalTime.now()
        assumeTrue("needs a later minute today", now.hour * 60 + now.minute < 23 * 60 + 57)
        minute = now.hour * 60 + now.minute + 2
        assertTrue("alarm volume must not be 0 on the test emulator", audio.getStreamVolume(AudioManager.STREAM_ALARM) > 0)
        withContext(Dispatchers.IO) { app.database.clearAllTables() }
        repo.setLanguage("en")
        repo.setPin("1234")
        notifications.cancelAll()
        AlarmSound.forcedFailure = null
        // A parent who allowed notifications (a fresh test install has not). Never revoked here:
        // revoking a runtime permission kills the app process, and with it this test.
        shell("pm grant ${app.packageName} android.permission.POST_NOTIFICATIONS")
        shell("cmd notification allow_dnd ${app.packageName}")
        Unit
    }

    @After
    fun tearDown() {
        AlarmSound.forcedFailure = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach { it.finish() }
        }
        // A home screen opened by the tap test re-arms alarms in the background; let that finish
        // before the alarms are cancelled below, so none of them fires later on this emulator.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1_500)
        // Silence anything still playing, then put the emulator back: ringer normal, Do Not Disturb off.
        val playing = AlarmSoundService.playingId
        if (playing != AlarmSoundService.NO_ID) AlarmSound.stop(app, com.example.tracker.domain.SoundPolicy.StopReason.DISMISSED, playing)
        waitUntil("no sound") { ourAlarmPlayers().isEmpty() }
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        shell("cmd notification disallow_dnd ${app.packageName}")
        // No test alarm may fire later on this emulator.
        slotIds.forEach { app.alarms.cancelSlot(it) }
        doseIds.forEach { app.alarms.cancelRepeat(it) }
        notifications.cancelAll()
    }

    // ------------------------------------------------------------------ helpers

    private fun shell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
    }

    /** piids of started players on the alarm stream (`dumpsys audio`), of this app or of any uid. */
    private fun alarmPlayers(onlyOurs: Boolean): Set<String> =
        shell("dumpsys audio").lines()
            .filter { "AudioPlaybackConfiguration" in it && "state:started" in it && "usage=USAGE_ALARM" in it }
            .filter { !onlyOurs || "u/pid:$uid/" in it }
            .mapNotNull { Regex("piid:(\\d+)").find(it)?.groupValues?.get(1) }
            .toSet()

    private fun ourAlarmPlayers() = alarmPlayers(onlyOurs = true)

    private fun waitUntil(what: String, timeoutMillis: Long = 10_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < end) {
            if (check()) return
            Thread.sleep(200)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    /** Milliseconds until [check] holds, failing after [limitMillis]. */
    private fun millisUntil(what: String, limitMillis: Long, check: () -> Boolean): Long {
        val start = System.currentTimeMillis()
        waitUntil(what, limitMillis, check)
        return System.currentTimeMillis() - start
    }

    private fun addMedicine(patient: String, medicine: String): Long = runBlocking {
        val today = LocalDate.now().dayOfWeek.value
        val saved = repo.saveMedicine(
            MedicineInput(
                medicineId = null,
                patient = PatientChoice.New(patient, "7", Gender.FEMALE),
                name = medicine,
                slots = listOf(Slot(today, minute)),
                startEpochDay = null,
                endEpochDay = null,
                reminderIntervalMinutes = 10,
            )
        ) as SaveResult.Saved
        val slotId = app.database.medicines().slotsFor(saved.medicineId).single().id
        slotIds += slotId
        slotId
    }

    private fun fireSlot(slotId: Long) {
        app.sendBroadcast(
            Intent(app, ReminderReceiver::class.java)
                .setAction(AlarmScheduler.ACTION_SLOT)
                .putExtra(AlarmScheduler.EXTRA_SLOT_ID, slotId)
                .putExtra(AlarmScheduler.EXTRA_OCCURRENCE_DAY, repo.todayEpochDay())
        )
    }

    private fun fireRepeat(doseId: Long) {
        app.sendBroadcast(
            Intent(app, ReminderReceiver::class.java)
                .setAction(AlarmScheduler.ACTION_REPEAT)
                .putExtra(AlarmScheduler.EXTRA_DOSE_ID, doseId)
        )
    }

    /** Today's dose of a slot (created when the medicine is saved or when its alarm fires). */
    private fun doseOf(slotId: Long): Long {
        var id: Long? = null
        waitUntil("dose of slot $slotId") {
            id = runBlocking {
                val slot = checkNotNull(repo.slotSchedule(slotId))
                app.database.doses().day(repo.todayEpochDay()).singleOrNull { it.medicineId == slot.medicineId }?.doseId
            }
            id != null
        }
        doseIds += id!!
        return id!!
    }

    private fun posted(doseId: Long): StatusBarNotification? =
        notifications.activeNotifications.firstOrNull { it.id == Notifications.notificationId(doseId) }

    private fun actionsOf(doseId: Long): List<String> =
        posted(doseId)?.notification?.actions.orEmpty().map { it.title.toString() }

    /** Fires one reminder and waits until the app plays it: returns its dose id. */
    private fun ringOne(ringer: Int = AudioManager.RINGER_MODE_NORMAL): Long {
        audio.ringerMode = ringer
        assertEquals(ringer, audio.ringerMode)
        val slot = addMedicine("Yasmin", "Panadol")
        fireSlot(slot)
        val took = millisUntil("alarm sound of the app", 3_000) { ourAlarmPlayers().size == 1 }
        val dose = doseOf(slot)
        waitUntil("ringing reminder") { actionsOf(dose) == listOf("I took it", "Stop sound") }
        assertEquals(Notifications.RINGING_CHANNEL_ID, posted(dose)!!.notification.channelId)
        assertTrue("started after $took ms", took <= 3_000)
        return dose
    }

    // ------------------------------------------------------------------ plays on silent and vibrate

    @Test
    fun silentRingerStillPlaysOnTheAlarmStreamAndStopButtonSilences() {
        val dose = ringOne(AudioManager.RINGER_MODE_SILENT)
        // One sound only: the reminder's channel is silent, so no other alarm-stream player exists.
        assertEquals(ourAlarmPlayers(), alarmPlayers(onlyOurs = false))

        posted(dose)!!.notification.actions.single { it.title.toString() == "Stop sound" }.actionIntent.send()
        waitUntil("silence after Stop sound", 3_000) { ourAlarmPlayers().isEmpty() }
        // The reminder stays (follow-ups still come), without the button.
        waitUntil("quiet reminder") { actionsOf(dose) == listOf("I took it") }
        assertEquals(AlarmSoundService.NO_ID, AlarmSoundService.playingId)
    }

    @Test
    fun vibrateRingerStillPlaysAndATapSilences() {
        val dose = ringOne(AudioManager.RINGER_MODE_VIBRATE)
        tapInShade("Panadol")
        waitUntil("silence after a tap", 3_000) { ourAlarmPlayers().isEmpty() }
        waitUntil("tapped reminder gone") { posted(dose) == null }
        waitUntil("the dose opens") { resumedActivity() is MainActivity }
    }

    /**
     * Taps the reminder in the notification shade, as a parent would. (Sending its PendingIntent
     * from the test is not the same: Android blocks that activity start from the background.)
     */
    private fun tapInShade(text: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // The shade is a System UI window: let the test see every window, not only the app's.
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        // Walks the tree itself: the platform's text search skips these notification views.
        fun find(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.text?.toString()?.contains(text) == true) return n
            for (i in 0 until n.childCount) find(n.getChild(i))?.let { return it }
            return null
        }
        var label: AccessibilityNodeInfo? = null
        waitUntil("reminder in the shade") {
            label = automation.windows.firstNotNullOfOrNull { find(it.root) }
            label != null
        }
        // The text itself is not clickable; the notification row around it is.
        var node = label
        while (node != null && !node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) node = node.parent
        assertNotNull("a clickable reminder row", node)
    }

    private fun resumedActivity(): android.app.Activity? {
        var top: android.app.Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            top = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return top
    }

    @Test
    fun confirmingTheDoseSilences() {
        val dose = ringOne()
        runBlocking { repo.confirmDose(dose) }
        app.alarms.onDoseTaken(dose)
        waitUntil("silence after confirm", 3_000) { ourAlarmPlayers().isEmpty() }
        waitUntil("confirmed reminder gone") { posted(dose) == null }
    }

    @Test
    fun swipingTheReminderAwaySilences() {
        val dose = ringOne()
        // What Android sends when the reminder is swiped away.
        posted(dose)!!.notification.deleteIntent.send()
        waitUntil("silence after dismiss", 3_000) { ourAlarmPlayers().isEmpty() }
        waitUntil("dismissed reminder gone") { posted(dose) == null }
    }

    @Test
    fun soundStopsByItselfAfter30Seconds() {
        val dose = ringOne()
        Thread.sleep(25_000)
        assertEquals("still playing at 25 s", 1, ourAlarmPlayers().size)
        waitUntil("silence after 30 s", 10_000) { ourAlarmPlayers().isEmpty() }
        waitUntil("quiet reminder") { actionsOf(dose) == listOf("I took it") }
    }

    // ------------------------------------------------------------------ no sound, one sound

    @Test
    fun alarmSoundOffPlaysNothingAndUsesTheStandardChannel() {
        runBlocking { repo.setAlarmSound(false) }
        val slot = addMedicine("Yasmin", "Panadol")
        fireSlot(slot)
        val dose = doseOf(slot)
        waitUntil("reminder shown") { posted(dose) != null }
        Thread.sleep(3_000)
        assertEquals(emptySet<String>(), ourAlarmPlayers())
        assertEquals(Notifications.STANDARD_CHANNEL_ID, posted(dose)!!.notification.channelId)
        assertEquals(AlarmSoundService.NO_ID, AlarmSoundService.playingId)
    }

    @Test
    fun twoDosesTogetherGiveOneSoundAndTwoReminders() {
        val first = addMedicine("Yasmin", "Panadol")
        val second = addMedicine("Omar", "Augmentin 1g")
        fireSlot(first)
        fireSlot(second)
        val a = doseOf(first)
        val b = doseOf(second)
        waitUntil("both reminders shown") { posted(a) != null && posted(b) != null && "Stop sound" in actionsOf(a) + actionsOf(b) }
        Thread.sleep(2_000)
        assertEquals(1, ourAlarmPlayers().size)
        // The newer reminder has the sound; the other one stays, quiet.
        val ringing = listOf(a, b).filter { "Stop sound" in actionsOf(it) }
        assertEquals(1, ringing.size)
        assertEquals(listOf("I took it"), actionsOf(listOf(a, b).single { it != ringing.single() }))
    }

    @Test
    fun followUpOfATakenDosePlaysNothing() {
        val dose = ringOne()
        runBlocking { repo.confirmDose(dose) }
        app.alarms.onDoseTaken(dose)
        waitUntil("silence after confirm", 3_000) { ourAlarmPlayers().isEmpty() }

        fireRepeat(dose)
        Thread.sleep(3_000)
        assertEquals(emptySet<String>(), ourAlarmPlayers())
        assertNull(posted(dose))
    }

    // ------------------------------------------------------------------ fallback

    @Test
    fun serviceThatCannotStartFallsBackToTheAlarmChannel() {
        fallsBackToTheAlarmChannel(AlarmSound.Failure.SERVICE_START)
    }

    @Test
    fun startForegroundThatFailsFallsBackToTheAlarmChannel() {
        fallsBackToTheAlarmChannel(AlarmSound.Failure.START_FOREGROUND)
    }

    private fun fallsBackToTheAlarmChannel(failure: AlarmSound.Failure) {
        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        AlarmSound.forcedFailure = failure
        val slot = addMedicine("Yasmin", "Panadol")
        fireSlot(slot)
        val dose = doseOf(slot)
        waitUntil("reminder on the alarm channel") { posted(dose)?.notification?.channelId == Notifications.ALARM_CHANNEL_ID }
        // Never silence by accident: Android rings the alarm channel (a player that is not the app's) ...
        waitUntil("the alarm channel rings", 3_000) { alarmPlayers(onlyOurs = false).isNotEmpty() }
        // ... and never both: the app plays nothing.
        assertEquals(emptySet<String>(), ourAlarmPlayers())
        assertEquals(AlarmSoundService.NO_ID, AlarmSoundService.playingId)
        assertNotNull(posted(dose))
    }
}
