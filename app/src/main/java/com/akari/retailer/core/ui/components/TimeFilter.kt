package com.akari.retailer.core.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Time-filter presets used by every screen with a date selector.
 *
 * SPECIFIC_DAY and CUSTOM_RANGE require additional millis state on [TimeFilter].
 */
enum class TimeFilterPreset {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    SPECIFIC_DAY,
    CUSTOM_RANGE
}

/**
 * Immutable time-range selection. Universal across all screens.
 */
data class TimeFilter(
    val preset: TimeFilterPreset = TimeFilterPreset.THIS_MONTH,
    val specificDayMillis: Long? = null,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val label: String = ""
) {

    fun resolveRange(now: Long = System.currentTimeMillis()): LongRange {
        return when (preset) {
            TimeFilterPreset.TODAY ->
                LongRange(dayStart(now), dayEnd(now))

            TimeFilterPreset.THIS_WEEK ->
                LongRange(weekStart(now), weekEnd(now))

            TimeFilterPreset.THIS_MONTH ->
                LongRange(monthStart(now), monthEnd(now))

            TimeFilterPreset.THIS_YEAR ->
                LongRange(yearStart(now), yearEnd(now))

            TimeFilterPreset.SPECIFIC_DAY -> {
                val ts = specificDayMillis ?: now
                LongRange(dayStart(ts), dayEnd(ts))
            }

            TimeFilterPreset.CUSTOM_RANGE -> {
                val s = customStartMillis ?: now
                val e = customEndMillis ?: now
                // Ensure end >= start
                val realStart = minOf(s, e)
                val realEnd = maxOf(s, e)
                LongRange(dayStart(realStart), dayEnd(realEnd))
            }
        }
    }

    fun withComputedLabel(
        todayText: String,
        thisWeekText: String,
        thisMonthText: String,
        thisYearText: String,
        now: Long = System.currentTimeMillis()
    ): TimeFilter {
        val newLabel = when (preset) {
            TimeFilterPreset.TODAY -> todayText
            TimeFilterPreset.THIS_WEEK -> thisWeekText
            TimeFilterPreset.THIS_MONTH -> thisMonthText
            TimeFilterPreset.THIS_YEAR -> thisYearText
            TimeFilterPreset.SPECIFIC_DAY -> {
                val ts = specificDayMillis ?: now
                val fmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                fmt.format(Date(ts))
            }
            TimeFilterPreset.CUSTOM_RANGE -> {
                val s = customStartMillis ?: now
                val e = customEndMillis ?: now
                val fmt = SimpleDateFormat("MMM dd", Locale.getDefault())
                "${fmt.format(Date(minOf(s, e)))} - ${fmt.format(Date(maxOf(s, e)))}"
            }
        }
        return copy(label = newLabel)
    }

    companion object {
        fun dayStart(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun dayEnd(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        fun weekStart(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun weekEnd(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            add(Calendar.DAY_OF_WEEK, 6)
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        fun monthStart(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun monthEnd(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        fun yearStart(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        fun yearEnd(t: Long): Long = Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
