package com.akari.retailer.features.inventory.presentation

import com.akari.retailer.features.inventory.domain.models.Product

data class InventoryListState(
    val products: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val showLowStockOnly: Boolean = false,
    val totalProducts: Int = 0
)
