package com.example.tracker.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.tracker.R
import com.example.tracker.app
import com.example.tracker.databinding.DialogPatientBinding
import com.example.tracker.domain.Gender
import com.example.tracker.domain.Validation
import com.example.tracker.domain.ValidationError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Edits a patient's name and age. Field contents survive rotation through view state. */
class PatientDialogFragment : DialogFragment() {

    private var _binding: DialogPatientBinding? = null
    private val b get() = checkNotNull(_binding)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPatientBinding.inflate(layoutInflater)
        val args = requireArguments()
        if (savedInstanceState == null) {
            b.nameInput.setText(args.getString(ARG_NAME))
            b.ageInput.setText(Fmt.number(args.getInt(ARG_AGE)))
            setGender(b.gender, Gender.fromStored(args.getString(ARG_GENDER)))
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_patient)
            .setView(b.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener { save(dialog) }
        }
        return dialog
    }

    /** Never wider than 560dp on tablets and unfolded phones (CLAUDE.md 9). */
    override fun onStart() {
        super.onStart()
        Responsive.limitDialogWidth(dialog)
    }

    private fun save(dialog: AlertDialog) {
        val name = b.nameInput.text?.toString()
        val ageText = b.ageInput.text?.toString()
        val errors = Validation.validatePatient(name, ageText)
        b.nameLayout.error = when {
            ValidationError.PATIENT_NAME_EMPTY in errors -> getString(R.string.err_patient_name)
            ValidationError.PATIENT_NAME_TOO_LONG in errors -> resources.getQuantityString(
                R.plurals.err_name_too_long, Validation.MAX_NAME_LENGTH, Validation.MAX_NAME_LENGTH,
            )
            else -> null
        }
        b.ageLayout.error = if (ValidationError.AGE_INVALID in errors) getString(R.string.err_age) else null
        if (errors.isNotEmpty()) return
        val id = requireArguments().getLong(ARG_ID)
        lifecycleScope.launch {
            requireContext().app.repository.updatePatient(
                id, name.orEmpty(), checkNotNull(Validation.parseAge(ageText)), genderOf(b.gender),
            )
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply { putLong(RESULT_ID, id) })
            dialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val TAG = "patient_dialog"
        const val REQUEST_KEY = "patient_dialog_result"
        const val RESULT_ID = "id"
        private const val ARG_ID = "id"
        private const val ARG_NAME = "name"
        private const val ARG_AGE = "age"
        private const val ARG_GENDER = "gender"

        fun show(fm: FragmentManager, id: Long, name: String, age: Int, gender: Gender?) {
            if (fm.isStateSaved || fm.findFragmentByTag(TAG) != null) return
            PatientDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ID, id)
                    putString(ARG_NAME, name)
                    putInt(ARG_AGE, age)
                    putString(ARG_GENDER, gender?.name)
                }
            }.showNow(fm, TAG)
        }
    }
}
