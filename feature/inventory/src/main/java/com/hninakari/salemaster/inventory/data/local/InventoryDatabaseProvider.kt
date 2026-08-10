package com.hninakari.salemaster.inventory.data.local

import android.content.Context
import androidx.room.Room

object InventoryDatabaseProvider {

    @Volatile
    private var instance: InventoryDatabase? = null

    fun get(context: Context): InventoryDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                InventoryDatabase::class.java,
                "inventory.db"
            ).build().also {
                instance = it
            }
        }
    }
}
