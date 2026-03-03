package com.punitkumar.gruhkharch.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun formatShortDate(timestamp: Long): String = shortDateFormat.format(Date(timestamp))
    fun formatMonthYear(timestamp: Long): String = monthYearFormat.format(Date(timestamp))
    fun formatFullDate(timestamp: Long): String = fullDateFormat.format(Date(timestamp))
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getStartOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return getStartOfDay(cal.timeInMillis)
    }

    fun getEndOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return getEndOfDay(cal.timeInMillis)
    }

    fun getStartOfWeek(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        return getStartOfDay(cal.timeInMillis)
    }

    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val date = Calendar.getInstance()
        date.timeInMillis = timestamp
        return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        val date = Calendar.getInstance()
        date.timeInMillis = timestamp
        return yesterday.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
    }

    fun getRelativeDateLabel(timestamp: Long): String {
        return when {
            isToday(timestamp) -> "Today"
            isYesterday(timestamp) -> "Yesterday"
            else -> formatDate(timestamp)
        }
    }
}
