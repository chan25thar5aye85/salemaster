package com.akari.retailer.core.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats money amounts for display and input.
 *
 * Uses the default locale's thousand separators (e.g., "1,234,567" in en-US).
 */
object MoneyFormatter {

    private val formatter: NumberFormat =
        NumberFormat.getIntegerInstance(Locale.getDefault())

    /** Format an amount for display (e.g., "1,234,567"). */
    fun format(amount: Int): String = formatter.format(amount.toLong())

    /** Format for a text input — same as display. */
    fun formatForInput(amount: Int): String = formatter.format(amount.toLong())

    /** Format a total for display. Currently identical to [format]. */
    fun formatTotal(amount: Int): String = formatter.format(amount.toLong())

    /**
     * Parse a user-typed string into an Int, tolerating separators and
     * whitespace. Returns 0 on unparseable input.
     */
    fun parse(input: String): Int {
        if (input.isEmpty()) return 0
        // Strip everything except digits and a leading minus
        val cleaned = input.filter { it.isDigit() || it == '-' }
        return cleaned.toIntOrNull() ?: 0
    }
}
