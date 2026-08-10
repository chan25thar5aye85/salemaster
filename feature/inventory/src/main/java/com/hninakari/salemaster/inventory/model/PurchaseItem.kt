package com.hninakari.salemaster.inventory.model

data class PurchaseItem(
    val id: Long,
    val purchaseId: Long,
    val productId: Long,
    val quantity: Int,
    val unitCost: Long
)
