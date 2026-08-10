package com.hninakari.salemaster.inventory.model

data class Product(
    val id: Long,
    val name: String,
    val sku: String?,
    val barcode: String?,
    val sellPrice: Long,
    val quantity: Int,
    val categoryId: Long?,
    val isActive: Boolean
)
