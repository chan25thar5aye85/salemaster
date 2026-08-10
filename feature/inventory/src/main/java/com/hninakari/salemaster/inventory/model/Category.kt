package com.hninakari.salemaster.inventory.model

data class Category(
    val id: Long,
    val name: String,
    val parentCategoryId: Long?,
    val isActive: Boolean
)
