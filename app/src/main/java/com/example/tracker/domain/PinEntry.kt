package com.example.tracker.domain

import kotlin.math.ceil

/**
 * What the PIN sheet shows and accepts (CLAUDE.md 8 and 9). Pure, so every rule is unit-tested
 * (PinEntryTest). Typed digits live only here, in memory: the sheet saves its [flow] and [step],
 * never digits. The PIN is 4 to 8 digits ([PinRules]); 4 dots show, more appear for longer PINs,
 * and the OK key submits (no auto-submit: a longer PIN would count as wrong after 4 digits).
 */
data class PinEntry(
    val flow: Flow,
    val step: Step = flow.steps.first(),
    val digits: String = "",
    /** The new PIN typed in step NEW, kept until it is repeated. Memory only. */
    val firstNew: String? = null,
    val message: Message? = null,
    val lockedSeconds: Int = 0,
    val busy: Boolean = false,
) {
    enum class Flow(val steps: List<Step>) {
        /** Check the PIN (confirm a pill, add, edit, gated settings). */
        VERIFY(listOf(Step.CURRENT)),
        /** First PIN (onboarding, or a verify that found no PIN). */
        CREATE(listOf(Step.NEW, Step.REPEAT)),
        /** Change PIN: the current one first, then the new one twice. */
        CHANGE(listOf(Step.CURRENT, Step.NEW, Step.REPEAT)),
        /** After "Forgot PIN" passed the phone's screen lock. */
        RESET(listOf(Step.NEW, Step.REPEAT)),
    }

    enum class Step { CURRENT, NEW, REPEAT }

    sealed interface Message {
        data class Wrong(val attemptsLeft: Int) : Message
        data object Mismatch : Message
        data object TooShort : Message
    }

    /** Result of a PIN check by the repository. */
    sealed interface Check {
        data object Ok : Check
        data object NoPin : Check
        data class Wrong(val attemptsLeft: Int) : Check
        data class Locked(val remainingMillis: Long) : Check
    }

    sealed interface Effect {
        data object None : Effect
        /** Check [pin] against the stored hash (counts failures and the lockout). */
        data class Verify(val pin: String) : Effect
        /** Store [pin] as the new parent PIN. */
        data class Save(val pin: String) : Effect
        /** Wrong PIN or the two new PINs differ: shake and reject haptic. */
        data object Reject : Effect
        /** The PIN was accepted (or saved): deliver the result and close. */
        data object Done : Effect
    }

    data class Next(val entry: PinEntry, val effect: Effect = Effect.None)

    val stepNumber: Int get() = flow.steps.indexOf(step) + 1
    val stepCount: Int get() = flow.steps.size
    val locked: Boolean get() = lockedSeconds > 0
    /** Keys work only while no check runs and no lockout is on. */
    val keysEnabled: Boolean get() = !busy && !locked
    /** OK: enough digits, and never while locked or checking. */
    val canSubmit: Boolean get() = keysEnabled && digits.length >= PinRules.MIN_LENGTH
    /** Dots to draw: 4, one more for every digit after the 4th (up to 8). */
    val dotCount: Int get() = maxOf(PinRules.MIN_LENGTH, digits.length)

    fun typed(digit: Char): Next {
        if (!keysEnabled || digit !in '0'..'9' || digits.length >= PinRules.MAX_LENGTH) return Next(this)
        // A new attempt starts: the old message goes.
        val clear = if (digits.isEmpty()) null else message
        return Next(copy(digits = digits + digit, message = clear))
    }

    fun deleted(): Next = if (!keysEnabled || digits.isEmpty()) Next(this) else Next(copy(digits = digits.dropLast(1)))

    fun submitted(): Next {
        if (!keysEnabled) return Next(this)
        if (digits.length < PinRules.MIN_LENGTH) return Next(copy(message = Message.TooShort), Effect.Reject)
        return when (step) {
            Step.CURRENT -> Next(copy(busy = true, message = null), Effect.Verify(digits))
            Step.NEW -> Next(copy(step = Step.REPEAT, firstNew = digits, digits = "", message = null))
            Step.REPEAT ->
                if (digits == firstNew) {
                    Next(copy(busy = true, message = null), Effect.Save(digits))
                } else {
                    // Start the new PIN over: both entries are typed again.
                    Next(copy(step = Step.NEW, firstNew = null, digits = "", message = Message.Mismatch), Effect.Reject)
                }
        }
    }

    /** The repository answered the check of step CURRENT. */
    fun checked(result: Check): Next = when (result) {
        Check.Ok ->
            if (flow == Flow.CHANGE) Next(copy(step = Step.NEW, digits = "", busy = false, message = null))
            else Next(copy(digits = "", busy = false), Effect.Done)
        Check.NoPin -> Next(PinEntry(Flow.CREATE))
        is Check.Wrong -> Next(copy(digits = "", busy = false, message = Message.Wrong(result.attemptsLeft)), Effect.Reject)
        is Check.Locked -> Next(copy(digits = "", busy = false, message = null, lockedSeconds = seconds(result.remainingMillis)), Effect.Reject)
    }

    fun saved(): Next = Next(copy(digits = "", firstNew = null, busy = false), Effect.Done)

    /** The lockout countdown, from the time left that the repository reports. */
    fun lockTicked(remainingMillis: Long): Next = Next(copy(lockedSeconds = seconds(remainingMillis)))

    /** "Forgot PIN" passed the phone's screen lock: a new PIN is set, nothing else changes. */
    fun forgotConfirmed(): Next = Next(PinEntry(Flow.RESET))

    /**
     * After a rotation: typed digits are gone, the step stays. A REPEAT step without the first new
     * PIN (the process was killed and only the saved step came back) starts the new PIN over.
     */
    fun restored(): PinEntry {
        val back = copy(digits = "", message = null)
        return if (back.step == Step.REPEAT && back.firstNew == null) back.copy(step = Step.NEW) else back
    }

    companion object {
        fun seconds(millis: Long): Int = if (millis <= 0) 0 else ceil(millis / 1000.0).toInt()
    }
}
