package com.nijika21.yourmoney.domain.time

import java.time.LocalDate

/**
 * A local calendar day as a half-open epoch-millis range, `[from, until)`.
 *
 * Half-open on purpose: an inclusive upper bound has to be "23:59:59.999", and
 * a transaction stamped exactly at midnight then lands in both days or neither.
 *
 * The day boundary is the *device's* zone, not UTC. "Hari ini" in Jakarta is
 * seven hours off UTC, so a UTC-based window would file every evening
 * transaction under tomorrow — and the reminder (§3.7) would nag about a day the
 * user already finished.
 */
data class DayWindow(val from: Long, val until: Long) {
    operator fun contains(waktu: Long): Boolean = waktu >= from && waktu < until
}

fun TimeProvider.dayWindow(date: LocalDate = today()): DayWindow {
    val start = date.atStartOfDay(zone()).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli()
    return DayWindow(from = start, until = end)
}

/** The window covering the whole calendar month `date` falls in. */
fun TimeProvider.monthWindow(date: LocalDate = today()): DayWindow {
    val first = date.withDayOfMonth(1)
    val start = first.atStartOfDay(zone()).toInstant().toEpochMilli()
    val end = first.plusMonths(1).atStartOfDay(zone()).toInstant().toEpochMilli()
    return DayWindow(from = start, until = end)
}
