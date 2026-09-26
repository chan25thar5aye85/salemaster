package com.akari.retailer.features.money.presentation

import com.akari.retailer.core.ui.components.TimeFilter

sealed class MoneyAnalyticsEvent {
    data object LoadAnalytics : MoneyAnalyticsEvent()
    data object RefreshAnalytics : MoneyAnalyticsEvent()
    data class TimeFilterChanged(val filter: TimeFilter) : MoneyAnalyticsEvent()
    data object ClearError : MoneyAnalyticsEvent()
}
