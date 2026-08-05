package com.nijika21.yourmoney.domain.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Every read of "now" goes through here (TDD §10). Nothing in the app calls
 * `System.currentTimeMillis()` or `LocalDate.now()` directly — the day-roller,
 * the 7-day auto-close and the transfer-match window are all untestable
 * otherwise, because they are defined in terms of elapsed calendar time.
 */
interface TimeProvider {
    val clock: Clock
    fun nowMillis(): Long
    fun today(): LocalDate
    fun zone(): ZoneId
}

class SystemTimeProvider(
    override val clock: Clock = Clock.systemDefaultZone(),
) : TimeProvider {
    override fun nowMillis(): Long = clock.millis()
    override fun today(): LocalDate = LocalDate.now(clock)
    override fun zone(): ZoneId = clock.zone
}

/** Test double. Kept in main so both unit and instrumentation tests can use it. */
class FixedTimeProvider(
    private var instant: Instant,
    private val zone: ZoneId = ZoneId.of("Asia/Jakarta"),
) : TimeProvider {
    override val clock: Clock get() = Clock.fixed(instant, zone)
    override fun nowMillis(): Long = instant.toEpochMilli()
    override fun today(): LocalDate = LocalDate.ofInstant(instant, zone)
    override fun zone(): ZoneId = zone

    fun advanceMillis(delta: Long) {
        instant = instant.plusMillis(delta)
    }

    fun setTo(newInstant: Instant) {
        instant = newInstant
    }
}
