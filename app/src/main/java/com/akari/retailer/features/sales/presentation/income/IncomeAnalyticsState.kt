package com.akari.retailer.features.sales.presentation.income

import com.akari.retailer.features.sales.domain.models.IncomeStream

enum class IncomeTimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}

enum class IncomeTypeFilter {
    ALL,
    BUSINESS,
    PERSONAL
}

data class IncomeStreamSpending(
    val stream: IncomeStream,
    val totalIncome: Int,
    val count: Int,
    val percentage: Double
)

data class IncomeAnalyticsState(
    val totalIncome: Int = 0,
    val businessIncome: Int = 0,
    val personalIncome: Int = 0,
    val streamSpending: List<IncomeStreamSpending> = emptyList(),
    val topStream: IncomeStreamSpending? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedMonth: Int = 0,
    val selectedYear: Int = 0,
    val timeRange: IncomeTimeRange = IncomeTimeRange.TODAY,
    val typeFilter: IncomeTypeFilter = IncomeTypeFilter.ALL,
    val rangeLabel: String = "Today"
)
