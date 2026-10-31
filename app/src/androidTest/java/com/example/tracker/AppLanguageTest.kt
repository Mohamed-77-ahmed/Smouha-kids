package com.example.tracker

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tracker.data.TrackerRepository
import com.example.tracker.data.db.AppDatabase
import com.example.tracker.domain.PinHasher
import com.example.tracker.ui.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Android 13+ per-app language screen (CLAUDE.md 9b): a choice made there wins over the stored one,
 * also when the app starts without any activity (Application.onCreate, alarm receivers).
 */
@RunWith(AndroidJUnit4::class)
class AppLanguageTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val localeManager = context.getSystemService(LocaleManager::class.java)
    private lateinit var saved: LocaleList
    private lateinit var db: AppDatabase
    private lateinit var repo: TrackerRepository

    @Before
    fun setUp() {
        saved = localeManager.applicationLocales
        db = AppDatabase.inMemory(context)
        repo = TrackerRepository(db, TestClock(LocalDate.parse("2026-10-12").atTime(9, 0)), PinHasher(iterations = 1_000))
    }

    @After
    fun tearDown() {
        localeManager.applicationLocales = saved
        db.close()
    }

    @Test
    fun systemChoiceWinsOverTheStoredOneWithoutAnyActivity() = runBlocking {
        repo.setLanguage(AppLanguage.ARABIC)
        localeManager.applicationLocales = LocaleList.forLanguageTags("en")

        AppLanguage.syncWithSystem(context, repo)

        assertEquals(AppLanguage.ENGLISH, repo.settings().languageTag)
        assertEquals("en", localeManager.applicationLocales.toLanguageTags())
        assertEquals(AppLanguage.ENGLISH, AppLanguage.effective(AppLanguage.ARABIC, context))
    }

    @Test
    fun storedChoiceIsAppliedWhenTheSystemHasNone() = runBlocking {
        repo.setLanguage(AppLanguage.ARABIC)
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()

        AppLanguage.syncWithSystem(context, repo)

        assertEquals(AppLanguage.ARABIC, repo.settings().languageTag)
        assertEquals("ar-EG-u-nu-latn", localeManager.applicationLocales.toLanguageTags())
    }
}
