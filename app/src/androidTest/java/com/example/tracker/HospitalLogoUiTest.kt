package com.example.tracker

import android.app.LocaleManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.LocaleList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.tracker.ui.AboutActivity
import com.example.tracker.ui.Mascots
import com.example.tracker.ui.OnboardingActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The hospital logo (batch 6, R4 and R5): on onboarding above the unchanged hospital name, in the
 * home header instead of the app's name (a heading that still names the app), large on About,
 * always on a white tile. The header must hold on a small screen at font 2.0, in Arabic (mirrored)
 * and in landscape.
 */
@RunWith(AndroidJUnit4::class)
class HospitalLogoUiTest {

    @get:Rule
    val english = EnglishAppLanguage()

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app = ApplicationProvider.getApplicationContext<PillPalsApp>()

    private fun shell(cmd: String): String =
        instr.uiAutomation.executeShellCommand(cmd).use { pfd ->
            java.io.FileInputStream(pfd.fileDescriptor).bufferedReader().readText().trim()
        }

    private fun waitUntil(what: String, check: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end) {
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for: $what")
    }

    private fun <A : android.app.Activity> ActivityScenario<A>.check(what: String, f: (A) -> Boolean) =
        waitUntil(what) {
            var ok = false
            onActivity { ok = f(it) }
            ok
        }

    /** The view shows exactly the hospital logo resource, pixel for pixel. */
    private fun showsHospitalLogo(view: ImageView): Boolean {
        val shown = (view.drawable as? BitmapDrawable)?.bitmap ?: return false
        val logo = BitmapFactory.decodeResource(view.resources, R.drawable.hospital_logo, BitmapFactory.Options().apply { inScaled = false })
        return shown.sameAs(logo)
    }

    private fun assertOnWhiteTile(logo: ImageView) {
        val tile = logo.parent as MaterialCardView
        assertEquals("the logo's tile is white", 0xFFFFFFFF.toInt(), tile.cardBackgroundColor.defaultColor)
        assertEquals(logo.context.getColor(R.color.logo_tile), tile.cardBackgroundColor.defaultColor)
    }

    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun onScreen(view: View): Rect {
        val xy = IntArray(2)
        view.getLocationOnScreen(xy)
        return Rect(xy[0], xy[1], xy[0] + view.width, xy[1] + view.height)
    }

    private fun seedHome(language: String) = runBlocking {
        withContext(Dispatchers.IO) { app.database.clearAllTables() }
        app.repository.setLanguage(language)
        app.repository.setPin("1234")
    }

    @Test
    fun onboardingShowsTheHospitalLogoAboveTheUnchangedHospitalName() {
        runBlocking { withContext(Dispatchers.IO) { app.database.clearAllTables() } }
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            scenario.check("the language step") { it.findViewById<View>(R.id.languageCard).isVisible }
            scenario.onActivity { a ->
                val logo = a.findViewById<ImageView>(R.id.logo)
                assertTrue("the hospital logo, not another picture", showsHospitalLogo(logo))
                assertOnWhiteTile(logo)
                assertEquals("Smouha University Children's Hospital logo", logo.contentDescription)
                // The text under the logo stays exactly the hospital's name, in both languages.
                assertEquals("مستشفى سموحة الجامعي للأطفال", a.findViewById<TextView>(R.id.appNameArabic).text.toString())
                assertEquals("Smouha University Children's Hospital", a.findViewById<TextView>(R.id.appNameEnglish).text.toString())
                assertTrue("the capsule emoji is gone from onboarding", texts(a.window.decorView).none { it.contains(Mascots.PILL) })
            }
        }
    }

    /** The header in its current configuration: logo, summary and settings button all there, apart. */
    private fun assertHeaderHolds(scenario: ActivityScenario<MainActivity>, rtl: Boolean) {
        scenario.check("home with today's summary") {
            val summary = it.findViewById<TextView>(R.id.todaySummary)
            summary.isVisible && summary.text.isNotBlank() && it.findViewById<View>(R.id.headerLogo).width > 0
        }
        scenario.onActivity { a ->
            val logo = a.findViewById<ImageView>(R.id.headerLogo)
            val header = a.findViewById<ViewGroup>(R.id.header)
            val summary = a.findViewById<TextView>(R.id.todaySummary)
            val settings = a.findViewById<View>(R.id.settingsButton)
            assertTrue("the hospital logo in the header", showsHospitalLogo(logo))
            assertOnWhiteTile(logo)
            assertTrue("the logo is the screen's heading", logo.isAccessibilityHeading)
            assertEquals(a.getString(R.string.home_logo_desc), logo.contentDescription)
            assertTrue("no app name text in the header any more", texts(header).none { it == a.getString(R.string.app_name) })
            assertTrue(settings.isVisible)
            val expected = a.resources.getDimensionPixelSize(R.dimen.header_logo_size)
            assertTrue("the logo keeps its size: ${logo.width}x${logo.height}, expected $expected", abs(logo.width - expected) <= 1 && abs(logo.height - expected) <= 1)
            val tile = onScreen(logo.parent as View)
            val gear = onScreen(settings)
            val text = onScreen(summary)
            val box = onScreen(header)
            assertFalse("the logo and the settings button overlap: $tile $gear", Rect.intersects(tile, gear))
            assertTrue("the logo inside the header: $tile in $box", box.contains(tile))
            assertTrue("the settings button inside the header: $gear in $box", box.contains(gear))
            assertTrue("today's summary under the logo: $text under $tile", text.top >= tile.bottom)
            assertEquals("today's summary is not cut", 0, (0 until summary.layout.lineCount).sumOf { summary.layout.getEllipsisCount(it) })
            if (rtl) assertTrue("in Arabic the logo is on the right: $tile, $gear", tile.left > gear.right)
            else assertTrue("in English the logo is on the left: $tile, $gear", tile.right < gear.left)
        }
    }

    @Test
    fun homeHeaderShowsTheLogoInsteadOfTheAppName() {
        seedHome("en")
        ActivityScenario.launch(MainActivity::class.java).use { assertHeaderHolds(it, rtl = false) }
    }

    @Test
    fun headerHoldsOnASmallScreenAtFontTwoInArabicAndInLandscape() {
        // The per-app language is set with LocaleManager (Android 13+, the test AVD is 14).
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        seedHome("ar")
        val locales = app.getSystemService(LocaleManager::class.java)
        val savedLocales = locales.applicationLocales
        try {
            locales.applicationLocales = LocaleList.forLanguageTags("ar-EG-u-nu-latn")
            shell("wm size 480x800")
            shell("wm density 240")
            shell("settings put system font_scale 2.0")
            Thread.sleep(1_500)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.check("Arabic at font 2.0") { it.resources.configuration.fontScale == 2.0f && it.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL }
                assertHeaderHolds(scenario, rtl = true)
                scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
                scenario.check("landscape") { it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
                assertHeaderHolds(scenario, rtl = true)
            }
        } finally {
            shell("wm size reset")
            shell("wm density reset")
            shell("settings put system font_scale 1.0")
            locales.applicationLocales = savedLocales
        }
    }

    @Test
    fun aboutShowsTheLogoLargeAndKeepsTheDisclaimerAndTheDataStatement() {
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.check("About laid out") { it.findViewById<View>(R.id.logo).width > 0 }
            scenario.onActivity { a ->
                val logo = a.findViewById<ImageView>(R.id.logo)
                assertTrue("the hospital logo on About", showsHospitalLogo(logo))
                assertOnWhiteTile(logo)
                assertEquals("Smouha University Children's Hospital logo", logo.contentDescription)
                val large = (160 * a.resources.displayMetrics.density).roundToInt()
                assertTrue("the logo is large: ${logo.width} px", logo.width >= large && logo.height >= large)
                assertEquals("Koko Dawa", a.findViewById<TextView>(R.id.appName).text.toString())
                assertEquals(a.getString(R.string.about_disclaimer), a.findViewById<TextView>(R.id.disclaimer).text.toString())
                assertEquals(a.getString(R.string.about_privacy), a.findViewById<TextView>(R.id.privacy).text.toString())
            }
        }
    }
}
