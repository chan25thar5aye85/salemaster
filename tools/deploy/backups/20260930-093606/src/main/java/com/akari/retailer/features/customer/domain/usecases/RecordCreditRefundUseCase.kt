package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.features.customer.data.repository.CreditRepository

class RecordCreditRefundUseCase(
    private val repository: CreditRepository
) {
    suspend fun invoke(
        customerId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String> {
        if (customerId.isEmpty()) return Result.failure(Exception("Customer is required"))
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than 0"))
        if (paymentAccountId.isEmpty()) return Result.failure(Exception("Refund account is required"))
        return repository.recordRefund(customerId, amount, paymentAccountId, description)
    }
}
