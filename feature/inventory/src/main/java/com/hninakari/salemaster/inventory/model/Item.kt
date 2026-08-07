package com.hninakari.salemaster.inventory.model

data class Item(
    val id: Long,
    val name: String,
    val quantity: Int,
    val costPrice: Double,
    val sellPrice: Double
)
