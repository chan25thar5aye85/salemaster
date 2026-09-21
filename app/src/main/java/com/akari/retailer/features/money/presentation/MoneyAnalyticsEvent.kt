package com.akari.retailer.features.money.presentation

sealed class MoneyAnalyticsEvent {
    data object LoadAnalytics : MoneyAnalyticsEvent()
    data object RefreshAnalytics : MoneyAnalyticsEvent()
    data class TimeRangeChanged(val range: MoneyAnalyticsTimeRange) : MoneyAnalyticsEvent()
    data object ClearError : MoneyAnalyticsEvent()
}
