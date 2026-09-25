package com.akari.retailer.features.sales.presentation.history

import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.sales.domain.models.Sale

data class SaleHistoryState(
    val sales: List<Sale> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val timeFilter: TimeFilter = TimeFilter()
)
