package com.akari.retailer.core.utils

object MoneyFormatter {
    
    fun format(amount: Int): String {
        return amount.toString()
    }
    
    fun formatForInput(amount: Int): String {
        return amount.toString()
    }
    
    fun parse(input: String): Int {
        if (input.isEmpty()) return 0
        return try {
            input.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    fun formatTotal(amount: Int): String {
        return amount.toString()
    }
}
