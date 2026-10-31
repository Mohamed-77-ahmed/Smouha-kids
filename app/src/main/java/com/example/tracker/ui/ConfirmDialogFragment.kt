package com.example.tracker.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Yes/no question that survives rotation. Answers come back as a fragment result. */
class ConfirmDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.getString(ARG_TITLE))
            .setMessage(args.getString(ARG_MESSAGE))
            .setPositiveButton(args.getString(ARG_POSITIVE)) { _, _ -> answer(true) }
            .setNegativeButton(args.getString(ARG_NEGATIVE)) { _, _ -> answer(false) }
            .create()
    }

    /** Never wider than 560dp on tablets and unfolded phones (CLAUDE.md 9). */
    override fun onStart() {
        super.onStart()
        Responsive.limitDialogWidth(dialog)
    }

    private fun answer(yes: Boolean) {
        val args = requireArguments()
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(RESULT_PURPOSE, args.getString(ARG_PURPOSE))
                putLong(RESULT_PAYLOAD, args.getLong(ARG_PAYLOAD))
                putBoolean(RESULT_YES, yes)
            },
        )
    }

    companion object {
        const val TAG = "confirm_dialog"
        const val REQUEST_KEY = "confirm_dialog_result"
        const val RESULT_PURPOSE = "purpose"
        const val RESULT_PAYLOAD = "payload"
        const val RESULT_YES = "yes"

        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE = "positive"
        private const val ARG_NEGATIVE = "negative"
        private const val ARG_PURPOSE = "purpose"
        private const val ARG_PAYLOAD = "payload"

        fun show(
            fm: FragmentManager,
            purpose: String,
            title: String,
            message: String?,
            positive: String,
            negative: String,
            payload: Long = -1,
        ): Boolean {
            if (fm.isStateSaved || fm.findFragmentByTag(TAG) != null) return false
            ConfirmDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_POSITIVE, positive)
                    putString(ARG_NEGATIVE, negative)
                    putString(ARG_PURPOSE, purpose)
                    putLong(ARG_PAYLOAD, payload)
                }
            }.showNow(fm, TAG)
            return true
        }
    }
}
