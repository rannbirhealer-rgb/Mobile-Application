package com.example.alarmboss.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {

    @Test
    fun `only exactly four digits make a valid pin`() {
        assertTrue(PinSecurity.isValidPin("0000"))
        assertTrue(PinSecurity.isValidPin("4821"))
        assertFalse(PinSecurity.isValidPin(""))
        assertFalse(PinSecurity.isValidPin("123"))
        assertFalse(PinSecurity.isValidPin("12345"))
        assertFalse(PinSecurity.isValidPin("12a4"))
        assertFalse(PinSecurity.isValidPin("12 4"))
        assertFalse(PinSecurity.isValidPin("١٢٣٤"))
    }

    @Test
    fun `the right pin matches and any other does not`() {
        val salt = PinSecurity.newSalt()
        val stored = PinSecurity.hash("4821", salt)
        assertTrue(PinSecurity.matches("4821", salt, stored))
        assertFalse(PinSecurity.matches("4822", salt, stored))
        assertFalse(PinSecurity.matches("", salt, stored))
    }

    @Test
    fun `the stored value is not the pin`() {
        val salt = PinSecurity.newSalt()
        val stored = PinSecurity.hash("4821", salt)
        // A fixed, long, non-reversible value, never the digits themselves. (No substring check: a
        // random 64-digit hex string can contain "4821" by chance and would make the test flaky.)
        assertNotEquals("4821", stored)
        assertEquals(64, stored.length)
    }

    @Test
    fun `the same pin hashes differently under different salts`() {
        val first = PinSecurity.hash("4821", PinSecurity.newSalt())
        val second = PinSecurity.hash("4821", PinSecurity.newSalt())
        assertNotEquals(first, second)
    }

    @Test
    fun `salts are random and well formed`() {
        val salt = PinSecurity.newSalt()
        assertEquals(32, salt.length)
        assertNotEquals(salt, PinSecurity.newSalt())
    }

    @Test
    fun `a malformed salt or hash never matches or crashes`() {
        assertFalse(PinSecurity.matches("4821", "xyz", "abc"))
    }

    @Test
    fun `the first few wrong guesses are free`() {
        for (failures in 0 until PinSecurity.FREE_ATTEMPTS) {
            assertEquals(0L, PinSecurity.lockoutMillis(failures))
        }
    }

    @Test
    fun `lockout starts at thirty seconds and doubles`() {
        assertEquals(30_000L, PinSecurity.lockoutMillis(5))
        assertEquals(60_000L, PinSecurity.lockoutMillis(6))
        assertEquals(120_000L, PinSecurity.lockoutMillis(7))
    }

    @Test
    fun `lockout is capped and never overflows`() {
        assertEquals(15 * 60_000L, PinSecurity.lockoutMillis(20))
        assertEquals(15 * 60_000L, PinSecurity.lockoutMillis(Int.MAX_VALUE))
    }
}
