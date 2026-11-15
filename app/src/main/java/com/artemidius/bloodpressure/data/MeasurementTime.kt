package com.artemidius.bloodpressure.data

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Material's date picker represents calendar dates as UTC midnight, not local instants. */
internal fun pickerDate(timestamp: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun measurementTimestamp(date: Long, hour: Int, minute: Int, zone: ZoneId): Long =
    Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC).toLocalDate()
        .atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
