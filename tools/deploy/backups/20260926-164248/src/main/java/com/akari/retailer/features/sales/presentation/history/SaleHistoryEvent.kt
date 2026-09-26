package com.akari.retailer.features.sales.presentation.history

import com.akari.retailer.core.ui.components.TimeFilter

sealed class SaleHistoryEvent {
    data object LoadSales : SaleHistoryEvent()
    data object RefreshSales : SaleHistoryEvent()
    data class DeleteSale(val saleId: String) : SaleHistoryEvent()
    data object ClearError : SaleHistoryEvent()
    data object ResetDeleteSuccess : SaleHistoryEvent()
    data class TimeFilterChanged(val filter: TimeFilter) : SaleHistoryEvent()
}
