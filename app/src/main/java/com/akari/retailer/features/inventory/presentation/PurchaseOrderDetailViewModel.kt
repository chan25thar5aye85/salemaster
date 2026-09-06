package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PurchaseOrderDetailViewModel(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository,
    private val sharedViewModel: PurchaseOrderSharedViewModel
) : ViewModel() {

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()
    
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    suspend fun deleteOrder(order: PurchaseOrder): Result<Unit> {
        return try {
            _isDeleting.value = true
            _deleteError.value = null
            
            // For each item, reduce stock
            order.items.forEach { item ->
                // Get current product
                val productResult = inventoryRepository.getProductByIdSync(item.productId)
                if (productResult.isSuccess) {
                    val product = productResult.getOrNull()
                    if (product != null) {
                        val newStock = product.stockQuantity - item.quantity
                        if (newStock >= 0) {
                            val updatedProduct = product.copy(
                                stockQuantity = newStock,
                                updatedAt = System.currentTimeMillis()
                            )
                            inventoryRepository.updateProduct(updatedProduct)
                        }
                    }
                }
            }
            
            // Delete stock movements (each item has a movement with the order ID)
            order.items.forEach { item ->
                // Find and delete the movement
                val movements = stockRepository.getMovementsForProductSync(item.productId)
                if (movements.isSuccess) {
                    movements.getOrNull()?.find { it.id == order.id }?.let { movement ->
                        stockRepository.addMovement(
                            movement.copy(
                                quantity = 0,
                                reason = "Deleted: ${movement.reason}"
                            )
                        )
                        // Note: We can't actually delete from Firestore easily with current setup
                        // We mark it as deleted by setting quantity to 0
                    }
                }
            }
            
            // Remove from shared ViewModel
            sharedViewModel.clear()
            
            _isDeleting.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            _isDeleting.value = false
            _deleteError.value = e.message
            Result.failure(e)
        }
    }
}
