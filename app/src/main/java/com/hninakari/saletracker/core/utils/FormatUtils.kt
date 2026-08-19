package com.hninakari.saletracker.core.utils

/**
 * Format amount without currency symbol
 * - Shows 2 decimal places if needed
 * - Trims .00 to show whole numbers
 * - No currency symbol
 */
fun formatAmount(amount: Double): String {
    return if (amount % 1 == 0.0) {
        // Whole number - no decimals
        amount.toInt().toString()
    } else {
        // Has decimals - show 2 decimal places
        String.format("%.2f", amount)
    }
}

// Keep old function for compatibility
fun formatCurrency(amount: Double): String {
    return formatAmount(amount)
}
