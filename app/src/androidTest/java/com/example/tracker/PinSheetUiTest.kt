package com.example.tracker

import android.app.Activity
import android.app.LocaleManager
import android.os.Bundle
import android.os.LocaleList
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.example.tracker.data.PinCheck
import com.example.tracker.ui.OnboardingActivity
import com.example.tracker.ui.PinSheet
import com.example.tracker.ui.SettingsActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The PIN sheet on the real screens (CLAUDE.md 8 and 9), with the in-memory DB of [TestApp]:
 * wrong PIN, lockout, rotation, no digits in saved state, one sheet at a time, a left-to-right
 * keypad in Arabic, animations off, and the first-launch PIN without a system keyboard.
 */
@RunWith(AndroidJUnit4::class)
class PinSheetUiTest {

    private val app = ApplicationProvider.getApplicationContext<PillPalsApp>()
    private val repo get() = app.repository
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val localeManager get() = app.getSystemService(LocaleManager::class.java)
    private lateinit var savedLocales: LocaleList
    private var savedAnimatorScale: String = "1"

    @Before
    fun setUp() = runBlocking {
        savedLocales = localeManager.applicationLocales
        savedAnimatorScale = shell("settings get global animator_duration_scale").trim().ifEmpty { "1" }.let { if (it == "null") "1" else it }
        withContext(Dispatchers.IO) { app.database.clearAllTables() }
        repo.setLanguage("en")
        repo.setPin("1234")
        Unit
    }

    @After
    fun tearDown() {
        shell("settings put global animator_duration_scale $savedAnimatorScale")
        localeManager.applicationLocales = savedLocales
        waitUntil("device language restored") { localeManager.applicationLocales == savedLocales }
    }

    private fun shell(command: String): String {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
    }

    private fun waitUntil(what: String, timeoutMillis: Long = 20_000, check: suspend () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < end) {
            if (runBlocking { check() }) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    private fun sheets(activity: AppCompatActivity) =
        activity.supportFragmentManager.fragments.count { it is PinSheet }

    private fun sheetView(activity: AppCompatActivity, id: Int): View =
        (activity.supportFragmentManager.findFragmentByTag(PinSheet.TAG) as PinSheet).requireView().findViewById(id)

    private fun text(scenario: ActivityScenario<SettingsActivity>, id: Int): String {
        var t = ""
        scenario.onActivity { t = (sheetView(it, id) as android.widget.TextView).text.toString() }
        return t
    }

    private fun openChangePin(): ActivityScenario<SettingsActivity> =
        ActivityScenario.launch(SettingsActivity::class.java).also {
            onView(withId(R.id.changePinButton)).perform(scrollTo(), click())
            onView(withId(R.id.keypad)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
        }

    /** Change PIN up to its second step: the current PIN accepted. */
    private fun toNewPinStep(scenario: ActivityScenario<SettingsActivity>) {
        PinPad.type("1234")
        var stepTwo = ""
        scenario.onActivity { stepTwo = it.getString(R.string.pin_step, 2, 3, it.resources.getQuantityString(R.plurals.pin_step_new, 8, 4, 8)) }
        waitUntil("step 2") { text(scenario, R.id.stepLabel) == stepTwo }
        awaitKeys(scenario)
    }

    /** After a check the keys are off for a moment; the next taps must wait for them. */
    private fun awaitKeys(scenario: ActivityScenario<SettingsActivity>) {
        waitUntil("keys on") { var on = false; scenario.onActivity { on = sheetView(it, R.id.key1).isEnabled }; on }
    }

    // ------------------------------------------------------------------ wrong PIN and lockout

    @Test
    fun wrongPinSaysTriesLeftClearsTheDotsAndChangesNothing() {
        openChangePin().use { scenario ->
            val hash = runBlocking { repo.settings().pinHash }
            PinPad.type("0000")
            waitUntil("PIN checked") { repo.settings().pinFailedAttempts == 1 }
            var expected = ""
            var dots = ""
            scenario.onActivity {
                expected = it.resources.getQuantityString(R.plurals.pin_wrong_attempts, 4, 4)
                dots = it.resources.getQuantityString(R.plurals.pin_dots_entered, 0, 0)
            }
            waitUntil("tries left shown") { text(scenario, R.id.status) == expected }
            scenario.onActivity { assertEquals(dots, sheetView(it, R.id.dots).contentDescription) }
            assertEquals(hash, runBlocking { repo.settings().pinHash })
        }
    }

    @Test
    fun lockoutShowsACountdownAndBlocksTheKeypadAndOk() {
        openChangePin().use { scenario ->
            repeat(5) { attempt ->
                PinPad.type("0000")
                if (attempt < 4) {
                    waitUntil("attempt ${attempt + 1} counted") { repo.settings().pinFailedAttempts == attempt + 1 }
                    awaitKeys(scenario)
                }
            }
            waitUntil("locked") { repo.pinLockRemainingMillis() > 0 }
            waitUntil("keypad off") {
                var off = false
                scenario.onActivity { off = !sheetView(it, R.id.key1).isEnabled && !sheetView(it, R.id.keyOk).isEnabled }
                off
            }
            // The countdown shows the seconds left of the stored lock (one tick of slack).
            val seconds = com.example.tracker.domain.PinEntry.seconds(runBlocking { repo.pinLockRemainingMillis() })
            val texts = listOf(seconds, seconds + 1).map { app.resources.getQuantityString(R.plurals.pin_locked, it, it) }
            val shown = text(scenario, R.id.status)
            assertTrue("countdown shown: '$shown' in $texts", shown in texts)
            // A tap on a key while locked types nothing.
            scenario.onActivity { (sheetView(it, R.id.key5) as MaterialButton).performClick() }
            var dots = ""
            scenario.onActivity { dots = sheetView(it, R.id.dots).contentDescription.toString() }
            assertEquals(app.resources.getQuantityString(R.plurals.pin_dots_entered, 0, 0), dots)
        }
    }

    // ------------------------------------------------------------------ rotation and saved state

    @Test
    fun rotationKeepsTheStepClearsDigitsAndKeepsOneSheet() {
        openChangePin().use { scenario ->
            toNewPinStep(scenario)
            PinPad.type("57", ok = false)
            scenario.recreate()
            var stepTwo = ""
            var noDigits = ""
            scenario.onActivity {
                stepTwo = it.getString(R.string.pin_step, 2, 3, it.resources.getQuantityString(R.plurals.pin_step_new, 8, 4, 8))
                noDigits = it.resources.getQuantityString(R.plurals.pin_dots_entered, 0, 0)
            }
            waitUntil("sheet back") { var n = 0; scenario.onActivity { n = sheets(it) }; n == 1 }
            assertEquals(stepTwo, text(scenario, R.id.stepLabel))
            scenario.onActivity { assertEquals(noDigits, sheetView(it, R.id.dots).contentDescription) }
            // The flow goes on from there: the new PIN twice, then it is saved.
            val hashBefore = runBlocking { repo.settings().pinHash }
            awaitKeys(scenario)
            PinPad.type("5555")
            PinPad.type("5555")
            waitUntil("new PIN saved") { repo.settings().pinHash != hashBefore }
            assertEquals(PinCheck.Ok, runBlocking { repo.verifyPin("5555") })
        }
    }

    @Test
    fun typedDigitsNeverReachSavedState() {
        openChangePin().use { scenario ->
            toNewPinStep(scenario)
            PinPad.type("8642")              // the first new PIN, now kept in memory only
            PinPad.type("9753", ok = false)  // typed digits of the repeat
            var bytes = ByteArray(0)
            scenario.onActivity { activity ->
                val state = Bundle()
                instrumentation.callActivityOnSaveInstanceState(activity, state)
                val parcel = Parcel.obtain()
                try {
                    state.writeToParcel(parcel, 0)
                    bytes = parcel.marshall()
                } finally {
                    parcel.recycle()
                }
            }
            // Strings are written as UTF-16 in a Parcel.
            fun contains(digits: String) = String(bytes, Charsets.UTF_16LE).contains(digits) ||
                String(bytes, Charsets.UTF_8).contains(digits)
            assertTrue("saved state was written", bytes.isNotEmpty())
            assertFalse("the new PIN is not in saved state", contains("8642"))
            assertFalse("typed digits are not in saved state", contains("9753"))
        }
    }

    @Test
    fun oneSheetAtATime() {
        openChangePin().use { scenario ->
            var second = true
            scenario.onActivity {
                second = PinSheet.verify(it.supportFragmentManager, "again", "title", null)
            }
            assertFalse("a second sheet was refused", second)
            scenario.onActivity { assertEquals(1, sheets(it)) }
        }
    }

    // ------------------------------------------------------------------ RTL, animations off

    @Test
    fun keypadStaysLeftToRightInArabicWhileTheSheetMirrors() {
        localeManager.applicationLocales = LocaleList.forLanguageTags("ar-EG-u-nu-latn")
        waitUntil("Arabic applied") { localeManager.applicationLocales.toLanguageTags().startsWith("ar") }
        openChangePin().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.LAYOUT_DIRECTION_RTL, sheetView(activity, R.id.title).layoutDirection)
                assertEquals(View.LAYOUT_DIRECTION_LTR, sheetView(activity, R.id.key1).layoutDirection)
                val x = listOf(R.id.key1, R.id.key2, R.id.key3).map { id ->
                    IntArray(2).also { sheetView(activity, id).getLocationOnScreen(it) }[0]
                }
                assertTrue("1, 2, 3 from left to right: $x", x[0] < x[1] && x[1] < x[2])
                assertEquals("1", (sheetView(activity, R.id.key1) as MaterialButton).text.toString())
                assertEquals("0", (sheetView(activity, R.id.key0) as MaterialButton).text.toString())
            }
        }
    }

    @Test
    fun animationsOffNoShakeAndNoHang() {
        shell("settings put global animator_duration_scale 0")
        waitUntil("animations off in the app") { var on = true; instrumentation.runOnMainSync { on = android.animation.ValueAnimator.areAnimatorsEnabled() }; !on }
        openChangePin().use { scenario ->
            PinPad.type("0000")
            waitUntil("PIN checked") { repo.settings().pinFailedAttempts == 1 }
            var expected = ""
            scenario.onActivity { expected = it.resources.getQuantityString(R.plurals.pin_wrong_attempts, 4, 4) }
            waitUntil("tries left shown", 5_000) { text(scenario, R.id.status) == expected }
            scenario.onActivity { assertEquals(0f, sheetView(it, R.id.dots).translationX) }
            // The keypad still works right away.
            awaitKeys(scenario)
            PinPad.type("1234")
            var stepTwo = ""
            scenario.onActivity { stepTwo = it.getString(R.string.pin_step, 2, 3, it.resources.getQuantityString(R.plurals.pin_step_new, 8, 4, 8)) }
            waitUntil("step 2 after the right PIN") { text(scenario, R.id.stepLabel) == stepTwo }
        }
    }

    // ------------------------------------------------------------------ first launch

    @Test
    fun firstLaunchCreatesThePinOnTheKeypadWithoutAKeyboard() {
        runBlocking { withContext(Dispatchers.IO) { app.database.clearAllTables() }; repo.setLanguage("en") }
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            onView(withId(R.id.keypad)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
            // No text field on the screen or in the sheet, so no system keyboard can open.
            scenario.onActivity { activity ->
                assertEquals(0, editTexts(activity.window.decorView))
                val sheet = activity.supportFragmentManager.findFragmentByTag(PinSheet.TAG) as PinSheet
                assertEquals(0, editTexts(sheet.requireDialog().window!!.decorView))
            }
            // The sheet cannot be dismissed on the first screen.
            pressBack()
            onView(withId(R.id.keypad)).inRoot(isDialog()).check(matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
            PinPad.type("2580")
            PinPad.type("2580")
            waitUntil("PIN saved and home open") {
                repo.hasPin() && resumed() is MainActivity
            }
            assertEquals(PinCheck.Ok, runBlocking { repo.verifyPin("2580") })
        }
    }

    private fun editTexts(view: View): Int = when (view) {
        is EditText -> 1
        is android.view.ViewGroup -> (0 until view.childCount).sumOf { editTexts(view.getChildAt(it)) }
        else -> 0
    }

    private fun resumed(): Activity? {
        var top: Activity? = null
        instrumentation.runOnMainSync {
            top = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()
        }
        return top
    }

    @Test
    fun sheetTitleNamesTheAction() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.alarmSoundSwitch)).perform(scrollTo(), click())
            var title = ""
            scenario.onActivity { title = it.getString(R.string.alarm_sound) }
            onView(withId(R.id.title)).inRoot(isDialog()).check(matches(withText(title)))
        }
    }
}
