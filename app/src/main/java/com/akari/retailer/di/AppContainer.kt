package com.akari.retailer.di

import com.akari.retailer.data.remote.FirestoreService
import com.akari.retailer.data.repository.FirestoreSaleRepository
import com.akari.retailer.data.repository.SaleRepository

/**
 * Manual dependency injection container
 * No Hilt - simple constructor injection
 */
class AppContainer {
    
    // Singleton dependencies
    private val firestoreService by lazy { FirestoreService() }
    
    val saleRepository: SaleRepository by lazy { 
        FirestoreSaleRepository(firestoreService)
    }
}
