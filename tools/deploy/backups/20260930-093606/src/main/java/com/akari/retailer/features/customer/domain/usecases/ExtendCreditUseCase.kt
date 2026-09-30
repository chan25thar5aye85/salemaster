package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.features.customer.data.repository.CreditRepository

class ExtendCreditUseCase(
    private val repository: CreditRepository
) {
    suspend fun invoke(
        customerId: String,
        amount: Int,
        saleId: String,
        description: String = ""
    ): Result<String> {
        if (customerId.isEmpty()) return Result.failure(Exception("Customer is required"))
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than 0"))
        return repository.extendCredit(customerId, amount, saleId, description)
    }
}
