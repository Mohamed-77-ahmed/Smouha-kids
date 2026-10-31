package com.example.tracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.example.tracker.app
import com.example.tracker.databinding.ActivityOnboardingBinding
import com.example.tracker.reminders.Notifications
import kotlinx.coroutines.launch

/**
 * First launch: language (before anything else), then the parent PIN in the same PIN sheet as
 * everywhere else, with its own keypad, so no system keyboard can cover anything here.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var b: ActivityOnboardingBinding
    private val repository get() = app.repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
        // Under the hospital's logo, the hospital's full name in both languages: no language is picked yet.
        b.appNameArabic.text = AppLanguage.localized(this, AppLanguage.ARABIC).getString(R.string.hospital_name)
        b.appNameEnglish.text = AppLanguage.localized(this, AppLanguage.ENGLISH).getString(R.string.hospital_name)
        Responsive.limitWidth(b.column)
        b.arabicButton.setOnClickListener { chooseLanguage(AppLanguage.ARABIC) }
        b.englishButton.setOnClickListener { chooseLanguage(AppLanguage.ENGLISH) }
        listOf(b.arabicButton, b.englishButton, b.createPinButton).forEach(Motion::springPress)
        b.createPinButton.setOnClickListener { openPinSheet() }
        // The PIN is saved by the sheet; then home opens.
        supportFragmentManager.setFragmentResultListener(PinSheet.REQUEST_KEY, this) { _, result ->
            if (result.getString(PinSheet.RESULT_PURPOSE) == PURPOSE_PIN) openHome()
        }
        lifecycleScope.launch { showStep() }
    }

    private suspend fun showStep() {
        val s = repository.settings()
        when {
            s.languageTag == null -> {
                b.languageCard.isVisible = true
                b.pinCard.isVisible = false
            }
            s.pinHash == null -> {
                b.languageCard.isVisible = false
                b.pinCard.isVisible = true
                openPinSheet()
            }
            else -> openHome()
        }
    }

    /** Not dismissible here: the app cannot be used without a parent PIN. At most one sheet. */
    private fun openPinSheet() {
        PinSheet.create(supportFragmentManager, PURPOSE_PIN, getString(R.string.pin_create_title), cancelable = false)
    }

    private fun chooseLanguage(language: String) {
        lifecycleScope.launch {
            repository.setLanguage(language)
            Notifications.ensureChannel(applicationContext, language)
            // Recreates this screen in the new language; the DB says the next step is the PIN.
            AppLanguage.apply(language)
            showStep()
        }
    }

    private fun openHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val PURPOSE_PIN = "onboarding_pin"
    }
}
