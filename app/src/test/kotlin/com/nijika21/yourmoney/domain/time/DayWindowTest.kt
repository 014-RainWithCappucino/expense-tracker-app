package com.nijika21.yourmoney.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DayWindowTest {

    private val jakarta = ZoneId.of("Asia/Jakarta")

    /** 2026-08-05 14:30 Jakarta = 07:30 UTC. */
    private val time = FixedTimeProvider(
        instant = Instant.parse("2026-08-05T07:30:00Z"),
        zone = jakarta,
    )

    @Test
    fun `today's window starts at local midnight, not UTC midnight`() {
        val window = time.dayWindow()

        assertEquals(
            Instant.parse("2026-08-04T17:00:00Z").toEpochMilli(),
            window.from,
        )
        assertEquals(
            Instant.parse("2026-08-05T17:00:00Z").toEpochMilli(),
            window.until,
        )
    }

    /**
     * The half-open boundary. An evening transaction filed under tomorrow is the
     * failure this guards: the reminder would then nag about a day the user has
     * already finished.
     */
    @Test
    fun `the window is half-open at both midnights`() {
        val window = time.dayWindow()

        assertTrue(window.from in window)
        assertFalse(window.until in window)
        assertTrue(window.until - 1 in window)
        assertFalse(window.from - 1 in window)
    }

    @Test
    fun `consecutive days abut with no gap and no overlap`() {
        val today = time.dayWindow(LocalDate.of(2026, 8, 5))
        val tomorrow = time.dayWindow(LocalDate.of(2026, 8, 6))

        assertEquals(today.until, tomorrow.from)
    }

    @Test
    fun `month window covers the whole calendar month`() {
        val window = time.monthWindow()

        assertEquals(time.dayWindow(LocalDate.of(2026, 8, 1)).from, window.from)
        assertEquals(time.dayWindow(LocalDate.of(2026, 9, 1)).from, window.until)
        assertTrue(time.dayWindow(LocalDate.of(2026, 8, 31)).until - 1 in window)
    }

    /**
     * Indonesia has not observed DST since 1964, but the app must not encode
     * that as an assumption — a device set to a DST zone still has to produce a
     * window that starts at that zone's actual midnight.
     */
    @Test
    fun `a DST transition day still starts at local midnight`() {
        val berlin = FixedTimeProvider(
            instant = Instant.parse("2026-03-29T12:00:00Z"),
            zone = ZoneId.of("Europe/Berlin"),
        )
        val window = berlin.dayWindow()

        assertEquals(Instant.parse("2026-03-28T23:00:00Z").toEpochMilli(), window.from)
        // 23 hours, not 24: the clocks jump forward inside this day.
        assertEquals(23 * 3_600_000L, window.until - window.from)
    }
}
