package com.akari.retailer.features.sales.domain.usecases

import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.core.usecases.UseCase

class DeleteSaleUseCase(
    private val repository: SaleRepository
) : UseCase<String, Result<Unit>> {
    
    override suspend fun invoke(params: String): Result<Unit> {
        return try {
            if (params.isEmpty()) {
                Result.failure(Exception("Sale ID cannot be empty"))
            } else {
                repository.deleteSale(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
