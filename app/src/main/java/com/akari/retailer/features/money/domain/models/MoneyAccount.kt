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
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getDisplayName(): String = "$icon $name"
    fun getColorInt(): Int = Color.parseColor(color)
}

object DefaultMoneyAccounts {
    val list = listOf(
        MoneyAccount(
            id = "default_cash",
            name = "Cash",
            type = MoneyAccountType.CASH,
            icon = "💵",
            color = "#4CAF50",
            isDefault = true
        ),
        MoneyAccount(
            id = "default_kpay",
            name = "KPay",
            type = MoneyAccountType.MOBILE_WALLET,
            icon = "📱",
            color = "#2196F3",
            isDefault = true
        ),
        MoneyAccount(
            id = "default_wave",
            name = "Wave Money",
            type = MoneyAccountType.MOBILE_WALLET,
            icon = "🌊",
            color = "#FF9800",
            isDefault = true
        ),
        MoneyAccount(
            id = "default_bank",
            name = "Bank Account",
            type = MoneyAccountType.BANK,
            icon = "🏦",
            color = "#9C27B0",
            isDefault = true
        )
    )
}
