package com.akari.retailer.di

import com.akari.retailer.data.remote.FirestoreService
import com.akari.retailer.data.repository.FirestoreSaleRepository
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.data.repository.FirestoreExpenseRepository
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseRepository
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService

class AppContainer {
    
    private val firestoreService by lazy { FirestoreService() }
    
    val saleRepository: SaleRepository by lazy { 
        FirestoreSaleRepository(firestoreService)
    }
    
    private val expenseFirestoreService by lazy { FirestoreExpenseService() }
    
    val expenseRepository: ExpenseRepository by lazy {
        FirestoreExpenseRepository(expenseFirestoreService)
    }
    
    private val inventoryFirestoreService by lazy { FirestoreInventoryService() }
    
    val inventoryRepository: InventoryRepository by lazy {
        FirestoreInventoryRepository(inventoryFirestoreService)
    }
    
    // Purchase Order Repository
    val purchaseOrderRepository: PurchaseOrderRepository by lazy {
        FirestorePurchaseOrderRepository()
    }
    
    // Purchase Repository (Completed purchases)
    val purchaseRepository: PurchaseRepository by lazy {
        FirestorePurchaseRepository()
    }
}
