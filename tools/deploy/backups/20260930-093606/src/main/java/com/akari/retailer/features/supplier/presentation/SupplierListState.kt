package com.akari.retailer.features.supplier.presentation

import com.akari.retailer.features.supplier.domain.models.Supplier

data class SupplierListState(
    val suppliers: List<Supplier> = emptyList(),
    val allSuppliers: List<Supplier> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)
