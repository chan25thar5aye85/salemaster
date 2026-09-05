package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.inventory.domain.models.StockMovement
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PurchaseOrderListState(
    val orders: List<PurchaseOrder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class PurchaseOrderListEvent {
    data object LoadOrders : PurchaseOrderListEvent()
    data object ClearError : PurchaseOrderListEvent()
}

class PurchaseOrderListViewModel(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseOrderListState())
    val state: StateFlow<PurchaseOrderListState> = _state.asStateFlow()

    init {
        loadOrders()
    }

    fun handleEvent(event: PurchaseOrderListEvent) {
        when (event) {
            PurchaseOrderListEvent.LoadOrders -> loadOrders()
            PurchaseOrderListEvent.ClearError -> _state.value = _state.value.copy(error = null)
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // Get all stock movements of type PURCHASE
                stockRepository.getAllMovements().collect { movements ->
                    val purchaseMovements = movements.filter { it.type == com.akari.retailer.features.inventory.domain.models.MovementType.PURCHASE }
                    
                    // Convert movements to PurchaseOrder objects
                    val orders = purchaseMovements.map { movement ->
                        val item = PurchaseOrderItem(
                            productId = movement.productId,
                            quantity = movement.quantity,
                            costPrice = movement.previousStock,
                            total = movement.quantity * movement.previousStock
                        )
                        
                        PurchaseOrder(
                            id = movement.id,
                            supplierId = "",
                            supplierName = "Supplier",
                            items = listOf(item),
                            totalCost = movement.quantity * movement.previousStock,
                            status = PurchaseOrderStatus.RECEIVED,
                            orderedAt = movement.createdAt,
                            notes = movement.reason
                        )
                    }
                    
                    _state.value = _state.value.copy(
                        orders = orders,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load purchase orders"
                )
            }
        }
    }
}

class PurchaseOrderListViewModelFactory(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository,
    private val supplierRepository: SupplierRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderListViewModel::class.java)) {
            return PurchaseOrderListViewModel(inventoryRepository, stockRepository, supplierRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
