package com.example.tracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.ui.AppLanguage
import com.example.tracker.ui.Fmt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Arabic plurals, digits, time format and week order, on the device's own resources and ICU. */
@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    private val base: Context = ApplicationProvider.getApplicationContext()
    private val ar = AppLanguage.localized(base, AppLanguage.ARABIC)
    private val en = AppLanguage.localized(base, AppLanguage.ENGLISH)
    private val easternDigits = Regex("[٠-٩۰-۹]")

    @Test
    fun arabicDayPluralsFollowEgyptianForms() {
        val days = listOf(0, 1, 2, 3, 10, 11, 100).map { ar.resources.getQuantityString(R.plurals.days_count, it, it) }
        assertEquals(listOf("0 يوم", "يوم واحد", "يومين", "3 أيام", "10 أيام", "11 يوم", "100 يوم"), days)
    }

    @Test
    fun arabicPointPlurals() {
        val points = listOf(1, 2, 3, 11, 15, 100).map { ar.resources.getQuantityString(R.plurals.points_count, it, it) }
        assertEquals(listOf("نقطة واحدة", "نقطتين", "3 نقط", "11 نقطة", "15 نقطة", "100 نقطة"), points)
        assertEquals("⁦+15⁩ نقطة", ar.resources.getQuantityString(R.plurals.plus_points, 15, 15))
        assertEquals("5 دقايق", ar.resources.getQuantityString(R.plurals.minutes_short, 5, 5))
        assertEquals("15 دقيقة", ar.resources.getQuantityString(R.plurals.minutes_short, 15, 15))
    }

    @Test
    fun pinAndNameLimitsUseTheRightPluralForm() {
        // The real limits (PIN 4 to 8 digits, names up to 40 letters) read as before the plurals.
        assertEquals("لازم من 4 لـ 8 أرقام", ar.resources.getQuantityString(R.plurals.pin_format_error, 8, 4, 8))
        assertEquals("Use 4 to 8 digits", en.resources.getQuantityString(R.plurals.pin_format_error, 8, 4, 8))
        assertEquals("أقصى حاجة 40 حرف.", ar.resources.getQuantityString(R.plurals.err_name_too_long, 40, 40))
        assertEquals("أقصى حاجة 10 حروف.", ar.resources.getQuantityString(R.plurals.err_name_too_long, 10, 10))
        assertEquals("At most 40 letters.", en.resources.getQuantityString(R.plurals.err_name_too_long, 40, 40))
    }

    @Test
    fun bareNumbersStayWesternUnderAnArabicDefaultLocale() {
        val saved = Locale.getDefault()
        try {
            // Plain ar-EG formats with Eastern Arabic digits by default.
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("1250", Fmt.number(1250))
            assertEquals("0", Fmt.number(0))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun englishPlurals() {
        assertEquals("1 day", en.resources.getQuantityString(R.plurals.days_count, 1, 1))
        assertEquals("2 days", en.resources.getQuantityString(R.plurals.days_count, 2, 2))
        assertEquals("1 point", en.resources.getQuantityString(R.plurals.points_count, 1, 1))
    }

    @Test
    fun arabicUsesWesternDigitsEverywhere() {
        val samples = listOf(
            ar.getString(R.string.today_all, 3, 12),
            ar.getString(R.string.level_hint, ar.resources.getQuantityString(R.plurals.points_count, 45, 45), 2),
            ar.resources.getQuantityString(R.plurals.pin_format_error, 8, 4, 8),
            Fmt.time(ar, 21 * 60 + 5),
            Fmt.date(ar, java.time.LocalDate.parse("2026-10-23").toEpochDay()),
        )
        samples.forEach { assertFalse("Eastern digits in: $it", easternDigits.containsMatchIn(it)) }
        assertEquals("النهاردة: اتاخد 3 من 12", samples[0])
    }

    @Test
    fun arabicTimeUsesSabahAndMasa() {
        val morning = Fmt.time(ar, 9 * 60)
        val evening = Fmt.time(ar, 21 * 60)
        assertTrue(morning, morning.contains("9:00") && morning.contains("ص"))
        assertTrue(evening, evening.contains("9:00") && evening.contains("م"))
    }

    @Test
    fun weekStartsSaturdayInArabicAndSundayInEnglish() {
        assertEquals(listOf(6, 7, 1, 2, 3, 4, 5), Fmt.weekOrder(ar))
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), Fmt.weekOrder(en))
    }

    @Test
    fun latinNamesInsideArabicSentencesAreIsolated() {
        val wrapped = Fmt.bidi(ar, "Augmentin 1g")
        val sentence = ar.getString(R.string.delete_medicine_title, wrapped)
        assertTrue(sentence.contains("Augmentin 1g"))
        assertTrue("isolated: $sentence", sentence.length > "مسح Augmentin 1g؟".length)
    }

    @Test
    fun appIdentityKokoDawaAndTheHospitalNameApart() {
        assertEquals("Koko Dawa", en.getString(R.string.app_name))
        assertEquals("كوكو دوا", ar.getString(R.string.app_name))
        // The hospital keeps its own name (onboarding, About); it never names the app.
        assertEquals("Smouha University Children's Hospital", en.getString(R.string.hospital_name))
        assertEquals("مستشفى سموحة الجامعي للأطفال", ar.getString(R.string.hospital_name))
        // What Android reads: the application label is Koko Dawa, and the launcher activity has no label
        // of its own, so the launcher shows the same name.
        assertEquals(R.string.app_name, base.applicationInfo.labelRes)
        val launcher = checkNotNull(base.packageManager.getLaunchIntentForPackage(base.packageName)).component!!
        @Suppress("DEPRECATION") // the flags overload is API 33+; this test runs from API 28
        val info = base.packageManager.getActivityInfo(launcher, 0)
        assertEquals(0, info.labelRes)
        assertEquals(base.applicationInfo.loadLabel(base.packageManager).toString(), info.loadLabel(base.packageManager).toString())
    }
}
