package com.example.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidationTest {

    private val validDraft = MedicineDraft(
        patient = PatientChoice.Existing(1),
        name = "Panadol",
        slots = listOf(Slot(1, 480)),
        startEpochDay = null,
        endEpochDay = null,
        reminderIntervalMinutes = 10,
    )

    @Test
    fun normalizeNameTrimsAndCollapsesSpaces() {
        assertEquals("Yasmin Ali", Validation.normalizeName("  Yasmin   Ali "))
        assertNull(Validation.normalizeName("    "))
        assertNull(Validation.normalizeName(""))
        assertNull(Validation.normalizeName(null))
    }

    @Test
    fun arabicAndMixedNamesAreAccepted() {
        assertEquals(emptyList<ValidationError>(), Validation.validatePatient("ياسمين", "7"))
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(name = "Augmentin 1g")))
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(name = "بنادول Panadol")))
    }

    @Test
    fun nameLengthLimitIsFortyCharacters() {
        val forty = "ب".repeat(40)
        assertEquals(emptyList<ValidationError>(), Validation.validatePatient(forty, "7"))
        assertEquals(listOf(ValidationError.PATIENT_NAME_TOO_LONG), Validation.validatePatient(forty + "ب", "7"))
        assertEquals(listOf(ValidationError.MEDICINE_NAME_TOO_LONG), Validation.validateMedicine(validDraft.copy(name = "x".repeat(41))))
    }

    @Test
    fun ageParsingAcceptsZeroTo120Only() {
        assertEquals(0, Validation.parseAge("0"))
        assertEquals(120, Validation.parseAge("120"))
        assertEquals(12, Validation.parseAge(" 12 "))
        assertNull(Validation.parseAge("121"))
        assertNull(Validation.parseAge("-1"))
        assertNull(Validation.parseAge("7.5"))
        assertNull(Validation.parseAge("abc"))
        assertNull(Validation.parseAge(""))
        assertNull(Validation.parseAge(null))
        assertNull(Validation.parseAge("0120"))
    }

    @Test
    fun arabicIndicDigitsAreReadAsWesternDigits() {
        assertEquals(7, Validation.parseAge("٧"))
        assertEquals(12, Validation.parseAge("۱۲"))
        assertEquals("2026", Validation.westernDigits("٢٠٢٦"))
    }

    @Test
    fun patientValidationReportsEachProblem() {
        assertEquals(
            listOf(ValidationError.PATIENT_NAME_EMPTY, ValidationError.AGE_INVALID),
            Validation.validatePatient("  ", "130"),
        )
        assertEquals(listOf(ValidationError.AGE_INVALID), Validation.validatePatient("Omar", ""))
    }

    @Test
    fun validDraftHasNoErrors() {
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft))
    }

    @Test
    fun missingPatientIsAnError() {
        assertEquals(listOf(ValidationError.PATIENT_MISSING), Validation.validateMedicine(validDraft.copy(patient = PatientChoice.None)))
    }

    @Test
    fun newPatientFieldsAreValidated() {
        assertEquals(
            listOf(ValidationError.PATIENT_NAME_EMPTY, ValidationError.AGE_INVALID),
            Validation.validateMedicine(validDraft.copy(patient = PatientChoice.New("", "x", Gender.FEMALE))),
        )
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(patient = PatientChoice.New("Yasmin", "7", Gender.FEMALE))))
    }

    @Test
    fun newPatientNeedsAGenderExistingDoesNot() {
        assertEquals(
            listOf(ValidationError.GENDER_MISSING),
            Validation.validateMedicine(validDraft.copy(patient = PatientChoice.New("Yasmin", "7", null))),
        )
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(patient = PatientChoice.New("Omar", "7", Gender.MALE))))
        // An existing patient may have no gender (not set before v3): no error, no prompt.
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(patient = PatientChoice.Existing(1))))
    }

    @Test
    fun genderIsStoredByNameAndUnknownValuesAreNotSet() {
        assertEquals(Gender.MALE, Gender.fromStored("MALE"))
        assertEquals(Gender.FEMALE, Gender.fromStored("FEMALE"))
        assertNull(Gender.fromStored(null))
        assertNull(Gender.fromStored("female"))
    }

    @Test
    fun emptyMedicineNameIsAnError() {
        assertEquals(listOf(ValidationError.MEDICINE_NAME_EMPTY), Validation.validateMedicine(validDraft.copy(name = "   ")))
        assertEquals(listOf(ValidationError.MEDICINE_NAME_EMPTY), Validation.validateMedicine(validDraft.copy(name = null)))
    }

    @Test
    fun noSlotsIsAnError() {
        assertEquals(listOf(ValidationError.NO_SLOTS), Validation.validateMedicine(validDraft.copy(slots = emptyList())))
    }

    @Test
    fun duplicateSlotIsAnError() {
        assertEquals(
            listOf(ValidationError.DUPLICATE_SLOT),
            Validation.validateMedicine(validDraft.copy(slots = listOf(Slot(1, 480), Slot(2, 480), Slot(1, 480)))),
        )
    }

    @Test
    fun dayWithoutTimesIsAnError() {
        assertEquals(
            listOf(ValidationError.DAY_WITHOUT_TIMES),
            Validation.validateMedicine(validDraft.copy(daysWithoutTimes = setOf(3))),
        )
    }

    @Test
    fun slotOutsideTheWeekOrDayIsAnError() {
        for (bad in listOf(Slot(0, 480), Slot(8, 480), Slot(1, 1440), Slot(1, -1))) {
            assertEquals(listOf(ValidationError.SLOT_OUT_OF_RANGE), Validation.validateMedicine(validDraft.copy(slots = listOf(bad))))
        }
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(slots = listOf(Slot(7, 1439)))))
    }

    @Test
    fun endBeforeStartIsAnErrorButSameDayIsFine() {
        assertEquals(
            listOf(ValidationError.END_BEFORE_START),
            Validation.validateMedicine(validDraft.copy(startEpochDay = 100, endEpochDay = 99)),
        )
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(startEpochDay = 100, endEpochDay = 100)))
        assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(startEpochDay = null, endEpochDay = 5)))
    }

    @Test
    fun onlyTheFourReminderIntervalsAreAllowed() {
        for (ok in listOf(5, 10, 15, 30)) {
            assertEquals(emptyList<ValidationError>(), Validation.validateMedicine(validDraft.copy(reminderIntervalMinutes = ok)))
        }
        for (bad in listOf(0, 7, 60)) {
            assertEquals(listOf(ValidationError.INTERVAL_INVALID), Validation.validateMedicine(validDraft.copy(reminderIntervalMinutes = bad)))
        }
    }

    @Test
    fun medicineKeyIgnoresCaseAndSpacing() {
        assertEquals(Validation.medicineKey("panadol"), Validation.medicineKey("  Panadol "))
        assertEquals("augmentin 1g", Validation.medicineKey("Augmentin   1g"))
        assertEquals("بنادول", Validation.medicineKey("بنادول"))
    }
}
