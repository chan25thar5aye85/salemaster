package com.akari.retailer.features.supplier.domain.usecases

import com.akari.retailer.features.supplier.data.repository.SupplierCreditRepository

class RecordSupplierRefundReceivedUseCase(
    private val repository: SupplierCreditRepository
) {
    suspend fun invoke(
        supplierId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String> {
        if (supplierId.isEmpty()) return Result.failure(Exception("Supplier is required"))
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than 0"))
        if (paymentAccountId.isEmpty()) return Result.failure(Exception("Account is required"))
        return repository.recordRefundReceived(supplierId, amount, paymentAccountId, description)
    }
}
