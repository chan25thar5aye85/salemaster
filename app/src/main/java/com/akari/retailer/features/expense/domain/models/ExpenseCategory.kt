package com.akari.retailer.features.expense.domain.models

import android.graphics.Color

/**
 * ExpenseCategory represents a user-created category for expenses.
 *
 * Categories are entirely user-defined. There are no seeded "default"
 * categories. Every category can be renamed or deleted freely.
 */
data class ExpenseCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "📌",
    val color: String = "#636E72",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = "$icon $name"

    fun getColorInt(): Int {
        return try {
            Color.parseColor(color)
        } catch (e: Exception) {
            Color.parseColor("#636E72")
        }
    }

    fun isValid(): Boolean {
        return name.isNotBlank() && name.length >= 2
    }
}
