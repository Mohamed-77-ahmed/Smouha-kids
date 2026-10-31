package com.example.tracker

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/**
 * Runs a test with the app's per-app language set to English and puts the previous one back after.
 * The per-app language (Android 13+) wins over the stored choice by design (CLAUDE.md 9b,
 * AppLanguageTest), so a test that reads English texts must not depend on what the emulator was
 * left with (seen: a test AVD started again with Arabic, and every English text check failed).
 */
class EnglishAppLanguage : ExternalResource() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var saved: LocaleList? = null

    override fun before() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = context.getSystemService(LocaleManager::class.java)
        saved = manager.applicationLocales
        manager.applicationLocales = LocaleList.forLanguageTags("en")
        waitUntil { manager.applicationLocales.toLanguageTags() == "en" }
    }

    override fun after() {
        val previous = saved ?: return
        val manager = context.getSystemService(LocaleManager::class.java)
        manager.applicationLocales = previous
        waitUntil { manager.applicationLocales == previous }
    }

    private fun waitUntil(check: () -> Boolean) {
        val end = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < end) {
            if (check()) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for the app language")
    }
}
