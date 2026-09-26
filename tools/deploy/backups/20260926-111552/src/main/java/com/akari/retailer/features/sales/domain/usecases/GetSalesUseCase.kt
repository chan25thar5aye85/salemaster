package com.akari.retailer.features.sales.domain.usecases

import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.core.usecases.NoParamUseCase
import kotlinx.coroutines.flow.Flow

class GetSalesUseCase(
    private val repository: SaleRepository
) : NoParamUseCase<Flow<List<Sale>>> {
    
    override suspend fun invoke(): Flow<List<Sale>> {
        return repository.getSales()
    }
}
