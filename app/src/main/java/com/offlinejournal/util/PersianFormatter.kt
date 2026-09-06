package com.offlinejournal.util

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PersianFormatter {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String = buildString(input.length) {
        input.forEach { char ->
            if (char in '0'..'9') {
                append(persianDigits[char - '0'])
            } else {
                append(char)
            }
        }
    }

    fun toPersianDigits(number: Int): String = toPersianDigits(number.toString())

    fun formatJalaliDate(epochMillis: Long, includeWeekday: Boolean = false): String {
        val jalali = JalaliCalendar.fromMillis(epochMillis)
        val monthName = JalaliDate.monthName(jalali.month)
        val base = "${toPersianDigits(jalali.day)} $monthName ${toPersianDigits(jalali.year)}"
        if (!includeWeekday) return base

        val zdt = TehranTime.toZonedDateTime(epochMillis)
        val weekday = weekdayName(zdt.dayOfWeek.value)
        return "$weekday، $base"
    }

    fun formatJalaliDateNumeric(epochMillis: Long): String {
        val jalali = JalaliCalendar.fromMillis(epochMillis)
        return toPersianDigits(
            String.format(
                Locale.US,
                "%04d/%02d/%02d",
                jalali.year,
                jalali.month,
                jalali.day
            )
        )
    }

    fun formatTime(epochMillis: Long): String {
        val zdt = TehranTime.toZonedDateTime(epochMillis)
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        return toPersianDigits(zdt.format(formatter))
    }

    fun formatDateTime(epochMillis: Long): String {
        return "${formatJalaliDate(epochMillis)} - ${formatTime(epochMillis)}"
    }

    private fun weekdayName(dayOfWeek: Int): String = when (dayOfWeek) {
        6 -> "شنبه"
        7 -> "یکشنبه"
        1 -> "دوشنبه"
        2 -> "سه‌شنبه"
        3 -> "چهارشنبه"
        4 -> "پنجشنبه"
        5 -> "جمعه"
        else -> ""
    }
}
