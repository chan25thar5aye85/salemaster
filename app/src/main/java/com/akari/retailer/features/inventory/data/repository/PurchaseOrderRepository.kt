package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import kotlinx.coroutines.flow.Flow

interface PurchaseOrderRepository {
    suspend fun createOrder(order: PurchaseOrder): Result<String>
    suspend fun updateOrder(order: PurchaseOrder): Result<Unit>
    fun getOrder(orderId: String): Flow<PurchaseOrder?>
    suspend fun getOrderSync(orderId: String): PurchaseOrder?
    fun getOrders(): Flow<List<PurchaseOrder>>
    fun getOrdersByStatus(status: PurchaseOrderStatus): Flow<List<PurchaseOrder>>
    fun getOrdersBySupplier(supplierId: String): Flow<List<PurchaseOrder>>
    suspend fun deleteOrder(orderId: String): Result<Unit>
    suspend fun updateStatus(orderId: String, newStatus: PurchaseOrderStatus): Result<Unit>
}
