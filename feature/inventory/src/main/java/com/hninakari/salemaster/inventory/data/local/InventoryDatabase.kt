package com.hninakari.salemaster.inventory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hninakari.salemaster.inventory.data.local.dao.ProductDao
import com.hninakari.salemaster.inventory.data.local.entity.ProductEntity

@Database(
    entities = [
        ProductEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
}
