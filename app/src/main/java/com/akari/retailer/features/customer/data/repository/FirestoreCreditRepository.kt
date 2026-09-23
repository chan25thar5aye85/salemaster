package com.akari.retailer.features.customer.data.repository

import com.akari.retailer.features.customer.data.remote.FirestoreCreditService
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import kotlinx.coroutines.flow.Flow

class FirestoreCreditRepository(
    private val service: FirestoreCreditService
) : CreditRepository {

    override suspend fun extendCredit(
        customerId: String,
        amount: Int,
        saleId: String,
        description: String
    ): Result<String> = service.extendCredit(customerId, amount, saleId, description)

    override suspend fun recordPayment(
        customerId: String,
        amount: Int,
        paymentAccountId: String,
        description: String
    ): Result<String> = service.recordPayment(customerId, amount, paymentAccountId, description)

    override fun getTransactions(): Flow<List<CreditTransaction>> = service.getTransactions()

    override fun getTransactionsForCustomer(customerId: String): Flow<List<CreditTransaction>> =
        service.getTransactionsForCustomer(customerId)
}
