package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.features.customer.data.repository.CreditRepository

class RecordCreditPaymentUseCase(
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
        if (paymentAccountId.isEmpty()) return Result.failure(Exception("Payment account is required"))
        return repository.recordPayment(customerId, amount, paymentAccountId, description)
    }
}
