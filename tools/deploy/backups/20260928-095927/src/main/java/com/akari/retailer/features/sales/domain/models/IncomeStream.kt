package com.akari.retailer.features.sales.domain.models

import android.graphics.Color

enum class IncomeType {
    PRODUCT_SALES,
    SERVICE,
    RENT,
    INVESTMENT,
    FREELANCE,
    INTEREST,
    OTHER
}

/**
 * IncomeStream represents a user-created income source.
 *
 * Income streams are entirely user-defined. There are no seeded "defaults".
 */
data class IncomeStream(
    val id: String = "",
    val name: String = "",
    val type: IncomeType = IncomeType.PRODUCT_SALES,
    val icon: String = "💰",
    val color: String = "#4CAF50",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = "$icon $name"
    fun getColorInt(): Int = Color.parseColor(color)
}
