package com.example.tracker

import android.app.Activity
import android.app.LocaleManager
import android.os.LocaleList
import android.widget.CompoundButton
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.tracker.ui.ManageActivity
import com.example.tracker.ui.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Parent settings gates (CLAUDE.md 8), on the real screens with the in-memory DB of [TestApp].
 * Target: a wrong PIN on each gated action changes nothing. Control: free actions need no PIN.
 */
@RunWith(AndroidJUnit4::class)
class SettingsGatesUiTest {

    private val app = ApplicationProvider.getApplicationContext<PillPalsApp>()
    private val repo get() = app.repository
    private lateinit var savedLocales: LocaleList

    @Before
    fun setUp() = runBlocking {
        savedLocales = localeManager.applicationLocales
        withContext(Dispatchers.IO) { app.database.clearAllTables() }
        repo.setLanguage("en")
        repo.setPin("1234")
        repo.addPatient("Yasmin", 7)
        Unit
    }

    @After
    fun tearDown() {
        // The language test switches the app language; give the device its own setting back. Through
        // the system service: with no activity left, AppCompat would only remember it in memory.
        localeManager.applicationLocales = savedLocales
        waitUntil("device language restored") { localeManager.applicationLocales == savedLocales }
    }

    private val localeManager get() = app.getSystemService(LocaleManager::class.java)

    private fun appLocales(): LocaleList = localeManager.applicationLocales

    private fun waitUntil(what: String, check: suspend () -> Boolean) {
        val end = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < end) {
            if (runBlocking { check() }) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    /** Types on the PIN sheet's keypad and presses OK, then waits until the check finished. */
    private fun enterPin(pin: String, attemptsAfter: Int) {
        PinPad.type(pin)
        waitUntil("PIN checked") { repo.settings().pinFailedAttempts == attemptsAfter }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun resumed(): Activity? {
        var top: Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            top = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return top
    }

    // ------------------------------------------------------------------ control: free

    @Test
    fun settingsOpenFromHomeWithoutPin() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntil("home shown") { resumed() is MainActivity }
            onView(withId(R.id.settingsButton)).perform(click())
            waitUntil("settings open") { resumed() is SettingsActivity }
            onView(withId(R.id.keypad)).check(doesNotExist())
        }
    }

    @Test
    fun languageChangesWithoutPin() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.arabicButton)).perform(click())
            waitUntil("language saved") { repo.settings().languageTag == "ar" }
            // Saved first, applied right after: wait for both, so tearDown's restore comes last.
            waitUntil("language applied") { appLocales().toLanguageTags().startsWith("ar") }
            onView(withId(R.id.keypad)).check(doesNotExist())
            assertEquals(0, runBlocking { repo.settings().pinFailedAttempts })
        }
    }

    @Test
    fun aboutOpensWithoutPinAndShowsTheInstalledVersion() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.aboutButton)).perform(scrollTo(), click())
            waitUntil("About open") { resumed() is com.example.tracker.ui.AboutActivity }
            onView(withId(R.id.keypad)).check(doesNotExist())
            val pm = app.packageManager
            @Suppress("DEPRECATION")
            val installed = pm.getPackageInfo(app.packageName, 0).versionName!!
            onView(withId(R.id.version)).check(matches(withText(org.hamcrest.Matchers.containsString(installed))))
            onView(withId(R.id.disclaimer)).check(matches(withText(R.string.about_disclaimer)))
            assertEquals(0, runBlocking { repo.settings().pinFailedAttempts })
        }
    }

    // ------------------------------------------------------------------ target: gated

    @Test
    fun wrongPinLeavesPinOnConfirmUnchanged() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.requirePinSwitch)).check(matches(isChecked()))
            onView(withId(R.id.requirePinSwitch)).perform(click())
            enterPin("0000", attemptsAfter = 1)
            assertTrue(runBlocking { repo.settings().requirePinForDose })
            // The PIN sheet is still open, so read the switch from the screen behind it.
            scenario.onActivity {
                assertTrue(it.findViewById<CompoundButton>(R.id.requirePinSwitch).isChecked)
            }
        }
    }

    @Test
    fun wrongPinLeavesAlarmSoundUnchanged() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.alarmSoundSwitch)).perform(scrollTo(), click())
            enterPin("0000", attemptsAfter = 1)
            assertTrue(runBlocking { repo.settings().alarmSoundEnabled })
            scenario.onActivity {
                assertTrue(it.findViewById<CompoundButton>(R.id.alarmSoundSwitch).isChecked)
            }
        }
    }

    @Test
    fun rightPinTurnsAlarmSoundOff() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.alarmSoundSwitch)).perform(scrollTo(), click())
            enterPin("1234", attemptsAfter = 0)
            waitUntil("alarm sound saved") { !repo.settings().alarmSoundEnabled }
        }
    }

    @Test
    fun wrongPinLeavesCelebrationSoundUnchanged() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.celebrationSoundSwitch)).perform(scrollTo(), click())
            enterPin("0000", attemptsAfter = 1)
            assertTrue(runBlocking { repo.settings().celebrationSoundEnabled })
            scenario.onActivity {
                assertTrue(it.findViewById<CompoundButton>(R.id.celebrationSoundSwitch).isChecked)
            }
        }
    }

    @Test
    fun rightPinTurnsCelebrationSoundOffAndLeavesTheAlarmSound() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.celebrationSoundSwitch)).perform(scrollTo(), click())
            enterPin("1234", attemptsAfter = 0)
            waitUntil("celebration sound saved") { !repo.settings().celebrationSoundEnabled }
            assertTrue("the control: the alarm sound stays on", runBlocking { repo.settings().alarmSoundEnabled })
        }
    }

    @Test
    fun rightPinChangesPinOnConfirm() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.requirePinSwitch)).perform(click())
            enterPin("1234", attemptsAfter = 0)
            waitUntil("toggle saved") { !repo.settings().requirePinForDose }
        }
    }

    @Test
    fun wrongPinDoesNotStartChangePin() {
        val hashBefore = runBlocking { repo.settings().pinHash }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.changePinButton)).perform(click())
            enterPin("0000", attemptsAfter = 1)
            // Still on step 1 (the current PIN), never on the new-PIN steps; hash untouched.
            var stepOne = ""
            scenario.onActivity { stepOne = it.getString(R.string.pin_step, 1, 3, it.getString(R.string.pin_step_current)) }
            onView(withId(R.id.stepLabel)).inRoot(isDialog()).check(matches(withText(stepOne)))
            assertEquals(hashBefore, runBlocking { repo.settings().pinHash })
        }
    }

    @Test
    fun wrongPinDoesNotOpenManage() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.manageButton)).perform(scrollTo(), click())
            enterPin("0000", attemptsAfter = 1)
            Thread.sleep(500)
            assertTrue(resumed() !is ManageActivity)
        }
    }

    @Test
    fun wrongPinErasesNothing() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            onView(withId(R.id.eraseButton)).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            enterPin("0000", attemptsAfter = 1)
            Thread.sleep(500)
            assertEquals(1, runBlocking { repo.patients().size })
            assertTrue(runBlocking { repo.hasPin() })
        }
    }
}
