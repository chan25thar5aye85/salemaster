package com.akari.retailer.features.supplier.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.supplier.data.repository.SupplierRepository

class DeleteSupplierUseCase(
    private val repository: SupplierRepository
) : UseCase<String, Result<Unit>> {
    
    override suspend fun invoke(params: String): Result<Unit> {
        return try {
            if (params.isEmpty()) {
                Result.failure(Exception("Supplier ID cannot be empty"))
            } else {
                repository.deleteSupplier(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
