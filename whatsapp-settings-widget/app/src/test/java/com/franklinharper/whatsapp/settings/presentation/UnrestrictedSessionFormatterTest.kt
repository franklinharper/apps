package com.franklinharper.whatsapp.settings.presentation

import com.franklinharper.whatsapp.settings.domain.UnrestrictedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class UnrestrictedSessionFormatterTest {

    private val formatter = UnrestrictedSessionFormatter(Locale.US)

    @Test
    fun `formats same day completed session for today`() {
        val now = millis(2026, Calendar.APRIL, 26, 12, 0)
        val start = millis(2026, Calendar.APRIL, 26, 9, 15)
        val end = millis(2026, Calendar.APRIL, 26, 10, 45)

        val ui = formatter.format(UnrestrictedSession(1, start, end), now)

        assertEquals("Today", ui.header)
        assertEquals("9:15 AM – 10:45 AM", ui.timeRange)
        assertEquals("Approx. 1 hr 30 min", ui.duration)
        assertFalse(ui.ongoing)
    }

    @Test
    fun `formats same day completed session for yesterday`() {
        val now = millis(2026, Calendar.APRIL, 26, 12, 0)
        val start = millis(2026, Calendar.APRIL, 25, 19, 0)
        val end = millis(2026, Calendar.APRIL, 25, 19, 45)

        val ui = formatter.format(UnrestrictedSession(1, start, end), now)

        assertEquals("Yesterday", ui.header)
        assertEquals("7:00 PM – 7:45 PM", ui.timeRange)
        assertEquals("Approx. 45 min", ui.duration)
    }

    @Test
    fun `formats cross midnight completed session`() {
        val now = millis(2026, Calendar.APRIL, 26, 12, 0)
        val start = millis(2026, Calendar.APRIL, 25, 23, 0)
        val end = millis(2026, Calendar.APRIL, 26, 1, 30)

        val ui = formatter.format(UnrestrictedSession(1, start, end), now)

        assertEquals("Yesterday – Today", ui.header)
        assertEquals("Apr 25, 2026, 11:00 PM – Apr 26, 2026, 1:30 AM", ui.timeRange)
        assertEquals("Approx. 2 hr 30 min", ui.duration)
    }

    @Test
    fun `formats multi day completed session`() {
        val now = millis(2026, Calendar.APRIL, 26, 12, 0)
        val start = millis(2026, Calendar.APRIL, 20, 9, 0)
        val end = millis(2026, Calendar.APRIL, 23, 16, 0)

        val ui = formatter.format(UnrestrictedSession(1, start, end), now)

        assertEquals("Apr 20, 2026 – Apr 23, 2026", ui.header)
        assertEquals("Approx. 3 days 7 hr", ui.duration)
    }

    @Test
    fun `formats ongoing same day session`() {
        val now = millis(2026, Calendar.APRIL, 26, 11, 25)
        val start = millis(2026, Calendar.APRIL, 26, 9, 15)

        val ui = formatter.format(UnrestrictedSession(1, start, null), now)

        assertEquals("Today", ui.header)
        assertEquals("9:15 AM – Now", ui.timeRange)
        assertEquals("Approx. 2 hr 10 min so far", ui.duration)
        assertTrue(ui.ongoing)
    }

    @Test
    fun `formats ongoing multi day session`() {
        val now = millis(2026, Calendar.APRIL, 26, 10, 0)
        val start = millis(2026, Calendar.APRIL, 24, 22, 0)

        val ui = formatter.format(UnrestrictedSession(1, start, null), now)

        assertEquals("Apr 24, 2026 – Now", ui.header)
        assertEquals("Apr 24, 2026, 10:00 PM – Now", ui.timeRange)
        assertEquals("Approx. 1 day 12 hr so far", ui.duration)
        assertTrue(ui.ongoing)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(Locale.US).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
