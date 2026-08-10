package com.hninakari.salemaster.inventory.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val sku: String?,
    val barcode: String?,
    val sellPrice: Long,
    val categoryId: Long?,
    val isActive: Boolean
)
