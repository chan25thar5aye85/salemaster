package com.akari.retailer.features.supplier.domain.models

data class Supplier(
    val id: String = "",
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val products: List<String> = emptyList(),
    val totalPurchased: Int = 0,
    val lastOrderDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
