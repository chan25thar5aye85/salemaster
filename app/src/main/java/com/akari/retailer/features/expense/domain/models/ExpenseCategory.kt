package com.akari.retailer.features.expense.domain.models

import android.graphics.Color

/**
 * ExpenseCategory represents a category for expenses.
 * Supports both default system categories and user-defined custom categories.
 */
data class ExpenseCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "📌",
    val color: String = "#636E72",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Get display name with icon
     */
    fun getDisplayName(): String = "$icon $name"
    
    /**
     * Get Android Color int from hex string
     */
    fun getColorInt(): Int {
        return try {
            Color.parseColor(color)
        } catch (e: Exception) {
            Color.parseColor("#636E72")
        }
    }
    
    /**
     * Check if this is a valid category
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && name.length >= 2
    }
}

/**
 * Default system categories that come pre-installed
 */
object DefaultCategories {
    val list = listOf(
        ExpenseCategory(
            id = "default_utilities",
            name = "Utilities",
            icon = "💡",
            color = "#4ECDC4",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_rent",
            name = "Rent",
            icon = "🏠",
            color = "#FF6B6B",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_salary",
            name = "Salary",
            icon = "👨‍💼",
            color = "#96CEB4",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_inventory",
            name = "Inventory",
            icon = "📦",
            color = "#FF9F43",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_marketing",
            name = "Marketing",
            icon = "📢",
            color = "#FDCB6E",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_travel",
            name = "Travel",
            icon = "✈️",
            color = "#6C5CE7",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_meals",
            name = "Meals",
            icon = "🍕",
            color = "#FD79A8",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_supplies",
            name = "Supplies",
            icon = "📎",
            color = "#A29BFE",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_maintenance",
            name = "Maintenance",
            icon = "🔧",
            color = "#45B7D1",
            isDefault = true
        ),
        ExpenseCategory(
            id = "default_other",
            name = "Other",
            icon = "📌",
            color = "#636E72",
            isDefault = true
        )
    )
    
    // Map for quick lookup by ID
    val map: Map<String, ExpenseCategory> = list.associateBy { it.id }
}
