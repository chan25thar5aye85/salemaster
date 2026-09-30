package com.akari.retailer.features.inventory.domain.models

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val sku: String = "",
    val costPrice: Int = 0,
    val sellPrice: Int = 0,
    val stockQuantity: Int = 0,
    val minStockLevel: Int = 0,
    val supplierId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stockQuantity <= minStockLevel && stockQuantity > 0
    
    val isOutOfStock: Boolean
        get() = stockQuantity <= 0
    
    val stockStatus: StockStatus
        get() = when {
            isOutOfStock -> StockStatus.OUT_OF_STOCK
            isLowStock -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}

enum class StockStatus {
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}
