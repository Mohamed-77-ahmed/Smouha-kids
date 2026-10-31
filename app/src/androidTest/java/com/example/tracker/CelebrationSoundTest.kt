package com.example.tracker

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.SaveResult
import com.example.tracker.databinding.ViewCelebrationBinding
import com.example.tracker.domain.AgeGroup
import com.example.tracker.domain.Gender
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.Slot
import com.example.tracker.domain.SoundPolicy
import com.example.tracker.reminders.AlarmScheduler
import com.example.tracker.reminders.AlarmSound
import com.example.tracker.reminders.AlarmSoundService
import com.example.tracker.reminders.Notifications
import com.example.tracker.reminders.ReminderReceiver
import com.example.tracker.ui.AboutActivity
import com.example.tracker.ui.Celebration
import com.example.tracker.ui.CelebrationSound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

/**
 * The reward's short sound (batch 6, R3) on the test emulator only, read from `dumpsys audio`:
 * "playing" means a started USAGE_GAME player of this app's uid. Never together with the app's
 * alarm sound (a started USAGE_ALARM player). How loud it is on a real phone stays UNVERIFIED.
 */
@RunWith(AndroidJUnit4::class)
class CelebrationSoundTest {

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app = ApplicationProvider.getApplicationContext<PillPalsApp>()
    private val repo get() = app.repository
    private val notifications = app.getSystemService(NotificationManager::class.java)
    private val uid = Process.myUid()
    private val slotIds = mutableListOf<Long>()
    private val doseIds = mutableListOf<Long>()
    private var minute = 0

    @get:Rule
    val english = EnglishAppLanguage()

    @Before
    fun setUp() = runBlocking {
        val now = LocalTime.now()
        assumeTrue("needs a later minute today", now.hour * 60 + now.minute < 23 * 60 + 57)
        minute = now.hour * 60 + now.minute + 2
        // No volume precondition: these tests read player states from dumpsys audio, which a player
        // reports whatever the stream volume (found muted on the test AVD once; the app never sets a volume).
        withContext(Dispatchers.IO) { app.database.clearAllTables() }
        repo.setLanguage("en")
        repo.setPin("1234")
        notifications.cancelAll()
        AlarmSound.forcedFailure = null
        shell("pm grant ${app.packageName} android.permission.POST_NOTIFICATIONS")
        Unit
    }

    @After
    fun tearDown() {
        val playing = AlarmSoundService.playingId
        if (playing != AlarmSoundService.NO_ID) AlarmSound.stop(app, SoundPolicy.StopReason.DISMISSED, playing)
        waitUntil("no sound left") { players("USAGE_ALARM").isEmpty() && players("USAGE_GAME").isEmpty() }
        slotIds.forEach { app.alarms.cancelSlot(it) }
        doseIds.forEach { app.alarms.cancelRepeat(it) }
        notifications.cancelAll()
    }

    // ------------------------------------------------------------------ helpers

    private fun shell(command: String): String {
        val pfd = instr.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
    }

    private fun <T> onMain(f: () -> T): T {
        var v: Any? = null
        instr.runOnMainSync { v = f() }
        @Suppress("UNCHECKED_CAST")
        return v as T
    }

    /** This app's players with [usage] in `dumpsys audio`: started only, or any not yet released. */
    private fun players(usage: String, startedOnly: Boolean = true, dump: String = shell("dumpsys audio")): Set<String> =
        dump.lines()
            .filter { "AudioPlaybackConfiguration" in it && "usage=$usage" in it && "u/pid:$uid/" in it }
            .filter { if (startedOnly) "state:started" in it else "state:released" !in it }
            .mapNotNull { Regex("piid:(\\d+)").find(it)?.groupValues?.get(1) }
            .toSet()

    private fun waitUntil(what: String, timeoutMillis: Long = 10_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < end) {
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    /** One stretch of time in which a player of this app played (from the player event log). */
    private data class Play(val piid: String, val usage: String, val from: Long, val to: Long)

    /** The last line of the player event log now, so later checks only read what came after. */
    private fun eventMark(): String = playerEvents(shell("dumpsys audio")).lastOrNull().orEmpty()

    private fun playerEvents(dump: String): List<String> {
        val lines = dump.lines()
        val start = lines.indexOfFirst { "playback activity as reported through PlayerBase" in it }
        if (start < 0) return emptyList()
        return lines.drop(start + 1).takeWhile { it.isBlank() || Regex("^\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d:\\d{3} ").containsMatchIn(it) }.filter { it.isNotBlank() }
    }

    private fun millisOf(line: String): Long {
        val m = Regex("^(\\d\\d)-(\\d\\d) (\\d\\d):(\\d\\d):(\\d\\d):(\\d{3})").find(line)!!.groupValues.drop(1).map { it.toLong() }
        return ((((m[0] * 31 + m[1]) * 24 + m[2]) * 60 + m[3]) * 60 + m[4]) * 1000 + m[5]
    }

    /**
     * Every time this app's players played since [mark], from the timestamped player event log of
     * `dumpsys audio` (milliseconds): it shows overlaps far too short for repeated sampling to see.
     */
    private fun playsSince(mark: String): List<Play> {
        val all = playerEvents(shell("dumpsys audio"))
        val events = all.drop(all.indexOf(mark) + 1)
        val usage = mutableMapOf<String, String>()
        val ours = mutableSetOf<String>()
        val open = mutableMapOf<String, Long>()
        val plays = mutableListOf<Play>()
        for (e in events) {
            Regex("new player piid:(\\d+) uid/pid:(\\d+)/.*usage=(\\w+)").find(e)?.let {
                if (it.groupValues[2] == uid.toString()) ours += it.groupValues[1]
                usage[it.groupValues[1]] = it.groupValues[3]
            }
            Regex("player piid:(\\d+) new AudioAttributes:.*usage=(\\w+)").find(e)?.let { usage[it.groupValues[1]] = it.groupValues[2] }
            val piid = Regex("piid:(\\d+)").find(e)?.groupValues?.get(1) ?: continue
            when {
                "event:started" in e -> open.putIfAbsent(piid, millisOf(e))
                "event:stopped" in e || "event:paused" in e || "releasing player" in e ->
                    open.remove(piid)?.let { plays += Play(piid, usage[piid].orEmpty(), it, millisOf(e)) }
            }
        }
        open.forEach { (piid, from) -> plays += Play(piid, usage[piid].orEmpty(), from, Long.MAX_VALUE) }
        return plays.filter { it.piid in ours }
    }

    /** No moment, to the millisecond, in which the app's alarm sound and the reward's sound both played. */
    private fun assertNeverBoth(plays: List<Play>) {
        val alarms = plays.filter { it.usage == "USAGE_ALARM" }
        val games = plays.filter { it.usage == "USAGE_GAME" }
        for (a in alarms) for (g in games) {
            assertFalse("both played at once: alarm ${a.from}..${a.to}, reward ${g.from}..${g.to} ($plays)", a.from < g.to && g.from < a.to)
        }
    }

    /** For [millis], no game player of the app starts. */
    private fun assertSilentFor(millis: Long, why: String) {
        val end = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < end) {
            assertTrue(why, players("USAGE_GAME").isEmpty())
            Thread.sleep(150)
        }
    }

    private fun reward(group: AgeGroup) = ConfirmOutcome.Confirmed(
        patientId = 1, patientName = "Yasmin", ageGroup = group, gender = Gender.FEMALE, medicineName = "Panadol",
        dosePoints = 10, onTimeBonus = 0, dosePaid = true, perfectDayBonus = 0, milestone = null,
        milestoneBonus = 0, liveStreak = 1, totalPoints = 10, dayComplete = false,
    )

    /** The overlay on a plain screen (About) with a real reward sound. */
    private fun withSoundOverlay(block: (Celebration, CelebrationSound, ViewCelebrationBinding) -> Unit) {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            lateinit var c: Celebration
            lateinit var s: CelebrationSound
            lateinit var b: ViewCelebrationBinding
            scenario.onActivity { a ->
                b = ViewCelebrationBinding.inflate(a.layoutInflater, a.findViewById<ViewGroup>(android.R.id.content), true)
                s = CelebrationSound(a)
                c = Celebration(b, s)
            }
            try {
                block(c, s, b)
            } finally {
                onMain { c.hide() }
            }
        }
    }

    /** A medicine with one reminder later today: (medicine id, slot id). */
    private fun addReminder(medicine: String): Pair<Long, Long> = runBlocking {
        val today = LocalDate.now().dayOfWeek.value
        val saved = repo.saveMedicine(
            MedicineInput(
                medicineId = null,
                patient = PatientChoice.New("Yasmin", "7", Gender.FEMALE),
                name = medicine,
                slots = listOf(Slot(today, minute)),
                startEpochDay = null,
                endEpochDay = null,
                reminderIntervalMinutes = 10,
            )
        ) as SaveResult.Saved
        val slotId = app.database.medicines().slotsFor(saved.medicineId).single().id
        slotIds += slotId
        saved.medicineId to slotId
    }

    /** Fires the reminder of [slotId] as its exact alarm would. */
    private fun fire(slotId: Long) {
        app.sendBroadcast(
            Intent(app, ReminderReceiver::class.java)
                .setAction(AlarmScheduler.ACTION_SLOT)
                .putExtra(AlarmScheduler.EXTRA_SLOT_ID, slotId)
                .putExtra(AlarmScheduler.EXTRA_OCCURRENCE_DAY, repo.todayEpochDay())
        )
    }

    private fun doseOf(medicineId: Long): Long {
        var dose: Long? = null
        waitUntil("the dose of the reminder") {
            dose = runBlocking { app.database.doses().day(repo.todayEpochDay()).singleOrNull { it.medicineId == medicineId }?.doseId }
            dose != null
        }
        doseIds += dose!!
        return dose!!
    }

    /** A real reminder rings with the app's alarm sound; returns its dose id. */
    private fun ringAReminder(medicine: String): Long {
        val (medicineId, slotId) = addReminder(medicine)
        fire(slotId)
        waitUntil("the app's alarm sound", 5_000) { players("USAGE_ALARM").size == 1 }
        return doseOf(medicineId)
    }

    // ------------------------------------------------------------------ tests

    @Test
    fun childRewardPlaysTheDoseSoundOnceAndReleasesIt() = withSoundOverlay { c, _, _ ->
        onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = true) }
        waitUntil("the reward's sound", 2_000) { players("USAGE_GAME").size == 1 }
        val first = players("USAGE_GAME")
        // Once: the sound (2.8 s) ends by itself and does not start again; the reward closes at 3 s.
        waitUntil("the sound ended", 4_000) { players("USAGE_GAME").isEmpty() }
        waitUntil("the reward closed", 3_000) { !onMain { c.isShowing } }
        waitUntil("the player released", 3_000) { players("USAGE_GAME", startedOnly = false).isEmpty() }
        assertSilentFor(1_500, "never played a second time")
        assertEquals(1, first.size)
    }

    @Test
    fun teenRewardPlaysItToo() = withSoundOverlay { c, _, _ ->
        onMain { c.showReward(reward(AgeGroup.TEEN), soundOn = true) }
        waitUntil("the reward's sound", 2_000) { players("USAGE_GAME").size == 1 }
    }

    @Test
    fun settingOffOrAnAdultPlaysNothing() = withSoundOverlay { c, _, _ ->
        onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = false) }
        assertSilentFor(2_500, "the setting is off")
        onMain { c.hide() }
        onMain { c.showReward(reward(AgeGroup.ADULT), soundOn = true) }
        assertSilentFor(2_500, "an adult's reward stays calm")
        // The control: the same overlay plays for a child with the setting on.
        onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = true) }
        waitUntil("the reward's sound", 2_000) { players("USAGE_GAME").size == 1 }
    }

    @Test
    fun closingTheRewardStopsAndReleasesTheSound() = withSoundOverlay { c, _, _ ->
        onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = true) }
        waitUntil("the reward's sound", 2_000) { players("USAGE_GAME").size == 1 }
        // A tap closes it early.
        onMain { c.hide() }
        waitUntil("stopped right away", 1_000) { players("USAGE_GAME").isEmpty() }
        waitUntil("released", 3_000) { players("USAGE_GAME", startedOnly = false).isEmpty() }
    }

    @Test
    fun aRotationShowsTheRewardAgainWithoutTheSound() = withSoundOverlay { c, s, b ->
        onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = true) }
        waitUntil("the reward's sound", 2_000) { players("USAGE_GAME").size == 1 }
        val saved = Bundle()
        onMain { c.save(saved) }
        // What a rotation does: the screen stops (the sound with it), a new overlay restores the state.
        onMain { s.stop() }
        waitUntil("stopped with the old screen", 1_000) { players("USAGE_GAME").isEmpty() }
        val again = onMain { Celebration(b, CelebrationSound(app)).also { it.restore(saved) } }
        assertTrue(onMain { again.isShowing })
        assertSilentFor(2_000, "restored without playing again")
        onMain { again.hide() }
    }

    @Test
    fun theConfirmedDosesAlarmStopsFirstThenTheRewardPlaysNeverBoth() {
        val mark = eventMark()
        val dose = ringAReminder("Panadol")
        withSoundOverlay { c, _, _ ->
            // What confirming does: stop that dose's alarm, then show the reward.
            onMain {
                app.alarms.onDoseTaken(dose)
                c.showReward(reward(AgeGroup.CHILD), soundOn = true, reminderId = Notifications.notificationId(dose))
            }
            var sawReward = false
            val end = System.currentTimeMillis() + 3_500
            while (System.currentTimeMillis() < end) {
                val dump = shell("dumpsys audio")
                val alarm = players("USAGE_ALARM", dump = dump)
                val game = players("USAGE_GAME", dump = dump)
                assertFalse("never both: alarm $alarm, reward $game", alarm.isNotEmpty() && game.isNotEmpty())
                if (game.isNotEmpty()) sawReward = true
                Thread.sleep(80)
            }
            assertTrue("the reward's sound played once the alarm had stopped", sawReward)
            assertEquals(AlarmSoundService.NO_ID, AlarmSoundService.playingId)
            // To the millisecond: the alarm's player stopped before the reward's player started.
            val plays = playsSince(mark)
            assertEquals("one alarm, one reward: $plays", listOf("USAGE_ALARM", "USAGE_GAME"), plays.sortedBy { it.from }.map { it.usage })
            assertNeverBoth(plays)
        }
    }

    @Test
    fun whileAnotherReminderRingsTheRewardStaysSilent() {
        ringAReminder("Augmentin 1g")
        withSoundOverlay { c, _, _ ->
            // A reward for another dose (its reminder does not ring).
            onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = true, reminderId = 999_999) }
            assertSilentFor(2_500, "another reminder rings")
            assertEquals("that reminder keeps ringing", 1, players("USAGE_ALARM").size)
        }
    }

    @Test
    fun anAlarmDuringTheRewardStopsTheRewardsSound() {
        val (medicineId, slotId) = addReminder("Vitamin D")
        val mark = eventMark()
        withSoundOverlay { c, _, _ ->
            onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = true) }
            waitUntil("the reward's sound", 2_000) { players("USAGE_GAME").size == 1 }
            val rewardStarted = System.currentTimeMillis()
            // A reminder fires now: its alarm wins, and the two are never heard together.
            fire(slotId)
            var alarmStarted = 0L
            val end = System.currentTimeMillis() + 4_000
            while (System.currentTimeMillis() < end) {
                val dump = shell("dumpsys audio")
                val alarm = players("USAGE_ALARM", dump = dump)
                val game = players("USAGE_GAME", dump = dump)
                assertFalse("never both: alarm $alarm, reward $game", alarm.isNotEmpty() && game.isNotEmpty())
                if (alarm.isNotEmpty() && alarmStarted == 0L) alarmStarted = System.currentTimeMillis()
                Thread.sleep(80)
            }
            assertTrue("the reminder rang", alarmStarted > 0)
            // Well before the 2.8 s sound would have ended by itself: the alarm cut it short.
            assertTrue("the alarm started ${alarmStarted - rewardStarted} ms after the reward's sound", alarmStarted - rewardStarted < 2_500)
            assertTrue("the reward's sound was stopped for it", players("USAGE_GAME").isEmpty())
            // To the millisecond: the reward's player stopped before the alarm's player started.
            assertNeverBoth(playsSince(mark))
            doseOf(medicineId)
        }
    }

    @Test
    fun withAnimationsOffTheSoundStillPlaysOnceAndTheRewardCloses() {
        val before = shell("settings get global animator_duration_scale").trim()
        try {
            shell("settings put global animator_duration_scale 0")
            waitUntil("animations off in the app") { !android.animation.ValueAnimator.areAnimatorsEnabled() }
            withSoundOverlay { c, _, b ->
                onMain { c.showReward(reward(AgeGroup.CHILD), soundOn = true) }
                waitUntil("the reward's sound", 2_000) { players("USAGE_GAME").size == 1 }
                assertEquals("the avatar at full size, no spring", 1f, onMain { b.avatarArt.scaleX }, 0f)
                waitUntil("the reward closed by itself", 5_000) { !onMain { c.isShowing } }
                waitUntil("released", 3_000) { players("USAGE_GAME", startedOnly = false).isEmpty() }
            }
        } finally {
            if (before == "null" || before.isEmpty()) shell("settings delete global animator_duration_scale")
            else shell("settings put global animator_duration_scale $before")
            if (before != "0" && before != "0.0") waitUntil("animations back on") { android.animation.ValueAnimator.areAnimatorsEnabled() }
        }
    }
}
