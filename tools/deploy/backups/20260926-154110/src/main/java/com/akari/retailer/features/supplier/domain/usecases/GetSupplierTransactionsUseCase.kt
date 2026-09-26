package com.akari.retailer.features.supplier.domain.usecases

import com.akari.retailer.features.supplier.data.repository.SupplierCreditRepository
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import kotlinx.coroutines.flow.Flow

class GetSupplierTransactionsUseCase(
    private val repository: SupplierCreditRepository
) {
    fun all(): Flow<List<SupplierTransaction>> = repository.getTransactions()

    fun forSupplier(supplierId: String): Flow<List<SupplierTransaction>> =
        repository.getTransactionsForSupplier(supplierId)
}
