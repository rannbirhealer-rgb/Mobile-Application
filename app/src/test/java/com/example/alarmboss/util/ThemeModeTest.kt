package com.example.alarmboss.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `system default follows the phone`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemIsDark = false))
    }

    @Test
    fun `light and dark ignore the phone`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemIsDark = true))
        assertFalse(ThemeMode.LIGHT.isDark(systemIsDark = false))
        assertTrue(ThemeMode.DARK.isDark(systemIsDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemIsDark = false))
    }

    @Test
    fun `every mode survives being stored and read back`() {
        ThemeMode.values().forEach { assertEquals(it, ThemeMode.parse(it.name)) }
    }

    @Test
    fun `nothing stored or something unrecognised means system default`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.parse("SEPIA"))
    }
}
