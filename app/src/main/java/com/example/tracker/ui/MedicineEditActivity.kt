package com.example.tracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.tracker.R
import com.example.tracker.app
import com.example.tracker.data.MedicineInput
import com.example.tracker.data.MedicineWithSlots
import com.example.tracker.data.SaveResult
import com.example.tracker.data.db.PatientEntity
import com.example.tracker.databinding.ActivityMedicineEditBinding
import com.example.tracker.databinding.ItemDayTimesBinding
import com.example.tracker.domain.PatientChoice
import com.example.tracker.domain.ScheduleRules
import com.example.tracker.domain.Slot
import com.example.tracker.domain.Validation
import com.example.tracker.domain.ValidationError
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.util.TreeSet

/** Add or edit one medicine: who it is for, name, weekly schedule, dates, follow-up interval. */
class MedicineEditActivity : AppCompatActivity() {

    private lateinit var b: ActivityMedicineEditBinding
    private val repository get() = app.repository
    private val medicineId: Long? by lazy { intent.getLongExtra(EXTRA_MEDICINE_ID, -1L).takeIf { it > 0 } }
    private val editing get() = medicineId != null

    private var patients: List<PatientEntity> = emptyList()
    private var fixedPatient: PatientEntity? = null

    // Form state. Kept in saved instance state, never in the DB until saved.
    private var patientSelection: Long? = null
    private var everyDay = true
    private var perDay = false
    private val days = sortedSetOf<Int>()
    private val commonTimes = sortedSetOf<Int>()
    private val perDayTimes = mutableMapOf<Int, TreeSet<Int>>()
    private var startDay: Long? = null
    private var endDay: Long? = null
    private var interval = Validation.DEFAULT_REMINDER_INTERVAL
    private var timeTarget = TARGET_COMMON
    private var dateTarget = DATE_START

    private var restored = false
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        b = ActivityMedicineEditBinding.inflate(layoutInflater)
        setContentView(b.root)
        Responsive.limitWidth(b.content)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        b.toolbar.setTitle(if (editing) R.string.edit_title else R.string.add_medicine)
        b.toolbar.setNavigationOnClickListener { finish() }
        b.deleteButton.isVisible = editing

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
            restored = true
        }
        reattachPickers()
        buildDayChips()
        buildIntervalChips()
        wireListeners()

        supportFragmentManager.setFragmentResultListener(ConfirmDialogFragment.REQUEST_KEY, this) { _, r ->
            if (r.getString(ConfirmDialogFragment.RESULT_PURPOSE) == PURPOSE_DELETE &&
                r.getBoolean(ConfirmDialogFragment.RESULT_YES)
            ) {
                deleteMedicine()
            }
        }

        lifecycleScope.launch {
            patients = repository.patients()
            val id = medicineId
            if (id != null) {
                val existing = repository.medicineWithSlots(id)
                if (existing == null) {
                    finish()
                    return@launch
                }
                fixedPatient = existing.patient
                if (!restored) loadFrom(existing)
            } else if (!restored && patients.size == 1) {
                // Preselect only when there is exactly one patient. Never guess between several.
                patientSelection = patients.first().id
            }
            renderAll()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(S_PATIENT, patientSelection ?: NONE)
        outState.putBoolean(S_EVERY_DAY, everyDay)
        outState.putBoolean(S_PER_DAY, perDay)
        outState.putIntArray(S_DAYS, days.toIntArray())
        outState.putIntArray(S_COMMON, commonTimes.toIntArray())
        for (d in 1..7) perDayTimes[d]?.let { outState.putIntArray("$S_PER_DAY_PREFIX$d", it.toIntArray()) }
        outState.putLong(S_START, startDay ?: NONE)
        outState.putLong(S_END, endDay ?: NONE)
        outState.putInt(S_INTERVAL, interval)
        outState.putInt(S_TIME_TARGET, timeTarget)
        outState.putInt(S_DATE_TARGET, dateTarget)
    }

    private fun restoreState(s: Bundle) {
        patientSelection = s.getLong(S_PATIENT, NONE).takeIf { it != NONE }
        everyDay = s.getBoolean(S_EVERY_DAY, true)
        perDay = s.getBoolean(S_PER_DAY, false)
        days.clear(); s.getIntArray(S_DAYS)?.let { days.addAll(it.toList()) }
        commonTimes.clear(); s.getIntArray(S_COMMON)?.let { commonTimes.addAll(it.toList()) }
        perDayTimes.clear()
        for (d in 1..7) s.getIntArray("$S_PER_DAY_PREFIX$d")?.let { perDayTimes[d] = TreeSet(it.toList()) }
        startDay = s.getLong(S_START, NONE).takeIf { it != NONE }
        endDay = s.getLong(S_END, NONE).takeIf { it != NONE }
        interval = s.getInt(S_INTERVAL, Validation.DEFAULT_REMINDER_INTERVAL)
        timeTarget = s.getInt(S_TIME_TARGET, TARGET_COMMON)
        dateTarget = s.getInt(S_DATE_TARGET, DATE_START)
    }

    /** Turns stored slots back into the simplest matching form layout. */
    private fun loadFrom(m: MedicineWithSlots) {
        b.nameInput.setText(m.medicine.name)
        val byDay = m.slots.groupBy { it.dayOfWeek }.mapValues { (_, s) -> TreeSet(s.map { it.minuteOfDay }) }
        val distinctSets = byDay.values.map { it.toList() }.toSet()
        everyDay = byDay.keys == ScheduleRules.ALL_DAYS.toSet()
        days.clear(); days.addAll(byDay.keys)
        if (distinctSets.size <= 1) {
            perDay = false
            commonTimes.clear(); byDay.values.firstOrNull()?.let { commonTimes.addAll(it) }
        } else {
            perDay = true
            perDayTimes.clear(); perDayTimes.putAll(byDay)
        }
        startDay = m.medicine.startEpochDay
        endDay = m.medicine.endEpochDay
        interval = m.medicine.reminderIntervalMinutes
    }

    // ---------------------------------------------------------------- building

    private fun buildDayChips() {
        b.dayChips.removeAllViews()
        for (day in Fmt.weekOrder(this)) {
            b.dayChips.addView(Chip(this).apply {
                id = View.generateViewId()
                tag = day
                text = Fmt.dayShort(this@MedicineEditActivity, day)
                contentDescription = Fmt.dayFull(this@MedicineEditActivity, day)
                isCheckable = true
                isChecked = day in days
                setOnCheckedChangeListener { _, checked ->
                    if (checked) days.add(day) else days.remove(day)
                    if (perDay) renderTimes()
                }
            })
        }
    }

    private fun buildIntervalChips() {
        b.intervalChips.removeAllViews()
        for (minutes in Validation.REMINDER_INTERVALS) {
            b.intervalChips.addView(Chip(this).apply {
                id = View.generateViewId()
                text = resources.getQuantityString(R.plurals.minutes_short, minutes, minutes)
                isCheckable = true
                isChecked = minutes == interval
                setOnCheckedChangeListener { _, checked -> if (checked) interval = minutes }
            })
        }
    }

    private fun wireListeners() {
        b.modeGroup.addOnButtonCheckedListener { _, buttonId, checked ->
            if (!checked) return@addOnButtonCheckedListener
            everyDay = buttonId == R.id.everyDayButton
            renderSchedule()
        }
        b.perDaySwitch.setOnCheckedChangeListener { _, checked ->
            if (checked == perDay) return@setOnCheckedChangeListener
            perDay = checked
            if (perDay) {
                // Start every day with the common times, so nothing typed is lost.
                activeDays().forEach { d -> perDayTimes.getOrPut(d) { TreeSet() }.addAll(commonTimes) }
            }
            renderSchedule()
        }
        b.addTimeButton.setOnClickListener { openTimePicker(TARGET_COMMON) }
        b.startButton.setOnClickListener { openDatePicker(DATE_START) }
        b.endButton.setOnClickListener { openDatePicker(DATE_END) }
        b.clearStart.setOnClickListener { startDay = null; renderDates() }
        b.clearEnd.setOnClickListener { endDay = null; renderDates() }
        b.patientDropdown.setOnItemClickListener { _, _, position, _ ->
            patientSelection = if (position < patients.size) patients[position].id else NEW_PATIENT
            b.patientError.isVisible = false
            renderPatient()
        }
        Motion.springPress(b.saveButton)
        b.saveButton.setOnClickListener { save() }
        b.deleteButton.setOnClickListener {
            ConfirmDialogFragment.show(
                supportFragmentManager,
                PURPOSE_DELETE,
                title = getString(R.string.delete_medicine_title, Fmt.bidi(this, b.nameInput.text.toString())),
                message = getString(R.string.delete_medicine_message),
                positive = getString(R.string.delete),
                negative = getString(R.string.cancel),
            )
        }
    }

    // ---------------------------------------------------------------- rendering

    private fun renderAll() {
        renderPatient()
        renderSchedule()
        renderDates()
    }

    private fun renderPatient() {
        val fixed = fixedPatient
        if (fixed != null) {
            b.patientFixed.isVisible = true
            b.patientFixed.text = getString(R.string.patient_fixed, patientLabel(fixed))
            b.patientLayout.isVisible = false
            b.newPatientGroup.isVisible = false
            return
        }
        b.patientFixed.isVisible = false
        b.patientLayout.isVisible = patients.isNotEmpty()
        val labels = patients.map { patientLabel(it) } + getString(R.string.new_patient_option)
        b.patientDropdown.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
        val index = when (val sel = patientSelection) {
            null -> -1
            NEW_PATIENT -> labels.lastIndex
            else -> patients.indexOfFirst { it.id == sel }
        }
        if (index >= 0) b.patientDropdown.setText(labels[index], false) else b.patientDropdown.setText("", false)
        b.newPatientGroup.isVisible = patients.isEmpty() || patientSelection == NEW_PATIENT
    }

    private fun patientLabel(p: PatientEntity): String {
        val age = repository.currentAge(p)
        val twins = patients.filter { it.name == p.name && repository.currentAge(it) == age }
        val name = Fmt.bidi(this, p.name)
        return if (twins.size <= 1) getString(R.string.patient_label, name, age)
        else getString(R.string.patient_label_numbered, name, age, twins.indexOfFirst { it.id == p.id } + 1)
    }

    private fun renderSchedule() {
        b.modeGroup.check(if (everyDay) R.id.everyDayButton else R.id.specificDaysButton)
        b.dayChips.isVisible = !everyDay
        for (i in 0 until b.dayChips.childCount) {
            val chip = b.dayChips.getChildAt(i) as Chip
            chip.isChecked = chip.tag as Int in days
        }
        b.perDaySwitch.isChecked = perDay
        renderTimes()
    }

    private fun renderTimes() {
        b.commonTimesGroup.isVisible = !perDay
        b.perDayContainer.isVisible = perDay
        if (!perDay) {
            fillTimeChips(b.commonTimes, commonTimes)
            return
        }
        b.perDayContainer.removeAllViews()
        for (day in Fmt.weekOrder(this).filter { it in activeDays() }) {
            val row = ItemDayTimesBinding.inflate(layoutInflater, b.perDayContainer, false)
            row.dayLabel.text = Fmt.dayFull(this, day)
            row.addButton.contentDescription = getString(R.string.add_time_for, Fmt.dayFull(this, day))
            row.addButton.setOnClickListener { openTimePicker(day) }
            fillTimeChips(row.times, perDayTimes.getOrPut(day) { TreeSet() })
            b.perDayContainer.addView(row.root)
        }
    }

    private fun fillTimeChips(group: ChipGroup, times: MutableSet<Int>) {
        group.removeAllViews()
        for (minute in times.sorted()) {
            val label = Fmt.time(this, minute)
            group.addView(Chip(this).apply {
                text = label
                isCloseIconVisible = true
                closeIconContentDescription = getString(R.string.remove_time, label)
                setOnCloseIconClickListener {
                    times.remove(minute)
                    renderTimes()
                }
            })
        }
    }

    private fun renderDates() {
        b.startButton.text = startDay?.let { getString(R.string.start_date_value, Fmt.date(this, it)) }
            ?: getString(R.string.no_start_date)
        b.endButton.text = endDay?.let { getString(R.string.end_date_value, Fmt.date(this, it)) }
            ?: getString(R.string.no_end_date)
        b.clearStart.isVisible = startDay != null
        b.clearEnd.isVisible = endDay != null
        b.datesError.isVisible = false
    }

    private fun activeDays(): List<Int> = if (everyDay) ScheduleRules.ALL_DAYS else days.toList()

    // ---------------------------------------------------------------- pickers

    private fun openTimePicker(target: Int) {
        if (supportFragmentManager.findFragmentByTag(TIME_TAG) != null) return
        timeTarget = target
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(8)
            .setMinute(0)
            .setTitleText(R.string.pick_time_title)
            .build()
        attachTimeListener(picker)
        picker.show(supportFragmentManager, TIME_TAG)
    }

    private fun attachTimeListener(picker: MaterialTimePicker) {
        picker.addOnPositiveButtonClickListener { addTime(timeTarget, picker.hour * 60 + picker.minute) }
    }

    private fun addTime(target: Int, minute: Int) {
        val set = if (target == TARGET_COMMON) commonTimes else perDayTimes.getOrPut(target) { TreeSet() }
        if (!set.add(minute)) {
            showError(b.scheduleError, getString(R.string.err_duplicate_slot))
            return
        }
        b.scheduleError.isVisible = false
        renderTimes()
    }

    private fun openDatePicker(target: Int) {
        if (supportFragmentManager.findFragmentByTag(DATE_TAG) != null) return
        dateTarget = target
        val current = (if (target == DATE_START) startDay else endDay) ?: repository.todayEpochDay()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (target == DATE_START) R.string.start_date else R.string.end_date)
            .setSelection(current * DAY_MILLIS)
            .build()
        attachDateListener(picker)
        picker.show(supportFragmentManager, DATE_TAG)
    }

    private fun attachDateListener(picker: MaterialDatePicker<Long>) {
        picker.addOnPositiveButtonClickListener { selection ->
            val day = Math.floorDiv(selection, DAY_MILLIS)
            if (dateTarget == DATE_START) startDay = day else endDay = day
            renderDates()
        }
    }

    /** Picker listeners are not kept across rotation, so hook them up again. */
    @Suppress("UNCHECKED_CAST")
    private fun reattachPickers() {
        (supportFragmentManager.findFragmentByTag(TIME_TAG) as? MaterialTimePicker)?.let { attachTimeListener(it) }
        (supportFragmentManager.findFragmentByTag(DATE_TAG) as? MaterialDatePicker<Long>)?.let { attachDateListener(it) }
    }

    // ---------------------------------------------------------------- saving

    private fun collectSlots(): Pair<List<Slot>, Set<Int>> {
        val active = activeDays()
        if (!perDay) return ScheduleRules.expand(active, commonTimes).toList() to emptySet()
        val slots = active.flatMap { d -> perDayTimes[d].orEmpty().map { Slot(d, it) } }
        val empty = active.filter { perDayTimes[it].isNullOrEmpty() }.toSet()
        return slots to empty
    }

    private fun save() {
        if (saving) return
        clearErrors()
        val (slots, emptyDays) = collectSlots()
        val fixed = fixedPatient
        val choice = when {
            fixed != null -> PatientChoice.Existing(fixed.id)
            patients.isEmpty() || patientSelection == NEW_PATIENT ->
                PatientChoice.New(b.newNameInput.text?.toString(), b.newAgeInput.text?.toString(), genderOf(b.newGender))
            patientSelection != null -> PatientChoice.Existing(checkNotNull(patientSelection))
            else -> PatientChoice.None
        }
        val input = MedicineInput(
            medicineId = medicineId,
            patient = choice,
            name = b.nameInput.text?.toString(),
            slots = slots,
            daysWithoutTimes = emptyDays,
            startEpochDay = startDay,
            endEpochDay = endDay,
            reminderIntervalMinutes = interval,
        )
        saving = true
        lifecycleScope.launch {
            when (val result = repository.saveMedicine(input)) {
                is SaveResult.Saved -> {
                    app.alarms.cancel(result.removed)
                    app.alarms.rearmAll()
                    setResult(RESULT_OK, Intent().putExtra(RESULT_ADDED, !editing))
                    finish()
                }
                is SaveResult.Invalid -> {
                    saving = false
                    showErrors(result.errors)
                }
            }
        }
    }

    private fun deleteMedicine() {
        val id = medicineId ?: return
        lifecycleScope.launch {
            app.alarms.cancel(repository.deleteMedicine(id))
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun clearErrors() {
        b.patientError.isVisible = false
        b.newNameLayout.error = null
        b.newAgeLayout.error = null
        b.newGender.genderError.isVisible = false
        b.nameLayout.error = null
        b.scheduleError.isVisible = false
        b.datesError.isVisible = false
    }

    private fun showErrors(errors: List<ValidationError>) {
        var first: View? = null
        fun mark(view: View) {
            if (first == null) first = view
        }
        for (e in errors) when (e) {
            ValidationError.PATIENT_MISSING -> {
                showError(b.patientError, getString(R.string.err_patient_missing)); mark(b.patientLayout)
            }
            ValidationError.PATIENT_NAME_EMPTY -> {
                b.newNameLayout.error = getString(R.string.err_patient_name); mark(b.newNameLayout)
            }
            ValidationError.PATIENT_NAME_TOO_LONG -> {
                b.newNameLayout.error = nameTooLong(); mark(b.newNameLayout)
            }
            ValidationError.AGE_INVALID -> {
                b.newAgeLayout.error = getString(R.string.err_age); mark(b.newAgeLayout)
            }
            ValidationError.GENDER_MISSING -> {
                b.newGender.genderError.isVisible = true; mark(b.newGender.root)
            }
            ValidationError.MEDICINE_NAME_EMPTY -> {
                b.nameLayout.error = getString(R.string.err_medicine_name); mark(b.nameLayout)
            }
            ValidationError.MEDICINE_NAME_TOO_LONG -> {
                b.nameLayout.error = nameTooLong(); mark(b.nameLayout)
            }
            ValidationError.NO_SLOTS, ValidationError.SLOT_OUT_OF_RANGE -> {
                showError(b.scheduleError, getString(R.string.err_no_slots)); mark(b.modeGroup)
            }
            ValidationError.DAY_WITHOUT_TIMES -> {
                showError(b.scheduleError, getString(R.string.err_day_without_times)); mark(b.modeGroup)
            }
            ValidationError.DUPLICATE_SLOT -> {
                showError(b.scheduleError, getString(R.string.err_duplicate_slot)); mark(b.modeGroup)
            }
            ValidationError.END_BEFORE_START -> {
                showError(b.datesError, getString(R.string.err_end_before_start)); mark(b.endButton)
            }
            ValidationError.INTERVAL_INVALID -> mark(b.intervalChips)
        }
        first?.let { v -> b.scroll.post { b.scroll.smoothScrollTo(0, maxOf(0, v.top - 48)) } }
    }

    private fun showError(view: android.widget.TextView, text: String) {
        view.text = text
        view.isVisible = true
    }

    private fun nameTooLong(): String = resources.getQuantityString(
        R.plurals.err_name_too_long, Validation.MAX_NAME_LENGTH, Validation.MAX_NAME_LENGTH,
    )

    companion object {
        const val RESULT_ADDED = "added"
        private const val EXTRA_MEDICINE_ID = "medicine_id"
        private const val PURPOSE_DELETE = "delete_medicine"
        private const val NEW_PATIENT = -2L
        private const val NONE = Long.MIN_VALUE
        private const val TARGET_COMMON = 0
        private const val DATE_START = 1
        private const val DATE_END = 2
        private const val TIME_TAG = "time_picker"
        private const val DATE_TAG = "date_picker"
        private const val DAY_MILLIS = 86_400_000L
        private const val S_PATIENT = "patient"
        private const val S_EVERY_DAY = "every_day"
        private const val S_PER_DAY = "per_day"
        private const val S_DAYS = "days"
        private const val S_COMMON = "common"
        private const val S_PER_DAY_PREFIX = "per_day_"
        private const val S_START = "start"
        private const val S_END = "end"
        private const val S_INTERVAL = "interval"
        private const val S_TIME_TARGET = "time_target"
        private const val S_DATE_TARGET = "date_target"

        fun addIntent(context: Context) = Intent(context, MedicineEditActivity::class.java)

        fun editIntent(context: Context, medicineId: Long) =
            Intent(context, MedicineEditActivity::class.java).putExtra(EXTRA_MEDICINE_ID, medicineId)
    }
}
