package com.example.tracker.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinRules {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8
    const val MAX_ATTEMPTS = 5
    const val LOCK_MILLIS = 60_000L

    /** 4 to 8 digits. Arabic-Indic digits are accepted and treated as 0-9. */
    fun isValidFormat(pin: String): Boolean {
        val digits = Validation.westernDigits(pin)
        return digits.length in MIN_LENGTH..MAX_LENGTH && digits.all { it in '0'..'9' }
    }

    fun normalize(pin: String): String = Validation.westernDigits(pin)
}

/** Salted PBKDF2 hash. The PIN itself is never stored. */
class PinHasher(private val iterations: Int = DEFAULT_ITERATIONS) {

    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(
            PinRules.normalize(pin).toCharArray(),
            Base64.getDecoder().decode(salt),
            iterations,
            KEY_BITS,
        )
        val key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        spec.clearPassword()
        return "pbkdf2-sha256:$iterations:" + Base64.getEncoder().encodeToString(key)
    }

    fun verify(pin: String, salt: String, storedHash: String): Boolean {
        val iterationsUsed = storedHash.split(':').getOrNull(1)?.toIntOrNull() ?: return false
        val candidate = PinHasher(iterationsUsed).hash(pin, salt)
        return MessageDigest.isEqual(candidate.toByteArray(), storedHash.toByteArray())
    }

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        private const val KEY_BITS = 256
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}

data class LockoutState(val failedAttempts: Int, val lockedUntilMillis: Long?)

object PinLockout {

    /** Time left on the lock. Capped at 60 s so moving the clock backward cannot lock forever. */
    fun remainingMillis(state: LockoutState, nowMillis: Long): Long {
        val until = state.lockedUntilMillis ?: return 0
        return (until - nowMillis).coerceIn(0, PinRules.LOCK_MILLIS)
    }

    /** The 5th wrong attempt in a row locks entry for 60 seconds and starts a new count. */
    fun onFailure(state: LockoutState, nowMillis: Long): LockoutState {
        val fails = state.failedAttempts + 1
        return if (fails >= PinRules.MAX_ATTEMPTS) {
            LockoutState(failedAttempts = 0, lockedUntilMillis = nowMillis + PinRules.LOCK_MILLIS)
        } else {
            LockoutState(failedAttempts = fails, lockedUntilMillis = null)
        }
    }

    fun onSuccess(): LockoutState = LockoutState(failedAttempts = 0, lockedUntilMillis = null)

    fun attemptsLeft(state: LockoutState): Int = PinRules.MAX_ATTEMPTS - state.failedAttempts
}
