package com.example.tracker.domain

import java.util.Locale

enum class ValidationError {
    PATIENT_MISSING,
    PATIENT_NAME_EMPTY,
    PATIENT_NAME_TOO_LONG,
    AGE_INVALID,
    GENDER_MISSING,
    MEDICINE_NAME_EMPTY,
    MEDICINE_NAME_TOO_LONG,
    NO_SLOTS,
    DAY_WITHOUT_TIMES,
    DUPLICATE_SLOT,
    SLOT_OUT_OF_RANGE,
    END_BEFORE_START,
    INTERVAL_INVALID,
}

/** Who a medicine is for, as chosen in the form. */
sealed interface PatientChoice {
    data object None : PatientChoice
    data class Existing(val patientId: Long) : PatientChoice
    /** A new patient needs a gender; existing patients may have none (not set). */
    data class New(val name: String?, val ageText: String?, val gender: Gender?) : PatientChoice
}

data class MedicineDraft(
    val patient: PatientChoice,
    val name: String?,
    /** Every (day, time) the parent picked, duplicates included so they can be reported. */
    val slots: List<Slot>,
    /** Days the parent selected that ended up with no time at all (per-day mode). */
    val daysWithoutTimes: Set<Int> = emptySet(),
    val startEpochDay: Long?,
    val endEpochDay: Long?,
    val reminderIntervalMinutes: Int,
)

object Validation {
    const val MAX_NAME_LENGTH = 40
    val REMINDER_INTERVALS = listOf(5, 10, 15, 30)
    const val DEFAULT_REMINDER_INTERVAL = 10

    private val whitespace = Regex("\\s+")

    /** Trimmed name with inner whitespace collapsed, or null when nothing is left. */
    fun normalizeName(raw: String?): String? =
        raw?.trim()?.replace(whitespace, " ")?.takeIf { it.isNotEmpty() }

    /** Key used by the anti-farming ledger, so "Panadol " and "panadol" count as one medicine. */
    fun medicineKey(name: String): String = (normalizeName(name) ?: "").lowercase(Locale.ROOT)

    /** Converts Arabic-Indic and Extended Arabic-Indic digits to 0-9. */
    fun westernDigits(text: String): String = buildString(text.length) {
        for (c in text) {
            append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    else -> c
                }
            )
        }
    }

    /** Age in 0..120, or null when the text is not a whole number in range. */
    fun parseAge(text: String?): Int? {
        val digits = westernDigits(text?.trim() ?: return null)
        if (digits.isEmpty() || digits.length > 3 || !digits.all { it in '0'..'9' }) return null
        return digits.toInt().takeIf { it in Ages.MIN_AGE..Ages.MAX_AGE }
    }

    fun validatePatient(name: String?, ageText: String?): List<ValidationError> = buildList {
        val n = normalizeName(name)
        when {
            n == null -> add(ValidationError.PATIENT_NAME_EMPTY)
            n.codePointCount(0, n.length) > MAX_NAME_LENGTH -> add(ValidationError.PATIENT_NAME_TOO_LONG)
        }
        if (parseAge(ageText) == null) add(ValidationError.AGE_INVALID)
    }

    fun validateMedicine(draft: MedicineDraft): List<ValidationError> = buildList {
        when (val p = draft.patient) {
            PatientChoice.None -> add(ValidationError.PATIENT_MISSING)
            is PatientChoice.Existing -> Unit
            is PatientChoice.New -> {
                addAll(validatePatient(p.name, p.ageText))
                if (p.gender == null) add(ValidationError.GENDER_MISSING)
            }
        }
        val n = normalizeName(draft.name)
        when {
            n == null -> add(ValidationError.MEDICINE_NAME_EMPTY)
            n.codePointCount(0, n.length) > MAX_NAME_LENGTH -> add(ValidationError.MEDICINE_NAME_TOO_LONG)
        }
        if (draft.daysWithoutTimes.isNotEmpty()) add(ValidationError.DAY_WITHOUT_TIMES)
        if (draft.slots.isEmpty()) add(ValidationError.NO_SLOTS)
        if (draft.slots.size != draft.slots.toSet().size) add(ValidationError.DUPLICATE_SLOT)
        if (draft.slots.any { it.dayOfWeek !in 1..7 || it.minuteOfDay !in 0 until Slot.MINUTES_PER_DAY }) {
            add(ValidationError.SLOT_OUT_OF_RANGE)
        }
        val start = draft.startEpochDay
        val end = draft.endEpochDay
        if (start != null && end != null && end < start) add(ValidationError.END_BEFORE_START)
        if (draft.reminderIntervalMinutes !in REMINDER_INTERVALS) add(ValidationError.INTERVAL_INVALID)
    }
}
