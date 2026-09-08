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

data class IncomeStream(
    val id: String = "",
    val name: String = "",
    val type: IncomeType = IncomeType.PRODUCT_SALES,
    val icon: String = "💰",
    val color: String = "#4CAF50",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = "$icon $name"
    fun getColorInt(): Int = Color.parseColor(color)
}

object DefaultIncomeStreams {
    val list = listOf(
        IncomeStream(
            id = "default_product_sales",
            name = "Product Sales",
            type = IncomeType.PRODUCT_SALES,
            icon = "🛒",
            color = "#4CAF50",
            isDefault = true
        ),
        IncomeStream(
            id = "default_service",
            name = "Service Income",
            type = IncomeType.SERVICE,
            icon = "💼",
            color = "#2196F3",
            isDefault = true
        ),
        IncomeStream(
            id = "default_rent",
            name = "Rent Income",
            type = IncomeType.RENT,
            icon = "🏠",
            color = "#FF9800",
            isDefault = true
        ),
        IncomeStream(
            id = "default_investment",
            name = "Investment Income",
            type = IncomeType.INVESTMENT,
            icon = "📈",
            color = "#9C27B0",
            isDefault = true
        ),
        IncomeStream(
            id = "default_freelance",
            name = "Freelance Income",
            type = IncomeType.FREELANCE,
            icon = "✏️",
            color = "#00BCD4",
            isDefault = true
        ),
        IncomeStream(
            id = "default_interest",
            name = "Interest Income",
            type = IncomeType.INTEREST,
            icon = "🏦",
            color = "#FF5722",
            isDefault = true
        ),
        IncomeStream(
            id = "default_other_income",
            name = "Other Income",
            type = IncomeType.OTHER,
            icon = "📌",
            color = "#607D8B",
            isDefault = true
        )
    )
}
