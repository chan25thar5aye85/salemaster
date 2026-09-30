package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.features.customer.data.repository.CreditRepository
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import kotlinx.coroutines.flow.Flow

class GetCreditTransactionsUseCase(
    private val repository: CreditRepository
) {
    fun all(): Flow<List<CreditTransaction>> = repository.getTransactions()

    fun forCustomer(customerId: String): Flow<List<CreditTransaction>> =
        repository.getTransactionsForCustomer(customerId)
}
