package com.akari.retailer.features.inventory.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CreatePurchaseOrderParams(
    val supplierId: String,
    val supplierName: String,
    val items: List<PurchaseOrderItem>,
    val notes: String = "",
    val createdBy: String = "default"
)

class CreatePurchaseOrderUseCase(
    private val repository: PurchaseOrderRepository
) : UseCase<CreatePurchaseOrderParams, Result<String>> {
    
    override suspend fun invoke(params: CreatePurchaseOrderParams): Result<String> {
        return try {
            // Validate
            if (params.supplierId.isEmpty()) {
                return Result.failure(Exception("Supplier is required"))
            }
            
            if (params.items.isEmpty()) {
                return Result.failure(Exception("At least one item is required"))
            }
            
            params.items.forEach { item ->
                if (item.productId.isEmpty()) {
                    return Result.failure(Exception("Product is required for all items"))
                }
                if (item.quantity <= 0) {
                    return Result.failure(Exception("Quantity must be greater than 0"))
                }
                if (item.costPrice <= 0) {
                    return Result.failure(Exception("Cost price must be greater than 0"))
                }
            }
            
            // Generate order number
            val orderNumber = generateOrderNumber()
            val totalCost = params.items.sumOf { it.total }
            
            // Create order with items (receivedQuantity = 0 initially)
            val orderItems = params.items.map { item ->
                item.copy(receivedQuantity = 0)
            }
            
            val order = PurchaseOrder(
                orderNumber = orderNumber,
                supplierId = params.supplierId,
                supplierName = params.supplierName,
                items = orderItems,
                status = PurchaseOrderStatus.DRAFT,
                totalCost = totalCost,
                receivedCost = 0,
                notes = params.notes,
                createdBy = params.createdBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            repository.createOrder(order)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateOrderNumber(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "PO-$date-$random"
    }
}
