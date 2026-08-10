package com.hninakari.salemaster.inventory.domain.repository

import com.hninakari.salemaster.inventory.model.Product

interface ProductRepository {

    suspend fun getAll(): List<Product>

    suspend fun getById(id: Long): Product?

    suspend fun create(product: Product): Long

    suspend fun update(product: Product)

    suspend fun delete(id: Long)
}
