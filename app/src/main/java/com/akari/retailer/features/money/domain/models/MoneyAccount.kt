package com.akari.retailer.features.money.domain.models

import android.graphics.Color

enum class MoneyAccountType {
    CASH,
    MOBILE_WALLET,
    BANK,
    CREDIT,
    OTHER
}

data class MoneyAccount(
    val id: String = "",
    val name: String = "",
    val type: MoneyAccountType = MoneyAccountType.CASH,
    val icon: String = "💵",
    val color: String = "#4CAF50",
    val openingBalance: Int = 0,
    val currentBalance: Int = 0,
    val accountNumber: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = "$icon $name"
    fun getColorInt(): Int = Color.parseColor(color)
}

/**
 * Single starter account seeded on first launch.
 * Users can rename it, change icon/color, and add more accounts.
 */
object DefaultMoneyAccounts {
    val list = listOf(
        MoneyAccount(
            id = "default_cash",
            name = "Cash",
            type = MoneyAccountType.CASH,
            icon = "💵",
            color = "#4CAF50"
        )
    )
}
