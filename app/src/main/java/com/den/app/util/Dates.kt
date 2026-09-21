package com.den.app.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Dates {

    fun now(): Long = System.currentTimeMillis()

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun toLocal(ts: Long): LocalDateTime = Instant.ofEpochMilli(ts).atZone(zone()).toLocalDateTime()

    fun fromLocal(ldt: LocalDateTime): Long = ldt.atZone(zone()).toInstant().toEpochMilli()

    fun startOfDay(ts: Long): Long = toLocal(ts).toLocalDate().atStartOfDay(zone()).toInstant().toEpochMilli()

    fun startOfTomorrow(ts: Long): Long = startOfDay(ts) + 86_400_000L

    fun endOfDay(ts: Long): Long = startOfTomorrow(ts) - 1

    fun isToday(ts: Long): Boolean = startOfDay(ts) == startOfDay(now())

    fun isTomorrow(ts: Long): Boolean = startOfDay(ts) == startOfDay(now() + 86_400_000L)

    fun isOverdue(ts: Long): Boolean = now() > ts

    fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val weekdayFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val weekdayYearFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
    private val shortFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    private val fullFmt = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a", Locale.getDefault())

    fun formatTime(ts: Long): String =
        toLocal(ts).atZone(zone()).format(timeFmt)

    private fun weekdayDate(ts: Long): String {
        val ldt = toLocal(ts)
        return if (ldt.year == LocalDateTime.now().year) {
            ldt.format(weekdayFmt)
        } else {
            ldt.format(weekdayYearFmt)
        }
    }

    fun formatDate(ts: Long): String = weekdayDate(ts)

    fun formatDateShort(ts: Long): String = toLocal(ts).format(shortFmt)

    fun formatDateTime(ts: Long): String = toLocal(ts).format(fullFmt)

    fun formatBackup(ts: Long): String = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault()).format(toLocal(ts))

    private val daySuffix: (Int) -> String = { d ->
        val mod100 = d % 100
        val suffix = when {
            mod100 in 11..13 -> "th"
            d % 10 == 1 -> "st"
            d % 10 == 2 -> "nd"
            d % 10 == 3 -> "rd"
            else -> "th"
        }
        suffix
    }

    fun relativeDay(ts: Long): String {
        val ldt = toLocal(ts)
        val today = LocalDateTime.now()
        if (startOfDay(ts) == startOfDay(now())) return "Today"
        if (startOfDay(ts) == startOfDay(now() + 86_400_000L)) return "Tomorrow"
        if (startOfDay(ts) < startOfDay(now())) return "Overdue"
        val day = ldt.dayOfMonth
        return "${ldt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${ldt.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} $day${
            daySuffix(day)
        }"
    }

    fun humanDue(ts: Long): String {
        if (startOfDay(ts) == startOfDay(now())) return "Today · ${formatTime(ts)}"
        if (startOfDay(ts) == startOfDay(now() + 86_400_000L)) return "Tomorrow · ${formatTime(ts)}"
        return "${formatDate(ts)} · ${formatTime(ts)}"
    }

    fun humanDay(ts: Long, hasTime: Boolean): String {
        if (startOfDay(ts) == startOfDay(now())) return "Today"
        if (startOfDay(ts) == startOfDay(now() + 86_400_000L)) return "Tomorrow"
        return formatDateShort(ts)
    }

    // ---- calendar ----

    fun toLocalDate(ts: Long): LocalDate = toLocal(ts).toLocalDate()

    fun dayMillis(date: LocalDate): Long = date.atStartOfDay(zone()).toInstant().toEpochMilli()

    fun startOfMonth(ts: Long): LocalDate = toLocalDate(ts).withDayOfMonth(1)

    fun shiftMonth(ts: Long, months: Long): LocalDate =
        YearMonth.from(toLocalDate(ts)).plusMonths(months).atDay(1)

    fun monthLabel(year: Int, month: Int): String {
        val monthName = java.time.Month.of(month + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())
        return "$monthName $year"
    }

    fun monthGrid(year: Int, month: Int, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<LocalDate> {
        val first = YearMonth.of(year, month + 1).atDay(1)
        val lead = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val leadStart = first.minusDays(lead.toLong())
        return (0 until 42).map { leadStart.plusDays(it.toLong()) }
    }

    fun isSameMonth(date: LocalDate, year: Int, month: Int): Boolean =
        date.year == year && date.monthValue == month + 1
}