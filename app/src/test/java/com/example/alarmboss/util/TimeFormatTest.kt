package com.example.alarmboss.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `24-hour format zero-pads the hour`() {
        assertEquals("07:05", formatTime(7, 5, is24Hour = true))
    }

    @Test
    fun `24-hour format shows midnight as 00 and the afternoon as 13 to 23`() {
        assertEquals("00:00", formatTime(0, 0, is24Hour = true))
        assertEquals("18:30", formatTime(18, 30, is24Hour = true))
        assertEquals("23:59", formatTime(23, 59, is24Hour = true))
    }

    @Test
    fun `12-hour preference keeps the AM PM format`() {
        assertEquals("6:30 PM", formatTime(18, 30, is24Hour = false))
    }

    @Test
    fun `midnight formats as 12 AM`() {
        assertEquals("12:00 AM", formatTime12h(0, 0))
    }

    @Test
    fun `noon formats as 12 PM`() {
        assertEquals("12:00 PM", formatTime12h(12, 0))
    }

    @Test
    fun `morning hour formats with AM and zero-padded minutes`() {
        assertEquals("7:05 AM", formatTime12h(7, 5))
    }

    @Test
    fun `afternoon hour converts from 24-hour to 12-hour with PM`() {
        assertEquals("6:30 PM", formatTime12h(18, 30))
    }

    @Test
    fun `hour just before midnight formats as 11 PM`() {
        assertEquals("11:59 PM", formatTime12h(23, 59))
    }
}
