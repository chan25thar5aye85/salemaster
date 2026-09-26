package com.akari.retailer.features.sales.presentation.history

import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.core.ui.components.TimeFilterPreset
import com.akari.retailer.features.sales.domain.models.Sale

data class SaleHistoryState(
    val sales: List<Sale> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.TODAY),

    // Summary totals (computed from the filtered list)
    val totalSales: Int = 0,
    val salesCount: Int = 0,
    val totalPaid: Int = 0,
    val totalCredit: Int = 0,

    /**
     * Money-in totals per money account id.
     * Excludes credit rows. Only accounts with a non-zero total are kept.
     */
    val paymentBreakdown: Map<String, Int> = emptyMap()
)
