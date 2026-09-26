package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.NoParamUseCase
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import kotlinx.coroutines.flow.Flow

class GetOrdersUseCase(
    private val repository: PurchaseOrderRepository
) : NoParamUseCase<Flow<List<PurchaseOrder>>> {
    
    override suspend fun invoke(): Flow<List<PurchaseOrder>> {
        return repository.getOrders()
    }
}
