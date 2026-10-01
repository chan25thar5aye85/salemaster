package com.akari.retailer.features.supplier.domain.usecases

import com.akari.retailer.features.supplier.data.repository.SupplierCreditRepository

/**
 * Manually increase what you owe a supplier — the "I bought something on
 * credit from this supplier" case, when it wasn't created through the full
 * Purchase Order flow.
 */
class AddSupplierPayableUseCase(
    private val repository: SupplierCreditRepository
) {
    suspend fun invoke(
        supplierId: String,
        amount: Int,
        description: String = ""
    ): Result<String> {
        if (supplierId.isEmpty()) return Result.failure(Exception("Supplier is required"))
        if (amount <= 0) return Result.failure(Exception("Amount must be greater than 0"))
        return repository.recordPurchaseOnCredit(
            supplierId = supplierId,
            amount = amount,
            purchaseId = "",
            description = description
        )
    }
}
