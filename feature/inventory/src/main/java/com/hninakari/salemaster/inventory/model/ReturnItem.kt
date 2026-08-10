package com.hninakari.salemaster.inventory.model

data class ReturnItem(
    val id: Long,
    val returnId: Long,
    val productId: Long,
    val quantity: Int
)
