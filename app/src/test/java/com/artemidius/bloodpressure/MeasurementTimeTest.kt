package com.artemidius.bloodpressure

import com.artemidius.bloodpressure.data.measurementTimestamp
import com.artemidius.bloodpressure.data.pickerDate
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementTimeTest {
    @Test fun pickerDateKeepsTheLocalCalendarDayOnEitherSideOfUtc() {
        val timestamp = Instant.parse("2026-11-15T23:30:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-11-16T00:00:00Z").toEpochMilli(), pickerDate(timestamp, ZoneId.of("Asia/Tokyo")))
        assertEquals(Instant.parse("2026-11-15T00:00:00Z").toEpochMilli(), pickerDate(timestamp, ZoneId.of("America/Los_Angeles")))
    }

    @Test fun selectedLocalDateAndTimeRoundTripAcrossTimeZonesAndDstSeasons() {
        for (name in listOf("Europe/Vienna", "America/Los_Angeles", "Asia/Kolkata")) {
            val zone = ZoneId.of(name)
            for (text in listOf("2026-11-15T12:34:00Z", "2026-07-15T12:34:00Z")) {
                val instant = Instant.parse(text)
                val local = instant.atZone(zone)
                assertEquals(instant.toEpochMilli(), measurementTimestamp(pickerDate(instant.toEpochMilli(), zone), local.hour, local.minute, zone))
            }
        }
    }
}
