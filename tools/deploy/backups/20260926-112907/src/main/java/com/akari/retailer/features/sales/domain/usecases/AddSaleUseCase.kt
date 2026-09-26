package com.akari.retailer.features.sales.domain.usecases

import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.core.usecases.UseCase

class AddSaleUseCase(
    private val repository: SaleRepository
) : UseCase<Sale, Result<String>> {
    
    override suspend fun invoke(params: Sale): Result<String> {
        return try {
            if (params.items.isEmpty()) {
                Result.failure(Exception("Sale must have at least one item"))
            } else {
                repository.saveSale(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
