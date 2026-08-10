package com.hninakari.salemaster.inventory.model

data class Supplier(
    val id: Long,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val note: String?,
    val isActive: Boolean
)
