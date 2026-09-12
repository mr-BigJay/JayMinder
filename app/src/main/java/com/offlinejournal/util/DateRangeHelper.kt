package com.offlinejournal.util

import com.offlinejournal.domain.model.DateFilterPeriod
import java.time.ZonedDateTime

object DateRangeHelper {
    fun rangeForPeriod(period: DateFilterPeriod, referenceMillis: Long = TehranTime.nowMillis()): Pair<Long, Long> {
        val zdt = TehranTime.toZonedDateTime(referenceMillis)
        return when (period) {
            DateFilterPeriod.DAY -> {
                val start = zdt.toLocalDate().atStartOfDay(TehranTime.zoneId).toInstant().toEpochMilli()
                val end = start + 24 * 60 * 60 * 1000
                start to end
            }
            DateFilterPeriod.WEEK -> {
                val dayOfWeek = zdt.dayOfWeek.value
                val daysFromSaturday = if (dayOfWeek == 6) 0 else dayOfWeek + 1
                val startDate = zdt.toLocalDate().minusDays(daysFromSaturday.toLong())
                val start = startDate.atStartOfDay(TehranTime.zoneId).toInstant().toEpochMilli()
                val end = start + 7 * 24 * 60 * 60 * 1000
                start to end
            }
            DateFilterPeriod.MONTH -> {
                val jalali = JalaliCalendar.fromMillis(referenceMillis)
                val start = JalaliCalendar.startOfJalaliMonthMillis(jalali)
                val nextMonth = if (jalali.month == 12) {
                    JalaliDate(jalali.year + 1, 1, 1)
                } else {
                    JalaliDate(jalali.year, jalali.month + 1, 1)
                }
                val end = JalaliCalendar.startOfJalaliMonthMillis(nextMonth)
                start to end
            }
            DateFilterPeriod.YEAR -> {
                val jalali = JalaliCalendar.fromMillis(referenceMillis)
                val start = JalaliCalendar.startOfJalaliYearMillis(jalali.year)
                val end = JalaliCalendar.startOfJalaliYearMillis(jalali.year + 1)
                start to end
            }
            DateFilterPeriod.ALL -> 0L to Long.MAX_VALUE
        }
    }

    fun labelForPeriod(period: DateFilterPeriod): String = when (period) {
        DateFilterPeriod.DAY -> "امروز"
        DateFilterPeriod.WEEK -> "این هفته"
        DateFilterPeriod.MONTH -> "این ماه"
        DateFilterPeriod.YEAR -> "امسال"
        DateFilterPeriod.ALL -> "همه"
    }

    fun isYesterday(epochMillis: Long, referenceMillis: Long = TehranTime.nowMillis()): Boolean {
        val refStart = TehranTime.toZonedDateTime(referenceMillis).toLocalDate()
            .atStartOfDay(TehranTime.zoneId).toInstant().toEpochMilli()
        val yesterdayStart = refStart - 24 * 60 * 60 * 1000
        val yesterdayEnd = refStart
        return epochMillis in yesterdayStart until yesterdayEnd
    }

    fun isToday(epochMillis: Long, referenceMillis: Long = TehranTime.nowMillis()): Boolean {
        val (start, end) = rangeForPeriod(DateFilterPeriod.DAY, referenceMillis)
        return epochMillis in start until end
    }
}
