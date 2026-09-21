package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.MoneyAccount

enum class MoneyAnalyticsTimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    ALL
}

data class AccountBalance(
    val account: MoneyAccount,
    val balance: Int,
    val percentage: Double
)

data class FeeSummary(
    val totalPaid: Int = 0,
    val totalEarned: Int = 0,
    val netFee: Int = 0,
    val feesByType: Map<String, Int> = emptyMap()
)

data class MoneyFlow(
    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val netFlow: Int = 0,
    val transactionCount: Int = 0
)

data class MoneyAnalyticsState(
    val accounts: List<MoneyAccount> = emptyList(),
    val accountBalances: List<AccountBalance> = emptyList(),
    val totalBalance: Int = 0,
    val feeSummary: FeeSummary = FeeSummary(),
    val moneyFlow: MoneyFlow = MoneyFlow(),
    val timeRange: MoneyAnalyticsTimeRange = MoneyAnalyticsTimeRange.THIS_MONTH,
    val isLoading: Boolean = true,
    val error: String? = null,
    val rangeLabel: String = "This Month"
)
