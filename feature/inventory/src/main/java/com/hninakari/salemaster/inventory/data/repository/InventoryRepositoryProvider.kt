package com.hninakari.salemaster.inventory.data.repository

import android.content.Context
import com.hninakari.salemaster.inventory.domain.repository.ProductRepository
import com.hninakari.salemaster.inventory.data.local.InventoryDatabaseProvider

object InventoryRepositoryProvider {

    fun productRepository(context: Context): ProductRepository {
        val database = InventoryDatabaseProvider.get(context)
        return ProductRepositoryImpl(database.productDao())
    }
}
