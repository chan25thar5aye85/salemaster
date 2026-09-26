package com.akari.retailer.features.customer.data.repository

import com.akari.retailer.features.customer.domain.models.CreditTransaction
import kotlinx.coroutines.flow.Flow

interface CreditRepository {
    suspend fun extendCredit(
        customerId: String,
        amount: Int,
        saleId: String,
        description: String = ""
    ): Result<String>

    suspend fun recordPayment(
        customerId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String>

    suspend fun recordRefund(
        customerId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String>

    fun getTransactions(): Flow<List<CreditTransaction>>
    fun getTransactionsForCustomer(customerId: String): Flow<List<CreditTransaction>>
}
