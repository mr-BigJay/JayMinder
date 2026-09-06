package com.offlinejournal.util

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    companion object {
        private val monthNames = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )

        fun monthName(month: Int): String = monthNames.getOrElse(month - 1) { "" }
    }
}

/**
 * Jalali (Persian Solar Hijri) calendar utilities.
 * Algorithm based on the standard jalaali conversion used in many open-source libraries.
 */
object JalaliCalendar {
    private val gregorianDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    fun fromGregorian(year: Int, month: Int, day: Int): JalaliDate {
        var gy = year
        var gm = month
        var gd = day

        var jy = if (gy > 1600) 979 else 0
        gy -= if (gy > 1600) 1600 else 621

        val gy2 = if (gm > 2) gy + 1 else gy
        var days = (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) - 80 + gd + gregorianDaysInMonth[gm - 1]

        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        jy += (days - 1) / 365
        if (days > 365) days = (days - 1) % 365

        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30

        return JalaliDate(jy, jm, jd)
    }

    fun fromMillis(epochMillis: Long): JalaliDate {
        val zdt = TehranTime.toZonedDateTime(epochMillis)
        return fromGregorian(zdt.year, zdt.monthValue, zdt.dayOfMonth)
    }

    fun toGregorian(jalali: JalaliDate): Triple<Int, Int, Int> {
        var jy = jalali.year
        var jm = jalali.month
        var jd = jalali.day

        jy += 1595
        var days = -355668 + (365 * jy) + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + jd
        days += if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186

        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1

        val salA = intArrayOf(0, 31, if (isGregorianLeap(gy)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return Triple(gy, gm, gd)
    }

    fun startOfJalaliDayMillis(jalali: JalaliDate): Long {
        val (y, m, d) = toGregorian(jalali)
        val zdt = java.time.LocalDate.of(y, m, d).atStartOfDay(TehranTime.zoneId)
        return zdt.toInstant().toEpochMilli()
    }

    fun startOfJalaliMonthMillis(jalali: JalaliDate): Long =
        startOfJalaliDayMillis(JalaliDate(jalali.year, jalali.month, 1))

    fun startOfJalaliYearMillis(year: Int): Long =
        startOfJalaliDayMillis(JalaliDate(year, 1, 1))

    fun daysInJalaliMonth(year: Int, month: Int): Int {
        if (month <= 6) return 31
        if (month <= 11) return 30
        return if (isLeapYear(year)) 30 else 29
    }

    fun isLeapYear(year: Int): Boolean {
        val r = year % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    private fun isGregorianLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
