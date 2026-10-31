package com.example.tracker.ui

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.tracker.data.TrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App language. The choice is stored in the DB (source of truth) and applied with AppCompat
 * per-app locales. Arabic uses the Egypt region (Saturday first) and Latin digits.
 */
object AppLanguage {
    const val ARABIC = "ar"
    const val ENGLISH = "en"

    fun localeTag(language: String): String =
        if (language == ARABIC) "ar-EG-u-nu-latn" else "en"

    /** With a [context] on Android 13+, sets the system per-app language directly (works with no activity). */
    fun apply(language: String, context: Context? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context != null) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(localeTag(language))
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag(language)))
        }
    }

    /**
     * Language set through AppCompat or the Android 13+ per-app language screen, if any. On 13+ it
     * asks the system directly: AppCompat answers "none" while no activity exists yet (for example
     * in Application.onCreate or a receiver), which used to re-apply the stored language over it.
     */
    fun systemChoice(context: Context? = null): String? {
        val tags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context != null) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
        return when (tags.substringBefore(',').substringBefore('-').ifEmpty { null }) {
            ARABIC -> ARABIC
            ENGLISH -> ENGLISH
            else -> null
        }
    }

    /** Language to use right now (e.g. for a notification), falling back to the stored choice. */
    fun effective(stored: String?, context: Context? = null): String? = systemChoice(context) ?: stored

    /** A context whose resources use [language], for code that runs without an activity. */
    fun localized(context: Context, language: String?): Context {
        if (language == null) return context
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(localeTag(language)))
        return context.createConfigurationContext(config)
    }

    /**
     * Keeps DB and system in step: a change made in Android's per-app language screen wins and is
     * saved to the DB; a stored choice the system forgot is applied again.
     */
    suspend fun syncWithSystem(context: Context, repository: TrackerRepository) {
        val stored = repository.settings().languageTag ?: return
        val system = withContext(Dispatchers.Main) { systemChoice(context) }
        when {
            system != null && system != stored -> repository.setLanguage(system)
            system == null -> withContext(Dispatchers.Main) { apply(stored, context) }
        }
    }
}
