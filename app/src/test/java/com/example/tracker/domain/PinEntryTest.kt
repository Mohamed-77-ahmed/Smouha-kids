package com.example.tracker.domain

import com.example.tracker.domain.PinEntry.Check
import com.example.tracker.domain.PinEntry.Effect
import com.example.tracker.domain.PinEntry.Flow
import com.example.tracker.domain.PinEntry.Message
import com.example.tracker.domain.PinEntry.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The PIN sheet's input rules (CLAUDE.md 8 and 9), with no Android involved. */
class PinEntryTest {

    private fun PinEntry.type(pin: String): PinEntry = pin.fold(this) { e, c -> e.typed(c).entry }

    @Test
    fun digitsUpTo8BackspaceAndOnlyWesternDigits() {
        var e = PinEntry(Flow.VERIFY).type("123456789")
        assertEquals("12345678", e.digits)
        e = e.deleted().entry.deleted().entry
        assertEquals("123456", e.digits)
        assertEquals("123456", e.typed('x').entry.digits)
        assertEquals("", PinEntry(Flow.VERIFY).deleted().entry.digits)
    }

    @Test
    fun fourDotsGrowingWithLongerPins() {
        assertEquals(4, PinEntry(Flow.VERIFY).dotCount)
        assertEquals(4, PinEntry(Flow.VERIFY).type("1234").dotCount)
        assertEquals(6, PinEntry(Flow.VERIFY).type("123456").dotCount)
        assertEquals(8, PinEntry(Flow.VERIFY).type("123456789").dotCount)
    }

    @Test
    fun okOnlyFromFourDigits() {
        assertFalse(PinEntry(Flow.VERIFY).type("123").canSubmit)
        assertTrue(PinEntry(Flow.VERIFY).type("1234").canSubmit)
        val short = PinEntry(Flow.VERIFY).type("12").submitted()
        assertEquals(Effect.Reject, short.effect)
        assertEquals(Message.TooShort, short.entry.message)
    }

    @Test
    fun verifyAcceptsTheRightPin() {
        val submitted = PinEntry(Flow.VERIFY).type("1234").submitted()
        assertEquals(Effect.Verify("1234"), submitted.effect)
        assertTrue(submitted.entry.busy)
        val done = submitted.entry.checked(Check.Ok)
        assertEquals(Effect.Done, done.effect)
        assertEquals("", done.entry.digits)
    }

    @Test
    fun wrongPinResetsTheDotsAndSaysTriesLeft() {
        val wrong = PinEntry(Flow.VERIFY).type("0000").submitted().entry.checked(Check.Wrong(attemptsLeft = 3))
        assertEquals(Effect.Reject, wrong.effect)
        assertEquals("", wrong.entry.digits)
        assertEquals(Message.Wrong(3), wrong.entry.message)
        assertFalse(wrong.entry.busy)
        // The message stays until the next attempt starts.
        assertEquals(Message.Wrong(3), wrong.entry.deleted().entry.message)
        assertNull(wrong.entry.typed('1').entry.message)
    }

    @Test
    fun keysAndOkAreOffWhileChecking() {
        val busy = PinEntry(Flow.VERIFY).type("1234").submitted().entry
        assertEquals("1234", busy.typed('5').entry.digits)
        assertEquals("1234", busy.deleted().entry.digits)
        assertEquals(Effect.None, busy.submitted().effect)
        assertFalse(busy.canSubmit)
    }

    @Test
    fun lockoutDisablesKeysAndOkUntilTheCountdownEnds() {
        val locked = PinEntry(Flow.VERIFY).type("0000").submitted().entry.checked(Check.Locked(60_000)).entry
        assertEquals(60, locked.lockedSeconds)
        assertTrue(locked.locked)
        assertFalse(locked.keysEnabled)
        assertEquals("", locked.typed('1').entry.digits)
        assertEquals(Effect.None, locked.type("1234").submitted().effect)
        assertEquals(1, locked.lockTicked(400).entry.lockedSeconds)
        val open = locked.lockTicked(0).entry
        assertFalse(open.locked)
        assertTrue(open.type("1234").canSubmit)
    }

    @Test
    fun createAsksTwiceAndSavesOnlyAMatch() {
        val first = PinEntry(Flow.CREATE)
        assertEquals(Step.NEW, first.step)
        assertEquals(1 to 2, first.stepNumber to first.stepCount)
        val second = first.type("2580").submitted()
        assertEquals(Effect.None, second.effect)
        assertEquals(Step.REPEAT, second.entry.step)
        assertEquals("", second.entry.digits)
        assertEquals(2 to 2, second.entry.stepNumber to second.entry.stepCount)
        assertEquals(Effect.Save("2580"), second.entry.type("2580").submitted().effect)
    }

    @Test
    fun mismatchStartsTheNewPinOver() {
        val mismatch = PinEntry(Flow.CREATE).type("2580").submitted().entry.type("2581").submitted()
        assertEquals(Effect.Reject, mismatch.effect)
        assertEquals(Step.NEW, mismatch.entry.step)
        assertNull(mismatch.entry.firstNew)
        assertEquals(Message.Mismatch, mismatch.entry.message)
    }

    @Test
    fun changeChecksTheCurrentPinFirst() {
        val start = PinEntry(Flow.CHANGE)
        assertEquals(Step.CURRENT to (1 to 3), start.step to (start.stepNumber to start.stepCount))
        val verify = start.type("1234").submitted()
        assertEquals(Effect.Verify("1234"), verify.effect)
        // A wrong current PIN never reaches the new-PIN steps.
        val wrong = verify.entry.checked(Check.Wrong(4)).entry
        assertEquals(Step.CURRENT, wrong.step)
        val next = verify.entry.checked(Check.Ok)
        assertEquals(Effect.None, next.effect)
        assertEquals(Step.NEW to 2, next.entry.step to next.entry.stepNumber)
        val repeat = next.entry.type("5555").submitted().entry
        assertEquals(Step.REPEAT to 3, repeat.step to repeat.stepNumber)
        val save = repeat.type("5555").submitted()
        assertEquals(Effect.Save("5555"), save.effect)
        assertEquals(Effect.Done, save.entry.saved().effect)
    }

    @Test
    fun noPinYetTurnsIntoCreate() {
        val next = PinEntry(Flow.VERIFY).type("1234").submitted().entry.checked(Check.NoPin).entry
        assertEquals(Flow.CREATE, next.flow)
        assertEquals(Step.NEW, next.step)
    }

    @Test
    fun forgotPinSetsANewPinInTwoSteps() {
        val reset = PinEntry(Flow.VERIFY).type("12").forgotConfirmed().entry
        assertEquals(Flow.RESET, reset.flow)
        assertEquals(Step.NEW, reset.step)
        assertEquals("", reset.digits)
    }

    @Test
    fun rotationClearsDigitsAndKeepsTheStep() {
        val repeat = PinEntry(Flow.CHANGE).type("1234").submitted().entry.checked(Check.Ok).entry
            .type("5555").submitted().entry.type("55")
        val back = repeat.restored()
        assertEquals("", back.digits)
        assertEquals(Step.REPEAT, back.step)
        assertEquals("5555", back.firstNew)
        // Only the step came back (process killed): the new PIN starts over.
        assertEquals(Step.NEW, PinEntry(Flow.CHANGE, step = Step.REPEAT).restored().step)
    }

    @Test
    fun lockSecondsRoundUp() {
        assertEquals(0, PinEntry.seconds(0))
        assertEquals(1, PinEntry.seconds(1))
        assertEquals(60, PinEntry.seconds(60_000))
        assertEquals(60, PinEntry.seconds(59_001))
    }
}
