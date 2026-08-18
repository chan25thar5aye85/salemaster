package com.hninakari.saletracker.sales.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SaleEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SalesDatabase : RoomDatabase() {
    abstract fun saleDao(): SaleDao
    
    companion object {
        @Volatile
        private var INSTANCE: SalesDatabase? = null
        
        fun getDatabase(context: Context): SalesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalesDatabase::class.java,
                    "sales_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
