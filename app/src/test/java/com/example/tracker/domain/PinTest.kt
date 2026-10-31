package com.example.tracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {

    private val hasher = PinHasher(iterations = 1_000)

    @Test
    fun formatIsFourToEightDigits() {
        assertFalse(PinRules.isValidFormat("123"))
        assertTrue(PinRules.isValidFormat("1234"))
        assertTrue(PinRules.isValidFormat("12345678"))
        assertFalse(PinRules.isValidFormat("123456789"))
        assertFalse(PinRules.isValidFormat("12a4"))
        assertFalse(PinRules.isValidFormat(""))
        assertTrue(PinRules.isValidFormat("١٢٣٤")) // Arabic-Indic digits
    }

    @Test
    fun arabicIndicDigitsVerifyAgainstTheSameWesternPin() {
        val salt = hasher.newSalt()
        val stored = hasher.hash("1234", salt)
        assertTrue(hasher.verify("١٢٣٤", salt, stored))
    }

    @Test
    fun correctPinVerifiesWrongPinDoesNot() {
        val salt = hasher.newSalt()
        val stored = hasher.hash("2468", salt)
        assertTrue(hasher.verify("2468", salt, stored))
        assertFalse(hasher.verify("2469", salt, stored))
        assertFalse(hasher.verify("", salt, stored))
    }

    @Test
    fun hashIsSaltedAndNeverContainsThePin() {
        val saltA = hasher.newSalt()
        val saltB = hasher.newSalt()
        assertNotEquals(saltA, saltB)
        val a = hasher.hash("1234", saltA)
        val b = hasher.hash("1234", saltB)
        assertNotEquals(a, b) // same PIN, different salt: different hash
        assertFalse(a.contains("1234"))
        assertTrue(a.startsWith("pbkdf2-sha256:1000:"))
        assertEquals(1_000, a.split(':')[1].toInt())
    }

    @Test
    fun verifyUsesTheIterationsStoredWithTheHash() {
        val salt = hasher.newSalt()
        val strong = PinHasher(iterations = 2_000).hash("1234", salt)
        assertTrue(hasher.verify("1234", salt, strong))
        assertFalse(hasher.verify("1234", salt, "garbage"))
    }

    @Test
    fun fourWrongTriesDoNotLockTheFifthLocksForSixtySeconds() {
        var state = LockoutState(0, null)
        val now = 1_000_000L
        repeat(4) { state = PinLockout.onFailure(state, now) }
        assertEquals(LockoutState(4, null), state)
        assertEquals(0L, PinLockout.remainingMillis(state, now))
        assertEquals(1, PinLockout.attemptsLeft(state))

        state = PinLockout.onFailure(state, now)
        assertEquals(LockoutState(0, now + 60_000L), state)
        assertEquals(60_000L, PinLockout.remainingMillis(state, now))
        assertEquals(1L, PinLockout.remainingMillis(state, now + 59_999L))
        assertEquals(0L, PinLockout.remainingMillis(state, now + 60_000L))
    }

    @Test
    fun afterTheLockANewCountStarts() {
        val now = 5_000_000L
        val locked = LockoutState(0, now)
        val next = PinLockout.onFailure(locked, now + 61_000L)
        assertEquals(1, next.failedAttempts)
        assertNull(next.lockedUntilMillis)
    }

    @Test
    fun successClearsFailures() {
        assertEquals(LockoutState(0, null), PinLockout.onSuccess())
    }

    @Test
    fun clockMovedBackwardCannotLockLongerThanSixtySeconds() {
        val lockedAt = 10_000_000L
        val state = LockoutState(0, lockedAt + 60_000L)
        // Phone clock moved back by an hour: still at most 60 s.
        assertEquals(60_000L, PinLockout.remainingMillis(state, lockedAt - 3_600_000L))
    }
}
