package com.example.alarmboss.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

/**
 * The parts of Parent Mode's PIN handling that need no Android: validating, salting and hashing a
 * PIN, comparing it safely, and deciding how long to lock out repeated wrong guesses. A four-digit
 * PIN has only ten thousand possibilities, so hashing alone cannot protect it from someone who can
 * read the stored value; the lockout is what stops a child simply trying every number on screen.
 */
object PinSecurity {
    const val PIN_LENGTH = 4

    /** Wrong guesses allowed before the lockout starts. */
    const val FREE_ATTEMPTS = 5

    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 15 * 60_000L
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt.hexToBytes(), ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex()
    }

    /** Compares in constant time, so how long a wrong PIN takes to reject reveals nothing. */
    fun matches(pin: String, salt: String, expectedHash: String): Boolean = runCatching {
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())
    }.getOrDefault(false) // damaged stored values must read as "wrong", never crash the unlock dialog

    /**
     * How long to refuse further attempts after [failedAttempts] wrong guesses in a row: nothing for
     * the first few, then 30 seconds, doubling with each further miss up to 15 minutes.
     */
    fun lockoutMillis(failedAttempts: Int): Long {
        if (failedAttempts < FREE_ATTEMPTS) return 0L
        val doublings = (failedAttempts - FREE_ATTEMPTS).coerceAtMost(10)
        return min(MAX_LOCKOUT_MS, BASE_LOCKOUT_MS shl doublings)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        if (length % 2 != 0) return ByteArray(0)
        return ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }
}
