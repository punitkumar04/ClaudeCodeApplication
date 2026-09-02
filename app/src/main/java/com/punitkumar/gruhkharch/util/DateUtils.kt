package com.punitkumar.gruhkharch.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val shortDateFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
    private val monthYearFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    private val fullDateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
    private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    private fun toLocalDateTime(timestamp: Long): LocalDateTime =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()

    private fun toLocalDate(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

    fun formatDate(timestamp: Long): String = toLocalDateTime(timestamp).format(dateFormat)
    fun formatShortDate(timestamp: Long): String = toLocalDateTime(timestamp).format(shortDateFormat)
    fun formatMonthYear(timestamp: Long): String = toLocalDateTime(timestamp).format(monthYearFormat)
    fun formatFullDate(timestamp: Long): String = toLocalDateTime(timestamp).format(fullDateFormat)
    fun formatTime(timestamp: Long): String = toLocalDateTime(timestamp).format(timeFormat)

    fun getStartOfDay(timestamp: Long): Long {
        val date = toLocalDate(timestamp)
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getEndOfDay(timestamp: Long): Long {
        val date = toLocalDate(timestamp)
        return date.atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getStartOfMonth(timestamp: Long): Long {
        val date = toLocalDate(timestamp).withDayOfMonth(1)
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getEndOfMonth(timestamp: Long): Long {
        val date = toLocalDate(timestamp)
        val lastDay = date.withDayOfMonth(date.lengthOfMonth())
        return lastDay.atTime(23, 59, 59, 999_000_000)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getStartOfWeek(timestamp: Long): Long {
        val date = toLocalDate(timestamp)
        val startOfWeek = date.with(java.time.DayOfWeek.MONDAY)
        return startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun isToday(timestamp: Long): Boolean = toLocalDate(timestamp) == LocalDate.now()

    fun isYesterday(timestamp: Long): Boolean = toLocalDate(timestamp) == LocalDate.now().minusDays(1)

    fun getRelativeDateLabel(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "Today"
            isYesterday(timestamp) -> "Yesterday"
            else -> formatDate(timestamp)
        }
    }
}
