package com.example.tracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tracker.data.ConfirmOutcome
import com.example.tracker.data.HomeData
import com.example.tracker.data.HomePatient
import com.example.tracker.data.db.DoseRow
import com.example.tracker.databinding.ActivityMainBinding
import com.example.tracker.domain.DoseStatus
import com.example.tracker.domain.Gender
import com.example.tracker.domain.SoundPolicy
import com.example.tracker.reminders.AlarmSound
import com.example.tracker.reminders.AlarmSoundService
import com.example.tracker.reminders.Notifications
import com.example.tracker.ui.Celebration
import com.example.tracker.ui.CelebrationSound
import com.example.tracker.ui.ConfirmDialogFragment
import com.example.tracker.ui.CourseEndScreen
import com.example.tracker.ui.Fmt
import com.example.tracker.ui.HomeAdapter
import com.example.tracker.ui.HomeBlock
import com.example.tracker.ui.Mascots
import com.example.tracker.ui.Motion
import com.example.tracker.ui.MedicineEditActivity
import com.example.tracker.ui.OnboardingActivity
import com.example.tracker.ui.PinSheet
import com.example.tracker.ui.Responsive
import com.example.tracker.ui.SettingsActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var celebration: Celebration
    /** The reward's short sound; stopped and released whenever this screen stops. */
    private val celebrationSound by lazy { CelebrationSound(this) }
    private lateinit var courseEnd: CourseEndScreen
    /** A course's final dose was confirmed: its course-complete screen waits for the reward to close. */
    private var pendingCourseEnd: String? = null
    private var pendingCourseEndSound = true
    private val closeCourseEndOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = courseEnd.close()
    }
    private val repository get() = app.repository
    private val adapter = HomeAdapter(onConfirm = ::onConfirmClicked, onEdit = ::onEditClicked)

    private var home: HomeData? = null
    /** Day shown on home. Setting a new day (after midnight) switches the single collector. */
    private val observedDay = MutableStateFlow<Long?>(null)
    private var ready = false
    private var chipKeys: List<String> = emptyList()
    private var pendingDoseFromIntent: Long? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { renderBanner() }

    private val editor = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data?.getBooleanExtra(MedicineEditActivity.RESULT_ADDED, false) == true) {
            askNotificationPermissionIfNeeded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Launch screen (Theme.Tracker.Starting) until the first frame; then the app theme.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.main.isVisible = false
        applyInsets()

        // When the reward closes, a course-complete screen comes next if one waits; then a sibling's
        // pending streak-broken animation.
        celebration = Celebration(b.celebration, celebrationSound) { afterReward() }
        courseEnd = CourseEndScreen(b.courseEnd) { showing ->
            closeCourseEndOnBack.isEnabled = showing
            if (!showing) home?.let { showStreakBreakIfPending(it) }
        }
        onBackPressedDispatcher.addCallback(this, closeCourseEndOnBack)
        savedInstanceState?.let {
            pendingCourseEnd = it.getString(PENDING_COURSE_END)
            pendingCourseEndSound = it.getBoolean(PENDING_COURSE_END_SOUND, true)
        }
        celebration.restore(savedInstanceState)
        courseEnd.restore(savedInstanceState)
        // One column on phones, two from 840dp; each column at most 600dp, centered (CLAUDE.md 9).
        val columns = Responsive.homeColumns(b.list)
        b.list.layoutManager = StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL)
        Responsive.limitWidth(b.list, Responsive.CONTENT_MAX_DP * columns)
        Responsive.limitWidth(b.header)
        Responsive.limitWidth(b.chipsScroll)
        // On short screens (small phones, large fonts, landscape) wrapped chips would fill the
        // screen if pinned: they scroll away with the header and come back on the first scroll up.
        if (Responsive.shortScreen(b.chipsScroll)) {
            b.chipsScroll.updateLayoutParams<AppBarLayout.LayoutParams> {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            }
        }
        b.list.adapter = adapter
        // Header text must not run under the clock: cover the status bar while the header scrolls.
        b.appBar.addOnOffsetChangedListener { _, offset -> b.statusScrim.alpha = if (offset < 0) 1f else 0f }

        supportFragmentManager.setFragmentResultListener(PinSheet.REQUEST_KEY, this) { _, result ->
            onPinAccepted(
                result.getString(PinSheet.RESULT_PURPOSE).orEmpty(),
                result.getLong(PinSheet.RESULT_PAYLOAD),
            )
        }
        supportFragmentManager.setFragmentResultListener(ConfirmDialogFragment.REQUEST_KEY, this) { _, result ->
            onConfirmAnswered(
                result.getString(ConfirmDialogFragment.RESULT_PURPOSE).orEmpty(),
                result.getLong(ConfirmDialogFragment.RESULT_PAYLOAD),
                result.getBoolean(ConfirmDialogFragment.RESULT_YES),
            )
        }

        Motion.springPress(b.addButton, target = b.addButtonFrame)
        b.addButton.setOnClickListener {
            PinSheet.verify(supportFragmentManager, PURPOSE_ADD, getString(R.string.add_medicine), getString(R.string.pin_msg_add))
        }
        b.settingsButton.setOnClickListener {
            // Settings open freely; each sensitive action inside asks for the PIN (CLAUDE.md 8).
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (savedInstanceState == null) {
            stopSoundFrom(intent)
            pendingDoseFromIntent = doseIdFrom(intent)
        }
        startObserving()

        lifecycleScope.launch {
            val settings = repository.settings()
            if (settings.languageTag == null || settings.pinHash == null) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@launch
            }
            // Show home right away; it updates live while the work below runs (slow phones).
            ready = true
            b.main.isVisible = true
            observedDay.value = repository.todayEpochDay()
            // Every launch: finish past days, create today's doses, re-arm every alarm from the DB.
            repository.refreshToday()
            observedDay.value = repository.todayEpochDay()
            app.alarms.rearmAll()
            pendingDoseFromIntent?.let { openDose(it) }
            pendingDoseFromIntent = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        stopSoundFrom(intent)
        val doseId = doseIdFrom(intent) ?: return
        if (ready) openDose(doseId) else pendingDoseFromIntent = doseId
    }

    override fun onResume() {
        super.onResume()
        renderBanner()
        if (!ready) return
        lifecycleScope.launch {
            repository.refreshToday()
            observedDay.value = repository.todayEpochDay()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        celebration.save(outState)
        courseEnd.save(outState)
        outState.putString(PENDING_COURSE_END, pendingCourseEnd)
        outState.putBoolean(PENDING_COURSE_END_SOUND, pendingCourseEndSound)
    }

    override fun onStart() {
        super.onStart()
        courseEnd.resume()
    }

    override fun onStop() {
        // In the background, after a rotation or when closing: no sound left playing, nothing held.
        celebrationSound.stop()
        courseEnd.pause()
        super.onStop()
    }

    override fun onDestroy() {
        courseEnd.release()
        super.onDestroy()
    }

    /** After the reward: the course-complete screen of a final dose, else a sibling's streak-broken screen. */
    private fun afterReward() {
        val medicine = pendingCourseEnd
        if (medicine != null) {
            pendingCourseEnd = null
            courseEnd.show(medicine, pendingCourseEndSound)
            return
        }
        home?.let { showStreakBreakIfPending(it) }
    }

    // ---------------------------------------------------------------- data

    private fun startObserving() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                observedDay.filterNotNull().collectLatest { day ->
                    repository.observeHome(day).collect { render(it) }
                }
            }
        }
    }

    private fun render(data: HomeData) {
        home = data
        renderChips(data)
        val selected = data.patients.firstOrNull { it.patient.id == data.selectedPatientId }
        val visible = if (selected != null) listOf(selected) else data.patients
        renderHeader(data, selected)

        val blocks = visible.map { p ->
            HomeBlock(
                patient = p,
                label = if (selected == null) labelFor(p, data.patients) else null,
                doses = p.doses.map { HomeBlock.Dose(it, p.patient.name, isOverdue(it)) },
            )
        }
        adapter.submitList(blocks)
        val empty = data.patients.isEmpty()
        if (!empty) Motion.stopIdle(b.emptyMascot) else if (!Motion.isIdling(b.emptyMascot)) Motion.idle(b.emptyMascot)
        b.emptyState.isVisible = empty
        renderBanner()
        showStreakBreakIfPending(data)
    }

    private fun renderHeader(data: HomeData, selected: HomePatient?) {
        b.statsRow.isVisible = selected != null
        b.levelProgress.isVisible = selected != null
        b.levelHint.isVisible = selected != null
        if (selected == null) {
            val total = data.patients.sumOf { it.doses.size }
            val taken = data.patients.sumOf { it.takenToday }
            b.todaySummary.text = when {
                data.patients.isEmpty() -> getString(R.string.today_empty)
                total == 0 -> getString(R.string.status_no_doses)
                else -> getString(R.string.today_all, taken, total)
            }
            b.statusHint.isVisible = false
            return
        }
        b.todaySummary.text = if (selected.doses.isEmpty()) {
            getString(R.string.today_patient_none, Fmt.bidi(this, selected.patient.name))
        } else {
            getString(
                R.string.today_patient,
                Fmt.bidi(this, selected.patient.name),
                selected.takenToday,
                selected.doses.size,
            )
        }
        b.pointsValue.text = Fmt.number(selected.totalPoints)
        b.pointsLabel.text = resources.getQuantityText(R.plurals.points_label, selected.totalPoints)
        b.streakValue.text = Fmt.number(selected.liveStreak)
        b.streakLabel.text = resources.getQuantityText(R.plurals.streak_label, selected.liveStreak)
        b.levelValue.text = Fmt.number(selected.level)
        b.levelProgress.max = 100
        b.levelProgress.setProgressCompat(100 - selected.pointsToNextLevel, true)
        b.levelHint.text = getString(
            R.string.level_hint,
            Fmt.points(this, selected.pointsToNextLevel),
            selected.level + 1,
        )
        b.statusHint.isVisible = true
        b.statusHint.text = HomeAdapter.statusText(this, selected)
    }

    /** "All" plus one chip per patient. Same names stay apart through the age and a number. */
    private fun renderChips(data: HomeData) {
        val keys = listOf("all") + data.patients.map { "${it.patient.id}|${chipLabel(it, data.patients)}" }
        if (keys != chipKeys) {
            chipKeys = keys
            b.filterChips.setOnCheckedStateChangeListener(null)
            b.filterChips.removeAllViews()
            b.filterChips.addView(chip(getString(R.string.filter_all), ALL_CHIP_TAG))
            data.patients.forEach { b.filterChips.addView(chip(chipLabel(it, data.patients), it.patient.id)) }
        }
        val wanted = data.selectedPatientId ?: ALL_CHIP_TAG
        for (i in 0 until b.filterChips.childCount) {
            val c = b.filterChips.getChildAt(i) as Chip
            if (c.tag == wanted && !c.isChecked) c.isChecked = true
        }
        // Chips wrap onto more lines, so every chip is always in view (no sideways scrolling).
        b.filterChips.setOnCheckedStateChangeListener { group, ids ->
            val id = ids.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val tag = group.findViewById<Chip>(id)?.tag as? Long ?: return@setOnCheckedStateChangeListener
            lifecycleScope.launch { repository.setSelectedPatient(tag.takeIf { it != ALL_CHIP_TAG }) }
        }
    }

    private fun chip(text: String, tag: Long) = Chip(this).apply {
        id = ViewGroup.generateViewId()
        this.text = text
        this.tag = tag
        isCheckable = true
        isCheckedIconVisible = false
        textDirection = android.view.View.TEXT_DIRECTION_FIRST_STRONG
    }

    /** Chip text: the avatar (age group and gender) before the name and age. */
    private fun chipLabel(p: HomePatient, all: List<HomePatient>): String =
        getString(R.string.avatar_label, Mascots.avatar(p.ageGroup, Gender.fromStored(p.patient.gender)), labelFor(p, all))

    private fun labelFor(p: HomePatient, all: List<HomePatient>): String {
        val twins = all.filter { it.patient.name == p.patient.name && it.age == p.age }
        val name = Fmt.bidi(this, p.patient.name)
        return if (twins.size <= 1) {
            getString(R.string.patient_label, name, p.age)
        } else {
            getString(R.string.patient_label_numbered, name, p.age, twins.indexOfFirst { it.patient.id == p.patient.id } + 1)
        }
    }

    private fun isOverdue(row: DoseRow): Boolean {
        if (row.status != DoseStatus.PENDING.name) return false
        val now = repository.nowZoned()
        return now.toLocalTime().toSecondOfDay() > row.scheduledMinuteOfDay * 60
    }

    // ---------------------------------------------------------------- actions

    private fun onConfirmClicked(row: DoseRow, patientName: String) {
        val requirePin = home?.settings?.requirePinForDose ?: true
        val patient = Fmt.bidi(this, patientName)
        val medicine = Fmt.bidi(this, row.medicineName)
        if (requirePin) {
            PinSheet.verify(
                supportFragmentManager,
                PURPOSE_CONFIRM,
                title = getString(R.string.pin_title_confirm_dose, patient),
                message = getString(R.string.pin_msg_confirm, patient, medicine),
                payload = row.doseId,
            )
        } else {
            ConfirmDialogFragment.show(
                supportFragmentManager,
                PURPOSE_CONFIRM,
                title = getString(R.string.confirm_dose_question, patient, medicine),
                message = null,
                positive = getString(R.string.confirm_yes),
                negative = getString(R.string.confirm_not_yet),
                payload = row.doseId,
            )
        }
    }

    private fun onEditClicked(row: DoseRow) {
        PinSheet.verify(
            supportFragmentManager,
            PURPOSE_EDIT,
            title = getString(R.string.edit_medicine),
            message = getString(R.string.pin_msg_edit),
            payload = row.medicineId,
        )
    }

    private fun onPinAccepted(purpose: String, payload: Long) {
        when (purpose) {
            PURPOSE_ADD -> editor.launch(MedicineEditActivity.addIntent(this))
            PURPOSE_EDIT -> editor.launch(MedicineEditActivity.editIntent(this, payload))
            PURPOSE_CONFIRM -> confirmDose(payload)
        }
    }

    private fun onConfirmAnswered(purpose: String, payload: Long, yes: Boolean) {
        when (purpose) {
            PURPOSE_CONFIRM -> if (yes) confirmDose(payload)
            PURPOSE_NOTIFICATIONS -> {
                lifecycleScope.launch { repository.markNotificationRationaleShown() }
                if (yes && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun confirmDose(doseId: Long) {
        lifecycleScope.launch {
            when (val outcome = repository.confirmDose(doseId)) {
                is ConfirmOutcome.Confirmed -> {
                    // The dose's alarm sound is asked to stop first; the reward's sound waits for it.
                    app.alarms.onDoseTaken(doseId)
                    val soundOn = repository.settings().celebrationSoundEnabled
                    // The final dose of a dated course: its course-complete screen follows the reward (R6).
                    if (repository.isFinalDoseOfCourse(doseId)) {
                        pendingCourseEnd = outcome.medicineName
                        pendingCourseEndSound = soundOn
                    }
                    celebration.showReward(outcome, soundOn, Notifications.notificationId(doseId))
                }
                is ConfirmOutcome.Rejected -> snack(R.string.dose_not_confirmable)
            }
        }
    }

    /** From a notification: open the confirm step of exactly that dose. */
    private fun openDose(doseId: Long) {
        lifecycleScope.launch {
            val row = repository.doseRow(doseId)
            val patient = row?.let { repository.patient(it.patientId) }
            when {
                row == null || patient == null -> snack(R.string.dose_not_found)
                row.status != DoseStatus.PENDING.name || row.scheduledEpochDay != repository.todayEpochDay() ->
                    snack(R.string.dose_not_confirmable)
                else -> onConfirmClicked(row, patient.name)
            }
        }
    }

    private fun showStreakBreakIfPending(data: HomeData) {
        if (celebration.isShowing || courseEnd.isShowing) return
        val p = data.patients.firstOrNull { it.brokenStreakPending != null } ?: return
        val lost = p.brokenStreakPending ?: return
        celebration.showStreakBroken(p.patient.name, p.ageGroup, Gender.fromStored(p.patient.gender), lost, p.bestStreak)
        // Acknowledge right away so it is shown exactly once, even if the app is closed now.
        lifecycleScope.launch { repository.acknowledgeStreakBreak(p.patient.id) }
    }

    // ---------------------------------------------------------------- permissions

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return
        lifecycleScope.launch {
            if (repository.settings().notificationRationaleShown) return@launch
            ConfirmDialogFragment.show(
                supportFragmentManager,
                PURPOSE_NOTIFICATIONS,
                title = getString(R.string.notif_rationale_title),
                message = getString(R.string.notif_rationale_message),
                positive = getString(R.string.notif_rationale_allow),
                negative = getString(R.string.not_now),
            )
        }
    }

    private fun renderBanner() {
        val hasPatients = home?.patients?.isNotEmpty() == true
        val notificationsOk = Notifications.canPost(this)
        val exactOk = app.alarms.canScheduleExact()
        when {
            !hasPatients -> b.banner.isVisible = false
            !notificationsOk -> {
                b.banner.isVisible = true
                b.bannerText.setText(R.string.banner_notifications_off)
                b.bannerAction.setText(R.string.banner_fix)
                b.bannerAction.setOnClickListener { openNotificationSettings(this) }
            }
            !exactOk -> {
                b.banner.isVisible = true
                b.bannerText.setText(R.string.banner_exact_off)
                b.bannerAction.setText(R.string.banner_fix)
                b.bannerAction.setOnClickListener { openExactAlarmSettings(this) }
            }
            else -> b.banner.isVisible = false
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun applyInsets() {
        val listBottom = b.list.paddingBottom
        val fabBottom = (b.addButtonFrame.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(b.main) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            b.main.updatePadding(left = bars.left, right = bars.right)
            // On the app bar, not the header: the pinned chips must stay below the status bar
            // when the header scrolls away (landscape, large fonts).
            b.appBar.updatePadding(top = bars.top)
            b.statusScrim.updateLayoutParams { height = bars.top }
            b.list.updatePadding(bottom = listBottom + bars.bottom)
            b.addButtonFrame.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = fabBottom + bars.bottom }
            insets
        }
    }

    private fun snack(text: Int) {
        Snackbar.make(b.main, text, Snackbar.LENGTH_LONG).setAnchorView(b.addButtonFrame).show()
    }

    private fun doseIdFrom(intent: Intent?): Long? =
        intent?.getLongExtra(EXTRA_DOSE_ID, -1L)?.takeIf { it > 0 }.also { intent?.removeExtra(EXTRA_DOSE_ID) }

    /** A tap on a ringing reminder silences the app's alarm sound (CLAUDE.md 7). */
    private fun stopSoundFrom(intent: Intent?) {
        val id = intent?.getIntExtra(EXTRA_SOUND_ID, AlarmSoundService.NO_ID) ?: return
        intent.removeExtra(EXTRA_SOUND_ID)
        if (id != AlarmSoundService.NO_ID) AlarmSound.stop(this, SoundPolicy.StopReason.TAPPED, id)
    }

    companion object {
        const val EXTRA_DOSE_ID = "dose_id"
        /** The notification id of the reminder that was tapped. */
        const val EXTRA_SOUND_ID = "sound_id"
        private const val ALL_CHIP_TAG = -1L
        private const val PURPOSE_ADD = "add"
        private const val PURPOSE_EDIT = "edit"
        private const val PURPOSE_CONFIRM = "confirm"
        private const val PURPOSE_NOTIFICATIONS = "notifications"
        private const val PENDING_COURSE_END = "pending_course_end"
        private const val PENDING_COURSE_END_SOUND = "pending_course_end_sound"

        /** Opens the confirm step for one dose (used by reminder notifications). */
        fun confirmIntent(context: Context, doseId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_DOSE_ID, doseId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        fun openNotificationSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }

        fun openExactAlarmSettings(context: Context) {
            val appUri = "package:${context.packageName}".toUri()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, appUri))
                    return
                }
            } catch (_: android.content.ActivityNotFoundException) {
                // Some phones hide this screen; the app details page is the next best place.
            }
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, appUri))
        }
    }
}
