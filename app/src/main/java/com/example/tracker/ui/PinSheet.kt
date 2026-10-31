package com.example.tracker.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.example.tracker.R
import com.example.tracker.app
import com.example.tracker.data.PinCheck
import com.example.tracker.data.TrackerRepository
import com.example.tracker.databinding.SheetPinBinding
import com.example.tracker.domain.PinEntry
import com.example.tracker.domain.PinEntry.Effect
import com.example.tracker.domain.PinEntry.Flow
import com.example.tracker.domain.PinEntry.Message
import com.example.tracker.domain.PinEntry.Step
import com.example.tracker.domain.PinRules
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The only way to enter the parent PIN (CLAUDE.md 8 and 9): a Material 3 bottom sheet with its own
 * keypad, so no system keyboard ever opens. Verify, create (onboarding), change and reset after
 * "Forgot PIN" are the same component. One sheet at a time ([show]); it survives rotation with
 * the step kept and typed digits cleared; digits are never written to saved state.
 */
class PinSheet : BottomSheetDialogFragment() {

    private var _binding: SheetPinBinding? = null
    private val b get() = checkNotNull(_binding)
    private lateinit var model: PinSheetModel
    private val dotViews = mutableListOf<View>()
    /** Less than [COMPACT_BELOW_DP] of height: keypad first, fewer words. */
    private var compact = false

    private val purpose get() = requireArguments().getString(ARG_PURPOSE).orEmpty()
    private val payload get() = requireArguments().getLong(ARG_PAYLOAD, NO_PAYLOAD)

    private val deviceCredential =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) model.forgotConfirmed()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[PinSheetModel::class.java]
        // Rotation: the model kept the entry in memory. Process death: only flow and step came back.
        val saved = savedInstanceState?.let { state ->
            val flow = Flow.entries.firstOrNull { it.name == state.getString(STATE_FLOW) }
            val step = Step.entries.firstOrNull { it.name == state.getString(STATE_STEP) }
            if (flow != null && step != null && step in flow.steps) PinEntry(flow, step) else null
        }
        val first = saved ?: PinEntry(Flow.entries.first { it.name == requireArguments().getString(ARG_FLOW) })
        model.start(first, requireContext().app.repository)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.apply {
            // Never wider than 480dp on tablets and unfolded phones, centered; always fully open.
            maxWidth = (MAX_WIDTH_DP * resources.displayMetrics.density).roundToInt()
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        // A hardware keyboard works too (digits, Delete, Enter).
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> model.typed('0' + (keyCode - KeyEvent.KEYCODE_0))
                in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> model.typed('0' + (keyCode - KeyEvent.KEYCODE_NUMPAD_0))
                KeyEvent.KEYCODE_DEL -> model.deleted()
                KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> model.submitted()
                else -> return@setOnKeyListener false
            }
            true
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetPinBinding.inflate(inflater, container, false)
        return b.root
    }

    @SuppressLint("SetTextI18n") // The keypad shows Western digits 0-9 in every language (CLAUDE.md 0c).
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val keys = with(b.keypad) { listOf(key0, key1, key2, key3, key4, key5, key6, key7, key8, key9) }
        keys.forEachIndexed { digit, key ->
            key.text = digit.toString()
            key.contentDescription = digit.toString()
            key.setOnClickListener { tap(it); model.typed('0' + digit) }
        }
        b.keypad.keyDelete.setOnClickListener { tap(it); model.deleted() }
        b.keypad.keyOk.setOnClickListener { tap(it); model.submitted() }
        b.header.title.text = requireArguments().getString(ARG_TITLE)
        // Little height (small phones, large fonts, landscape): the keypad comes first. No badge, no
        // drag handle (back and a tap outside still close the sheet) and shorter texts.
        compact = resources.configuration.screenHeightDp < COMPACT_BELOW_DP
        b.header.badge.isVisible = !compact
        b.dragHandle.isVisible = !compact
        if (compact) {
            listOf(b.header.title, b.header.message, b.header.stepLabel).forEach {
                it.maxLines = 2
                it.ellipsize = TextUtils.TruncateAt.END
            }
        }
        val keyguard = requireContext().getSystemService(KeyguardManager::class.java)
        b.forgot.forgotButton.setOnClickListener { startDeviceCredential(keyguard) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.entry.filterNotNull().collect { render(it, keyguard.isDeviceSecure) } }
                launch { model.effects.receiveAsFlow().collect { onEffect(it) } }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Flow and step only: typed digits and the new PIN never go into saved state.
        model.entry.value?.let {
            outState.putString(STATE_FLOW, it.flow.name)
            outState.putString(STATE_STEP, it.step.name)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dotViews.clear()
        _binding = null
    }

    private fun render(e: PinEntry, deviceSecure: Boolean) {
        val h = b.header
        h.title.text = when (e.flow) {
            // A verify that found no PIN, or "Forgot PIN": the title follows the new flow.
            Flow.CREATE -> if (requireArguments().getString(ARG_FLOW) == Flow.CREATE.name) {
                requireArguments().getString(ARG_TITLE)
            } else {
                getString(R.string.pin_create_title)
            }
            Flow.RESET -> getString(R.string.pin_reset_title)
            else -> requireArguments().getString(ARG_TITLE)
        }
        h.message.text = when (e.flow) {
            Flow.RESET -> getString(R.string.pin_reset_message)
            Flow.CREATE -> getString(R.string.pin_create_message)
            // "The current PIN is needed first" belongs to step 1 only.
            Flow.CHANGE -> if (e.step == Step.CURRENT) requireArguments().getString(ARG_MESSAGE) else null
            Flow.VERIFY -> requireArguments().getString(ARG_MESSAGE)
        }
        // With little height the step label alone says what to type; a one-step check keeps its message.
        h.message.isVisible = !h.message.text.isNullOrEmpty() && !(compact && e.stepCount > 1)
        h.stepLabel.isVisible = e.stepCount > 1
        if (e.stepCount > 1) h.stepLabel.text = getString(R.string.pin_step, e.stepNumber, e.stepCount, stepName(e.step))
        renderDots(e)
        h.status.text = when {
            e.locked -> resources.getQuantityString(R.plurals.pin_locked, e.lockedSeconds, e.lockedSeconds)
            e.busy -> getString(R.string.pin_checking)
            else -> when (val m = e.message) {
                is Message.Wrong -> resources.getQuantityString(R.plurals.pin_wrong_attempts, m.attemptsLeft, m.attemptsLeft)
                Message.Mismatch -> getString(R.string.pin_mismatch)
                Message.TooShort -> resources.getQuantityString(
                    R.plurals.pin_format_error, PinRules.MAX_LENGTH, PinRules.MIN_LENGTH, PinRules.MAX_LENGTH,
                )
                null -> ""
            }
        }
        // Errors and the lockout in the error color, next to words that say what happened.
        h.status.setTextColor(if (e.busy && !e.locked) h.message.currentTextColor else requireContext().getColor(R.color.error))
        // A reserved empty line keeps the keypad still on tall screens; with little height it goes.
        h.status.isVisible = !compact || h.status.text.isNotEmpty()
        h.checking.isVisible = e.busy
        // "Forgot PIN" only where the current PIN is asked, and only with a phone screen lock.
        val asksCurrent = e.step == Step.CURRENT
        b.forgot.forgotButton.isVisible = asksCurrent
        b.forgot.forgotButton.isEnabled = deviceSecure && !e.busy
        b.forgot.forgotNote.isVisible = asksCurrent && !deviceSecure
        with(b.keypad) {
            listOf(key0, key1, key2, key3, key4, key5, key6, key7, key8, key9, keyDelete).forEach { it.isEnabled = e.keysEnabled }
            keyOk.isEnabled = e.canSubmit
        }
    }

    private fun stepName(step: Step): String = when (step) {
        Step.CURRENT -> getString(R.string.pin_step_current)
        Step.NEW -> resources.getQuantityString(R.plurals.pin_step_new, PinRules.MAX_LENGTH, PinRules.MIN_LENGTH, PinRules.MAX_LENGTH)
        Step.REPEAT -> getString(R.string.pin_step_repeat)
    }

    /** 4 dots (more for longer PINs); a new digit's dot springs in. */
    private fun renderDots(e: PinEntry) {
        val row = b.header.dots
        val size = (DOT_DP * resources.displayMetrics.density).roundToInt()
        val gap = (DOT_GAP_DP * resources.displayMetrics.density).roundToInt()
        while (dotViews.size < e.dotCount) {
            val dot = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginStart = gap / 2; marginEnd = gap / 2 }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            row.addView(dot)
            dotViews += dot
        }
        while (dotViews.size > e.dotCount) row.removeView(dotViews.removeAt(dotViews.lastIndex))
        dotViews.forEachIndexed { i, dot ->
            val filled = i < e.digits.length
            val wasFilled = dot.tag == true
            dot.tag = filled
            dot.setBackgroundResource(if (filled) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty)
            if (filled && !wasFilled && Motion.animationsOn()) {
                dot.scaleX = DOT_START_SCALE
                dot.scaleY = DOT_START_SCALE
                listOf(DynamicAnimation.SCALE_X, DynamicAnimation.SCALE_Y).forEach { property ->
                    SpringAnimation(dot, property, 1f).apply {
                        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        spring.stiffness = SpringForce.STIFFNESS_MEDIUM
                    }.start()
                }
            } else if (!Motion.animationsOn() || !filled) {
                dot.scaleX = 1f
                dot.scaleY = 1f
            }
        }
        row.contentDescription = resources.getQuantityString(R.plurals.pin_dots_entered, e.digits.length, e.digits.length)
    }

    private fun onEffect(effect: Effect) {
        when (effect) {
            Effect.Reject -> {
                b.root.performHapticFeedback(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
                    else HapticFeedbackConstants.LONG_PRESS
                )
                // With animations off there is no shake; the reset dots and the message say it.
                if (Motion.animationsOn()) {
                    ObjectAnimator.ofFloat(b.header.dots, View.TRANSLATION_X, 0f, 24f, -24f, 18f, -18f, 8f, -8f, 0f)
                        .setDuration(SHAKE_MILLIS)
                        .start()
                }
            }
            Effect.Done -> deliver()
            else -> Unit
        }
    }

    private fun tap(key: View) {
        key.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    @Suppress("DEPRECATION") // createConfirmDeviceCredentialIntent works on every API level, no library needed.
    private fun startDeviceCredential(keyguard: KeyguardManager) {
        val intent = keyguard.createConfirmDeviceCredentialIntent(
            getString(R.string.device_credential_title),
            getString(R.string.device_credential_description),
        ) ?: return
        deviceCredential.launch(intent)
    }

    private fun deliver() {
        val flow = model.entry.value?.flow ?: return
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(RESULT_PURPOSE, purpose)
                putLong(RESULT_PAYLOAD, payload)
                putString(RESULT_FLOW, flow.name)
            },
        )
        dismissAllowingStateLoss()
    }

    companion object {
        const val TAG = "pin_sheet"
        const val REQUEST_KEY = "pin_sheet_result"
        const val RESULT_PURPOSE = "purpose"
        const val RESULT_PAYLOAD = "payload"
        /** The flow that finished: VERIFY, CREATE, CHANGE or RESET. */
        const val RESULT_FLOW = "flow"
        const val NO_PAYLOAD = -1L

        private const val ARG_FLOW = "flow"
        private const val ARG_PURPOSE = "purpose"
        private const val ARG_PAYLOAD = "payload"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val STATE_FLOW = "state_flow"
        private const val STATE_STEP = "state_step"
        private const val MAX_WIDTH_DP = 480
        private const val COMPACT_BELOW_DP = 600
        private const val DOT_DP = 16
        private const val DOT_GAP_DP = 16
        private const val DOT_START_SCALE = 0.4f
        private const val SHAKE_MILLIS = 400L

        /** Asks for the PIN for one action. Returns false when a PIN sheet is already open. */
        fun verify(fm: FragmentManager, purpose: String, title: String, message: String?, payload: Long = NO_PAYLOAD): Boolean =
            show(fm, Flow.VERIFY, purpose, payload, title, message)

        /** First PIN. [cancelable] false on the first screen: the app cannot be used without it. */
        fun create(fm: FragmentManager, purpose: String, title: String, cancelable: Boolean = true): Boolean =
            show(fm, Flow.CREATE, purpose, NO_PAYLOAD, title, null, cancelable)

        /** Change PIN: the current PIN, then the new one twice. Delivers once it is saved. */
        fun change(fm: FragmentManager, purpose: String, title: String, message: String?): Boolean =
            show(fm, Flow.CHANGE, purpose, NO_PAYLOAD, title, message)

        private fun show(
            fm: FragmentManager,
            flow: Flow,
            purpose: String,
            payload: Long,
            title: String,
            message: String?,
            cancelable: Boolean = true,
        ): Boolean {
            if (fm.isStateSaved || fm.findFragmentByTag(TAG) != null) return false
            PinSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_FLOW, flow.name)
                    putString(ARG_PURPOSE, purpose)
                    putLong(ARG_PAYLOAD, payload)
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                }
                isCancelable = cancelable
            }.showNow(fm, TAG)
            return true
        }
    }
}

/**
 * Keeps the PIN entry in memory across a rotation and runs the PIN check and save, so a rotation in
 * the middle neither repeats nor loses them. Nothing here is written to saved state.
 */
class PinSheetModel : ViewModel() {
    val entry = MutableStateFlow<PinEntry?>(null)
    /** One-shot effects for the screen; buffered, so one sent during a rotation still arrives. */
    val effects = Channel<Effect>(Channel.BUFFERED)
    private lateinit var repository: TrackerRepository
    private var countdown: Job? = null

    fun start(first: PinEntry, repository: TrackerRepository) {
        this.repository = repository
        val kept = entry.value
        if (kept != null) {
            entry.value = kept.restored()
        } else {
            entry.value = first.restored()
            if (first.step == Step.CURRENT) viewModelScope.launch { followLock(repository.pinLockRemainingMillis()) }
        }
    }

    fun typed(digit: Char) = entry.value?.let { apply(it.typed(digit)) }
    fun deleted() = entry.value?.let { apply(it.deleted()) }
    fun submitted() = entry.value?.let { apply(it.submitted()) }
    fun forgotConfirmed() = entry.value?.let { apply(it.forgotConfirmed()) }

    private fun apply(next: PinEntry.Next) {
        entry.value = next.entry
        when (val effect = next.effect) {
            is Effect.Verify -> viewModelScope.launch {
                val result = when (val r = repository.verifyPin(effect.pin)) {
                    PinCheck.Ok -> PinEntry.Check.Ok
                    PinCheck.NoPin -> PinEntry.Check.NoPin
                    is PinCheck.Wrong -> PinEntry.Check.Wrong(r.attemptsLeft)
                    is PinCheck.Locked -> PinEntry.Check.Locked(r.remainingMillis)
                }
                entry.value?.let { apply(it.checked(result)) }
                if (result is PinEntry.Check.Locked) followLock(result.remainingMillis)
            }
            is Effect.Save -> viewModelScope.launch {
                repository.setPin(effect.pin)
                entry.value?.let { apply(it.saved()) }
            }
            Effect.Reject, Effect.Done -> effects.trySend(effect)
            Effect.None -> Unit
        }
    }

    /** Counts the lockout down from the stored lock-until time, so it matches every other screen. */
    private fun followLock(remainingMillis: Long) {
        if (remainingMillis <= 0) return
        countdown?.cancel()
        countdown = viewModelScope.launch {
            var left = remainingMillis
            while (left > 0) {
                entry.value?.let { entry.value = it.lockTicked(left).entry }
                delay(minOf(1000L, left))
                left = repository.pinLockRemainingMillis()
            }
            entry.value?.let { entry.value = it.lockTicked(0).entry }
        }
    }
}
