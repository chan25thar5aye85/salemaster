package com.akari.retailer.features.supplier.data.repository

import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import kotlinx.coroutines.flow.Flow

interface SupplierCreditRepository {
    suspend fun recordPurchaseOnCredit(
        supplierId: String,
        amount: Int,
        purchaseId: String,
        description: String = ""
    ): Result<String>

    suspend fun recordPayment(
        supplierId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String>

    suspend fun recordRefundReceived(
        supplierId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String>

    fun getTransactions(): Flow<List<SupplierTransaction>>
    fun getTransactionsForSupplier(supplierId: String): Flow<List<SupplierTransaction>>
}
