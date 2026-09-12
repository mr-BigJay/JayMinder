package com.offlinejournal.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class JalaliCalendarTest {

    @Test
    fun convertsKnownGregorianDateToJalali() {
        // 2026-09-06 in Tehran should map to 1405/06/15 (15 Shahrivar 1405)
        val zdt = ZonedDateTime.of(2026, 9, 6, 12, 0, 0, 0, ZoneId.of("Asia/Tehran"))
        val jalali = JalaliCalendar.fromMillis(zdt.toInstant().toEpochMilli())
        assertEquals(1405, jalali.year)
        assertEquals(6, jalali.month)
        assertEquals(15, jalali.day)
    }

    @Test
    fun persianDigitsConversion() {
        assertEquals("۱۴۰۵", PersianFormatter.toPersianDigits(1405))
    }

    @Test
    fun jalaliRoundTrip() {
        val original = JalaliDate(1404, 1, 1)
        val (gy, gm, gd) = JalaliCalendar.toGregorian(original)
        val back = JalaliCalendar.fromGregorian(gy, gm, gd)
        assertEquals(original, back)
    }
}
