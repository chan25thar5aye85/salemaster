package com.akari.retailer.features.sales.presentation.history

sealed class SaleHistoryEvent {
    data object LoadSales : SaleHistoryEvent()
    data object RefreshSales : SaleHistoryEvent()
    data class DeleteSale(val saleId: String) : SaleHistoryEvent()
    data object ClearError : SaleHistoryEvent()
    data object ResetDeleteSuccess : SaleHistoryEvent()
    data class FilterByDate(val timestamp: Long) : SaleHistoryEvent()
    data object ClearDateFilter : SaleHistoryEvent()
}
