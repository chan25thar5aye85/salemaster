package com.hninakari.salemaster.inventory.model

data class SaleItem(
    val id: Long,
    val saleId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Long
)
