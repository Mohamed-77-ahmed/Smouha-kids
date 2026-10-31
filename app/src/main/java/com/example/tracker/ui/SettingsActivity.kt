package com.example.tracker.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.tracker.MainActivity
import com.example.tracker.R
import com.example.tracker.app
import com.example.tracker.databinding.ActivitySettingsBinding
import com.example.tracker.domain.SettingsAction
import com.example.tracker.domain.SoundStatus
import com.example.tracker.reminders.Notifications
import com.example.tracker.reminders.PhoneSoundSource
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Parent settings. Opens without the PIN; every action goes through [run], which asks for the PIN
 * exactly when [SettingsAction.needsPin] says so, every time.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding
    private val repository get() = app.repository

    /** True while a control is set from code, so only a parent's tap starts an action. */
    private var settingFromCode = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { renderStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        b.toolbar.setNavigationOnClickListener { finish() }
        Responsive.limitWidth(b.content)

        supportFragmentManager.setFragmentResultListener(PinSheet.REQUEST_KEY, this) { _, r ->
            val action = SettingsAction.entries.firstOrNull { it.name == r.getString(PinSheet.RESULT_PURPOSE) }
            if (action != null) perform(action, r.getLong(PinSheet.RESULT_PAYLOAD))
        }
        // Erase all data: first "are you sure", then the PIN, then the erase.
        supportFragmentManager.setFragmentResultListener(ConfirmDialogFragment.REQUEST_KEY, this) { _, r ->
            if (r.getString(ConfirmDialogFragment.RESULT_PURPOSE) == SettingsAction.ERASE_ALL.name &&
                r.getBoolean(ConfirmDialogFragment.RESULT_YES)
            ) {
                run(SettingsAction.ERASE_ALL)
            }
        }

        lifecycleScope.launch {
            val s = repository.settings()
            setFromCode {
                b.languageGroup.check(if (s.languageTag == AppLanguage.ARABIC) R.id.arabicButton else R.id.englishButton)
                b.requirePinSwitch.isChecked = s.requirePinForDose
                b.alarmSoundSwitch.isChecked = s.alarmSoundEnabled
                b.celebrationSoundSwitch.isChecked = s.celebrationSoundEnabled
            }
            b.alarmSoundSwitch.setOnCheckedChangeListener { _, checked ->
                if (settingFromCode) return@setOnCheckedChangeListener
                setFromCode { b.alarmSoundSwitch.isChecked = !checked }
                run(SettingsAction.ALARM_SOUND, if (checked) ON else OFF)
            }
            b.celebrationSoundSwitch.setOnCheckedChangeListener { _, checked ->
                if (settingFromCode) return@setOnCheckedChangeListener
                setFromCode { b.celebrationSoundSwitch.isChecked = !checked }
                run(SettingsAction.CELEBRATION_SOUND, if (checked) ON else OFF)
            }
            b.languageGroup.addOnButtonCheckedListener { _, id, checked ->
                if (checked && !settingFromCode) run(SettingsAction.LANGUAGE, if (id == R.id.arabicButton) ARABIC else ENGLISH)
            }
            b.requirePinSwitch.setOnCheckedChangeListener { _, checked ->
                if (settingFromCode) return@setOnCheckedChangeListener
                // Nothing changes until the PIN is right: put the switch back and ask.
                setFromCode { b.requirePinSwitch.isChecked = !checked }
                run(SettingsAction.TOGGLE_PIN_ON_CONFIRM, if (checked) ON else OFF)
            }
        }
        b.changePinButton.setOnClickListener { run(SettingsAction.CHANGE_PIN) }
        b.manageButton.setOnClickListener { run(SettingsAction.MANAGE_PATIENTS) }
        b.notificationsButton.setOnClickListener { run(SettingsAction.FIX_NOTIFICATIONS) }
        b.exactButton.setOnClickListener { run(SettingsAction.FIX_EXACT_ALARMS) }
        b.batteryButton.setOnClickListener { run(SettingsAction.BATTERY_SETTINGS) }
        b.soundSettingsButton.setOnClickListener { run(SettingsAction.SOUND_SETTINGS) }
        b.dndSettingsButton.setOnClickListener { run(SettingsAction.DND_SETTINGS) }
        b.testButton.setOnClickListener { run(SettingsAction.TEST_REMINDER) }
        b.aboutButton.setOnClickListener { run(SettingsAction.ABOUT) }
        b.versionText.text = versionLabel()
        b.eraseButton.setOnClickListener {
            ConfirmDialogFragment.show(
                supportFragmentManager,
                SettingsAction.ERASE_ALL.name,
                title = getString(R.string.erase_confirm_title),
                message = getString(R.string.erase_confirm_message),
                positive = getString(R.string.erase_confirm_yes),
                negative = getString(R.string.cancel),
            )
        }
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
        // Coming back from the system exact-alarm screen: re-book alarms with the new mode.
        lifecycleScope.launch { app.alarms.rearmAll() }
    }

    /** The only entry point for settings actions: PIN first when the table says so. */
    private fun run(action: SettingsAction, payload: Long = PinSheet.NO_PAYLOAD) {
        if (!action.needsPin) {
            perform(action, payload)
            return
        }
        val fm = supportFragmentManager
        // The sheet's title names the action, its message says why the PIN is asked.
        when (action) {
            SettingsAction.CHANGE_PIN -> PinSheet.change(fm, action.name, getString(R.string.change_pin), getString(R.string.pin_msg_change))
            SettingsAction.MANAGE_PATIENTS -> PinSheet.verify(fm, action.name, getString(R.string.manage_button), getString(R.string.pin_msg_manage), payload)
            SettingsAction.ERASE_ALL -> PinSheet.verify(fm, action.name, getString(R.string.erase_button), getString(R.string.pin_msg_erase), payload)
            SettingsAction.ALARM_SOUND -> PinSheet.verify(fm, action.name, getString(R.string.alarm_sound), getString(R.string.pin_msg_setting), payload)
            SettingsAction.CELEBRATION_SOUND -> PinSheet.verify(fm, action.name, getString(R.string.celebration_sound), getString(R.string.pin_msg_setting), payload)
            else -> PinSheet.verify(fm, action.name, getString(R.string.require_pin), getString(R.string.pin_msg_setting), payload)
        }
    }

    /** Runs an action that is allowed now (free, or the PIN was just accepted). */
    private fun perform(action: SettingsAction, payload: Long) {
        when (action) {
            SettingsAction.TOGGLE_PIN_ON_CONFIRM -> lifecycleScope.launch {
                repository.setRequirePinForDose(payload == ON)
                setFromCode { b.requirePinSwitch.isChecked = payload == ON }
            }
            SettingsAction.ALARM_SOUND -> lifecycleScope.launch {
                repository.setAlarmSound(payload == ON)
                setFromCode { b.alarmSoundSwitch.isChecked = payload == ON }
                renderSound()
            }
            SettingsAction.CELEBRATION_SOUND -> lifecycleScope.launch {
                repository.setCelebrationSound(payload == ON)
                setFromCode { b.celebrationSoundSwitch.isChecked = payload == ON }
            }
            SettingsAction.CHANGE_PIN -> snack(R.string.pin_changed)
            SettingsAction.MANAGE_PATIENTS -> startActivity(Intent(this, ManageActivity::class.java))
            SettingsAction.ERASE_ALL -> eraseAllData()
            SettingsAction.LANGUAGE -> changeLanguage(if (payload == ARABIC) AppLanguage.ARABIC else AppLanguage.ENGLISH)
            SettingsAction.FIX_NOTIFICATIONS -> enableNotifications()
            SettingsAction.FIX_EXACT_ALARMS -> MainActivity.openExactAlarmSettings(this)
            SettingsAction.BATTERY_SETTINGS -> openSystemSettings(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            SettingsAction.SOUND_SETTINGS -> openSystemSettings(Settings.ACTION_SOUND_SETTINGS)
            SettingsAction.DND_SETTINGS -> openSystemSettings(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
            SettingsAction.ABOUT -> startActivity(Intent(this, AboutActivity::class.java))
            SettingsAction.TEST_REMINDER -> {
                app.alarms.scheduleTest()
                snack(R.string.test_scheduled)
            }
        }
    }

    private inline fun setFromCode(block: () -> Unit) {
        settingFromCode = true
        try {
            block()
        } finally {
            settingFromCode = false
        }
    }

    private fun eraseAllData() {
        b.eraseButton.isEnabled = false
        val application = app
        // App scope: a rotation in the middle must not stop the erase halfway.
        application.appScope.launch {
            application.eraseAllData()
            withContext(Dispatchers.Main) {
                // Closes every screen; with no language and no PIN, home sends to the first screen.
                application.startActivity(
                    Intent(application, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
        }
    }

    private fun changeLanguage(language: String) {
        lifecycleScope.launch {
            if (repository.settings().languageTag == language) return@launch
            repository.setLanguage(language)
            Notifications.ensureChannel(applicationContext, language)
            AppLanguage.apply(language)
        }
    }

    private fun enableNotifications() {
        val askable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        lifecycleScope.launch {
            val neverAsked = !repository.settings().notificationRationaleShown
            if (askable && neverAsked) {
                repository.markNotificationRationaleShown()
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                MainActivity.openNotificationSettings(this@SettingsActivity)
            }
        }
    }

    private fun renderStatus() {
        val notificationsOk = Notifications.canPost(this)
        b.notificationsStatus.setText(if (notificationsOk) R.string.notifications_on else R.string.notifications_off)
        b.notificationsButton.isVisible = !notificationsOk
        val exactOk = app.alarms.canScheduleExact()
        b.exactStatus.setText(if (exactOk) R.string.exact_on else R.string.exact_off)
        b.exactButton.isVisible = !exactOk
        // Semantic colors on top of the words (never color alone): red = no reminders, a warning box = late.
        val normal = MaterialColors.getColor(b.exactStatus, com.google.android.material.R.attr.colorOnSurface)
        b.notificationsStatus.setTextColor(if (notificationsOk) normal else getColor(R.color.error))
        warningStyle(b.exactStatus, warning = !exactOk)
        renderSound()
    }

    /**
     * A warning row is a bordered amber box with a warning icon and dark text (palette v2): amber
     * sits between the brand's orange and yellow, so amber words alone would look like a title.
     */
    private fun warningStyle(text: TextView, warning: Boolean) {
        if (warning) {
            val h = resources.getDimensionPixelSize(R.dimen.warning_row_padding_h)
            val v = resources.getDimensionPixelSize(R.dimen.warning_row_padding_v)
            text.setBackgroundResource(R.drawable.bg_warning_row)
            text.setPaddingRelative(h, v, h, v)
            text.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
            text.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.warning_row_icon_gap)
            text.setTextColor(getColor(R.color.on_warning_container))
        } else {
            text.background = null
            text.setPaddingRelative(0, 0, 0, 0)
            text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
            text.setTextColor(MaterialColors.getColor(text, com.google.android.material.R.attr.colorOnSurface))
        }
        text.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = if (warning) resources.getDimensionPixelSize(R.dimen.warning_row_gap) else 0
        }
    }

    /** Alarm volume, Do Not Disturb and battery rows: read-only, no PIN (CLAUDE.md 7). */
    private fun renderSound() {
        lifecycleScope.launch {
            val source = PhoneSoundSource(this@SettingsActivity)
            val report = SoundStatus.check(source, alarmSoundOn = repository.settings().alarmSoundEnabled)
            b.silentNote.isVisible = report.showSilentNote
            show(report.volume, b.alarmVolumeStatus, b.soundSettingsButton) {
                if (it == SoundStatus.Text.VOLUME_ZERO) getString(R.string.alarm_volume_zero)
                else getString(R.string.alarm_volume_level, source.alarmVolume, source.alarmVolumeMax)
            }
            show(report.doNotDisturb, b.dndStatus, b.dndSettingsButton) {
                getString(
                    when (it) {
                        SoundStatus.Text.DND_BLOCKS_ALARMS -> R.string.dnd_blocks_alarms
                        SoundStatus.Text.DND_ALLOWS_ALARMS -> R.string.dnd_allows_alarms
                        else -> R.string.dnd_off
                    }
                )
            }
            show(report.battery, b.batteryStatus, b.batteryButton) {
                getString(if (it == SoundStatus.Text.BATTERY_UNRESTRICTED) R.string.battery_unrestricted else R.string.battery_optimized)
            }
        }
    }

    private inline fun show(row: SoundStatus.Row, text: TextView, button: View, words: (SoundStatus.Text) -> String) {
        text.text = words(row.text)
        // The warning box and icon next to words that say what is wrong: never color alone.
        warningStyle(text, warning = row.level == SoundStatus.Level.WARNING)
        button.isVisible = row.showButton
    }

    private fun openSystemSettings(action: String) {
        try {
            startActivity(Intent(action))
        } catch (_: ActivityNotFoundException) {
            // Some phones hide a screen; the main settings page is the next best place.
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun versionLabel(): String = AboutActivity.versionLabel(this)

    private fun snack(text: Int) = Snackbar.make(b.root, text, Snackbar.LENGTH_LONG).show()

    private companion object {
        const val ON = 1L
        const val OFF = 0L
        const val ARABIC = 0L
        const val ENGLISH = 1L
    }
}
