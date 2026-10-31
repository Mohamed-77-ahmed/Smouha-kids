package com.example.tracker

import android.app.Activity
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import android.os.Process
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.SaveResult
import com.example.tracker.domain.Gender
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.Slot
import com.example.tracker.ui.CourseEndLayout
import com.example.tracker.ui.CourseEndScreen
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
import java.time.LocalTime
import kotlin.math.abs

/**
 * The course-complete screen (batch 6, R6) on the real home screen, with the in-memory DB of the test
 * app: it follows the reward of a dated course's final dose, once. While the video plays the main
 * thread is rarely idle, so views are read with runOnMainSync instead of Espresso.
 */
@RunWith(AndroidJUnit4::class)
class CourseEndUiTest {

    @get:Rule
    val english = EnglishAppLanguage()

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app = ApplicationProvider.getApplicationContext<PillPalsApp>()
    private val repo get() = app.repository
    private val uid = Process.myUid()
    private var minute = 0

    @Before
    fun setUp() = runBlocking {
        val now = LocalTime.now()
        assumeTrue("needs later minutes today", now.hour * 60 + now.minute < 23 * 60 + 55)
        minute = now.hour * 60 + now.minute + 2
        withContext(Dispatchers.IO) { app.database.clearAllTables() }
        repo.setLanguage("en")
        repo.setPin("1234")
        // Confirming asks a plain yes/no instead of the PIN (the PIN gate has its own tests).
        repo.setRequirePinForDose(false)
        CourseEndScreen.forcedFailure = false
        Unit
    }

    @After
    fun tearDown() {
        CourseEndScreen.forcedFailure = false
        instr.runOnMainSync { (resumed() as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    // ------------------------------------------------------------------ helpers

    private fun <T> onMain(f: () -> T): T {
        var v: Any? = null
        instr.runOnMainSync { v = f() }
        @Suppress("UNCHECKED_CAST")
        return v as T
    }

    private fun resumed(): Activity? = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()

    private fun home(): MainActivity? = onMain { resumed() as? MainActivity }

    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instr.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }

    /** This app's started players with [usage] in `dumpsys audio`. */
    private fun players(usage: String): Int =
        shell("dumpsys audio").lines().count { "AudioPlaybackConfiguration" in it && "state:started" in it && "usage=$usage" in it && "u/pid:$uid/" in it }

    private fun waitUntil(what: String, timeoutMillis: Long = 10_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < end) {
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    /** The overlay [id] on the MainActivity on screen now (it may have been recreated meanwhile). */
    private fun overlayShowing(id: Int): Boolean = onMain { (resumed() as? MainActivity)?.findViewById<View>(id)?.isVisible == true }

    private fun <T> onHome(f: (MainActivity) -> T): T = onMain { f(resumed() as MainActivity) }

    /** Yasmin's medicine, daily at [minutes] today, running from 3 days ago to [endInDays] days from today. */
    private fun course(name: String, endInDays: Long?, vararg minutes: Int): Long = runBlocking {
        val today = repo.todayEpochDay()
        val saved = repo.saveMedicine(
            MedicineInput(
                medicineId = null,
                patient = PatientChoice.New("Yasmin", "7", Gender.FEMALE),
                name = name,
                slots = (1..7).flatMap { d -> minutes.map { Slot(d, it) } },
                startEpochDay = today - 3,
                endEpochDay = endInDays?.let { today + it },
                reminderIntervalMinutes = 10,
            )
        ) as SaveResult.Saved
        repo.refreshToday()
        saved.medicineId
    }

    private fun todaysDoses(medicineId: Long) = runBlocking {
        app.database.doses().day(repo.todayEpochDay()).filter { it.medicineId == medicineId }.sortedBy { it.scheduledMinuteOfDay }
    }

    /** The first "I took it" button on screen (a taken dose keeps its card, without the button). */
    private fun takeButton(): View? = onMain {
        val a = resumed() as? MainActivity ?: return@onMain null
        val found = ArrayList<View>()
        fun walk(v: View) {
            if (v.id == R.id.takeButton && v.isShown) found += v
            if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(a.findViewById(R.id.list))
        found.minByOrNull { IntArray(2).also { xy -> it.getLocationOnScreen(xy) }[1] }
    }

    /** Confirms the next dose on screen as a parent would (yes/no, no PIN here), then waits for the reward. */
    private fun confirmOnScreen() {
        waitUntil("a dose to confirm on home") { takeButton() != null }
        val button = takeButton()!!
        onMain { button.performClick() }
        // "Yes" clicked directly: Espresso would wait for an idle screen, which only comes after the
        // reward's 3 seconds of animation, and the reward would be gone by then.
        fun question(): AlertDialog? = (resumed() as? MainActivity)?.supportFragmentManager?.fragments
            ?.filterIsInstance<DialogFragment>()?.firstNotNullOfOrNull { (it.dialog as? AlertDialog)?.takeIf { d -> d.isShowing } }
        waitUntil("the yes/no question") { onMain { question() != null } }
        onMain { question()!!.getButton(DialogInterface.BUTTON_POSITIVE).performClick() }
        try {
            waitUntil("the reward") { overlayShowing(R.id.celebration) }
        } catch (e: AssertionError) {
            val taken = runBlocking { app.database.doses().day(repo.todayEpochDay()).map { "${it.medicineName} ${it.scheduledMinuteOfDay} ${it.status}" } }
            throw AssertionError("${e.message}; doses today: $taken; on screen: ${onMain { resumed()?.javaClass?.simpleName }}")
        }
    }

    private fun message(): String = onHome { it.findViewById<TextView>(R.id.courseMessage).text.toString() }

    // ------------------------------------------------------------------ tests

    @Test
    fun theFinalDoseShowsTheCourseEndOnceAfterTheReward() {
        val medicine = course("Panadol", endInDays = 0, minute)
        val dose = todaysDoses(medicine).single()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil("home") { home() != null }
            confirmOnScreen()
            // Not on top of the reward: only after it closed.
            assertFalse(overlayShowing(R.id.courseEnd))
            waitUntil("the course-complete screen after the reward", 8_000) { overlayShowing(R.id.courseEnd) }
            assertFalse("the reward has closed", overlayShowing(R.id.celebration))
            assertEquals("The recorded schedule for Panadol is complete. Bravo!", message())
            assertTrue("the close button from the first second", onHome { it.findViewById<View>(R.id.courseClose).isShown })
            assertFalse(onHome { it.findViewById<View>(R.id.fallback).isVisible })
            waitUntil("the video plays", 5_000) { players("USAGE_MEDIA") == 1 }
            assertFalse("its own sound with the setting on", CourseEndScreen.showing!!.isMuted)

            onHome { it.findViewById<View>(R.id.courseClose).performClick() }
            assertFalse(overlayShowing(R.id.courseEnd))
            waitUntil("the player released", 3_000) { players("USAGE_MEDIA") == 0 }

            // Confirming again changes nothing and shows nothing.
            assertTrue(runBlocking { repo.confirmDose(dose.doseId) } is ConfirmOutcome.Rejected)
            Thread.sleep(4_000)
            assertFalse(overlayShowing(R.id.courseEnd))
        }
    }

    @Test
    fun withTwoDosesOnTheLastDayOnlyTheSecondEndsTheCourse() {
        val medicine = course("Augmentin 1g", endInDays = 0, minute, minute + 1)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil("home") { home() != null }
            // The first of the two: a reward, no course-complete screen.
            confirmOnScreen()
            waitUntil("the reward closed", 6_000) { !overlayShowing(R.id.celebration) }
            Thread.sleep(2_500)
            assertFalse("not after the first dose of the last day", overlayShowing(R.id.courseEnd))
            assertEquals(1, todaysDoses(medicine).count { it.status == "TAKEN" })
            // The second one ends the course.
            confirmOnScreen()
            waitUntil("the course-complete screen", 8_000) { overlayShowing(R.id.courseEnd) }
            onHome { it.findViewById<View>(R.id.courseClose).performClick() }
        }
    }

    @Test
    fun aCourseThatGoesOnShowsNothing() {
        course("Vitamin D", endInDays = 1, minute)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil("home") { home() != null }
            confirmOnScreen()
            waitUntil("the reward closed", 6_000) { !overlayShowing(R.id.celebration) }
            Thread.sleep(3_000)
            assertFalse(overlayShowing(R.id.courseEnd))
        }
    }

    @Test
    fun withTheSettingOffTheVideoPlaysWithoutSound() {
        runBlocking { repo.setCelebrationSound(false) }
        course("Panadol", endInDays = 0, minute)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil("home") { home() != null }
            confirmOnScreen()
            waitUntil("the course-complete screen", 8_000) { overlayShowing(R.id.courseEnd) }
            waitUntil("the video plays", 5_000) { players("USAGE_MEDIA") == 1 }
            assertTrue("muted with the setting off", CourseEndScreen.showing!!.isMuted)
            onHome { it.findViewById<View>(R.id.courseClose).performClick() }
        }
    }

    @Test
    fun whenTheVideoCannotPlayTheAvatarShowsWithTheSameText() {
        CourseEndScreen.forcedFailure = true
        course("Panadol", endInDays = 0, minute)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil("home") { home() != null }
            confirmOnScreen()
            waitUntil("the course-complete screen", 8_000) { overlayShowing(R.id.courseEnd) }
            assertTrue(onHome { it.findViewById<View>(R.id.fallback).isShown })
            assertFalse(onHome { it.findViewById<View>(R.id.video).isVisible })
            assertEquals("The recorded schedule for Panadol is complete. Bravo!", message())
            assertEquals(0, players("USAGE_MEDIA"))
            onHome { it.findViewById<View>(R.id.courseClose).performClick() }
            assertFalse(overlayShowing(R.id.courseEnd))
        }
    }

    /** The card fits 80 percent of the screen and the video keeps its aspect ratio. */
    private fun assertSized() {
        val (screen, card, box) = onHome { a ->
            Triple(
                a.findViewById<View>(R.id.courseEnd).let { it.width to it.height },
                a.findViewById<View>(R.id.courseCard).let { it.width to it.height },
                a.findViewById<View>(R.id.videoBox).let { it.width to it.height },
            )
        }
        val slack = (4 * app.resources.displayMetrics.density).toInt()
        assertTrue("card $card within 80% of $screen", card.first <= screen.first * 0.8 + slack && card.second <= screen.second * 0.8 + slack)
        val aspect = box.first.toFloat() / box.second
        assertTrue("video box $box at the video's aspect", abs(aspect - CourseEndLayout.VIDEO_ASPECT) < 0.02f)
    }

    @Test
    fun aRotationKeepsTheScreenAndItsVideoFitsInBothOrientations() {
        course("Panadol", endInDays = 0, minute)
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil("home") { home() != null }
            confirmOnScreen()
            waitUntil("the course-complete screen", 8_000) { overlayShowing(R.id.courseEnd) }
            waitUntil("laid out") { onHome { it.findViewById<View>(R.id.videoBox).width > 0 } }
            assertSized()
            val before = home()!!
            onMain { before.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            waitUntil("landscape") { home()?.let { now -> onMain { now !== before && now.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE } } == true }
            waitUntil("the screen is back after the rotation") { overlayShowing(R.id.courseEnd) && onHome { it.findViewById<View>(R.id.videoBox).width > 0 } }
            assertEquals("The recorded schedule for Panadol is complete. Bravo!", message())
            assertSized()
            waitUntil("the video goes on", 5_000) { players("USAGE_MEDIA") == 1 }
            onHome { it.findViewById<View>(R.id.courseClose).performClick() }
            waitUntil("released", 3_000) { players("USAGE_MEDIA") == 0 }
            onHome { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }
}
