package com.example.tracker

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Settable clock for tests. The zone can change too (timezone change on the phone). */
class TestClock(start: LocalDateTime, private var zoneId: ZoneId = ZoneId.of("Africa/Cairo")) : Clock() {
    private var now: Instant = start.atZone(zoneId).toInstant()

    /** Moves to a local time in the current zone. */
    fun set(time: LocalDateTime) {
        now = time.atZone(zoneId).toInstant()
    }

    /** Same instant, different zone. */
    fun setZone(zone: ZoneId) {
        zoneId = zone
    }

    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = TestClock(LocalDateTime.ofInstant(now, zone), zone)
    override fun instant(): Instant = now
}
