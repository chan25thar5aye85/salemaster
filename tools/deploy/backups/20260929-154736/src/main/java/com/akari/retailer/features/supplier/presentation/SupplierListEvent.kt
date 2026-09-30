package com.akari.retailer.features.supplier.presentation

sealed class SupplierListEvent {
    data object LoadSuppliers : SupplierListEvent()
    data object RefreshSuppliers : SupplierListEvent()
    data class DeleteSupplier(val supplierId: String) : SupplierListEvent()
    data object ClearError : SupplierListEvent()
    data class SearchQueryChanged(val query: String) : SupplierListEvent()
    data object ClearSearch : SupplierListEvent()
}
