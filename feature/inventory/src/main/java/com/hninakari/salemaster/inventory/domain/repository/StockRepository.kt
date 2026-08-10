package com.hninakari.salemaster.inventory.domain.repository

interface StockRepository {

    suspend fun getQuantity(productId: Long): Int

    suspend fun increase(productId: Long, quantity: Int)

    suspend fun decrease(productId: Long, quantity: Int)
}
