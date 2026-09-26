package com.akari.retailer.features.inventory.presentation

sealed class InventoryListEvent {
    data object LoadProducts : InventoryListEvent()
    data object RefreshProducts : InventoryListEvent()
    data class DeleteProduct(val productId: String) : InventoryListEvent()
    data object ClearError : InventoryListEvent()
    data class SearchQueryChanged(val query: String) : InventoryListEvent()
    data object ClearSearch : InventoryListEvent()
    data object ToggleLowStockFilter : InventoryListEvent()
}
