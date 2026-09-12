package com.offlinejournal.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object TehranTime {
    val zoneId: ZoneId = ZoneId.of("Asia/Tehran")

    fun nowMillis(): Long = System.currentTimeMillis()

    fun nowZoned(): ZonedDateTime = ZonedDateTime.now(zoneId)

    fun toZonedDateTime(epochMillis: Long): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zoneId)

    fun startOfDayMillis(epochMillis: Long): Long {
        val zdt = toZonedDateTime(epochMillis)
        return zdt.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun endOfDayMillis(epochMillis: Long): Long {
        val start = startOfDayMillis(epochMillis)
        return start + 24 * 60 * 60 * 1000 - 1
    }
}
