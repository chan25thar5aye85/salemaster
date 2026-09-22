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
import com.akari.retailer.features.expense.domain.usecases.CalculateBusinessProfitUseCase
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseRepository
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.data.remote.FirestoreStockService
import com.akari.retailer.features.inventory.data.repository.FirestoreStockRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.money.data.remote.FirestoreMoneyService
import com.akari.retailer.features.money.data.remote.FirestoreMoneyTransactionService
import com.akari.retailer.features.money.data.repository.FirestoreMoneyAccountRepository
import com.akari.retailer.features.money.data.repository.FirestoreMoneyTransactionRepository
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.usecases.ExternalTransferUseCase
import com.akari.retailer.features.money.domain.usecases.ProcessMoneyTransactionUseCase
import com.akari.retailer.features.money.domain.usecases.TransferMoneyUseCase
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
    
    // Purchases
    val purchaseRepository: PurchaseRepository by lazy {
        FirestorePurchaseRepository()
    }
    
    // Stock Movements
    private val stockService by lazy { FirestoreStockService() }
    val stockRepository: StockRepository by lazy {
        FirestoreStockRepository(stockService)
    }
    
    // Money Accounts
    private val moneyService by lazy { FirestoreMoneyService() }
    val moneyAccountRepository: MoneyAccountRepository by lazy {
        FirestoreMoneyAccountRepository(moneyService)
    }
    
    // Money Transactions
    private val moneyTransactionService by lazy { FirestoreMoneyTransactionService() }
    val moneyTransactionRepository: MoneyTransactionRepository by lazy {
        FirestoreMoneyTransactionRepository(moneyTransactionService)
    }
    
    // Process Money Transaction
    val processMoneyTransactionUseCase by lazy {
        ProcessMoneyTransactionUseCase(
            moneyAccountRepository,
            moneyTransactionRepository
        )
    }
    
    // Transfer Money
    val transferMoneyUseCase by lazy {
        TransferMoneyUseCase(moneyAccountRepository, moneyTransactionRepository)
    }
    
    // External Transfer
    val externalTransferUseCase by lazy {
        ExternalTransferUseCase(moneyAccountRepository, moneyTransactionRepository)
    }
    
    // Profit & Loss (INCLUDES FEES NOW)
    val calculateProfitUseCase by lazy {
        CalculateBusinessProfitUseCase(
            saleRepository,
            expenseRepository,
            incomeEntryRepository,
            moneyTransactionRepository  // ✅ NEW
        )
    }
}
