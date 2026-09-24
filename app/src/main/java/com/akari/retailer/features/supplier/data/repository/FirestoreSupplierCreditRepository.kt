package com.akari.retailer.features.supplier.data.repository

import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierCreditService
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import kotlinx.coroutines.flow.Flow

class FirestoreSupplierCreditRepository(
    private val service: FirestoreSupplierCreditService
) : SupplierCreditRepository {

    override suspend fun recordPurchaseOnCredit(
        supplierId: String,
        amount: Int,
        purchaseId: String,
        description: String
    ): Result<String> = service.recordPurchaseOnCredit(supplierId, amount, purchaseId, description)

    override suspend fun recordPayment(
        supplierId: String,
        amount: Int,
        paymentAccountId: String,
        description: String
    ): Result<String> = service.recordPayment(supplierId, amount, paymentAccountId, description)

    override suspend fun recordRefundReceived(
        supplierId: String,
        amount: Int,
        paymentAccountId: String,
        description: String
    ): Result<String> = service.recordRefundReceived(supplierId, amount, paymentAccountId, description)

    override fun getTransactions(): Flow<List<SupplierTransaction>> = service.getTransactions()

    override fun getTransactionsForSupplier(supplierId: String): Flow<List<SupplierTransaction>> =
        service.getTransactionsForSupplier(supplierId)
}
