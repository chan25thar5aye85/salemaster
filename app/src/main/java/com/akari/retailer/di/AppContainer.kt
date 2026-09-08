package com.akari.retailer.di

import com.akari.retailer.data.remote.FirestoreService
import com.akari.retailer.data.repository.FirestoreSaleRepository
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.customer.data.remote.FirestoreCustomerService
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.data.repository.FirestoreCustomerRepository
import com.akari.retailer.features.expense.data.remote.FirestoreCategoryService
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.data.repository.FirestoreCategoryRepository
import com.akari.retailer.features.expense.data.repository.FirestoreExpenseRepository
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseRepository
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeEntryService
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeStreamService
import com.akari.retailer.features.sales.data.repository.FirestoreIncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.FirestoreIncomeStreamRepository
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.repository.SupplierRepository

class AppContainer {
    
    // Sales
    private val firestoreService by lazy { FirestoreService() }
    val saleRepository: SaleRepository by lazy { 
        FirestoreSaleRepository(firestoreService)
    }
    
    // Income Streams
    private val incomeStreamService by lazy { FirestoreIncomeStreamService() }
    val incomeStreamRepository: IncomeStreamRepository by lazy {
        FirestoreIncomeStreamRepository(incomeStreamService)
    }
    
    // Income Entries
    private val incomeEntryService by lazy { FirestoreIncomeEntryService() }
    val incomeEntryRepository: IncomeEntryRepository by lazy {
        FirestoreIncomeEntryRepository(incomeEntryService)
    }
    
    // Customers
    private val customerFirestoreService by lazy { FirestoreCustomerService() }
    val customerRepository: CustomerRepository by lazy {
        FirestoreCustomerRepository(customerFirestoreService)
    }
    
    // Expenses
    private val expenseFirestoreService by lazy { FirestoreExpenseService() }
    val expenseRepository: ExpenseRepository by lazy {
        FirestoreExpenseRepository(expenseFirestoreService)
    }
    
    // Categories
    private val categoryFirestoreService by lazy { FirestoreCategoryService() }
    val categoryRepository: CategoryRepository by lazy {
        FirestoreCategoryRepository(categoryFirestoreService)
    }
    
    // Suppliers
    private val supplierFirestoreService by lazy { FirestoreSupplierService() }
    val supplierRepository: SupplierRepository by lazy {
        FirestoreSupplierRepository(supplierFirestoreService)
    }
    
    // Inventory
    private val inventoryFirestoreService by lazy { FirestoreInventoryService() }
    val inventoryRepository: InventoryRepository by lazy {
        FirestoreInventoryRepository(inventoryFirestoreService)
    }
    
    // Purchase Orders
    val purchaseOrderRepository: PurchaseOrderRepository by lazy {
        FirestorePurchaseOrderRepository()
    }
    
    // Purchases (Completed purchases)
    val purchaseRepository: PurchaseRepository by lazy {
        FirestorePurchaseRepository()
    }
}
