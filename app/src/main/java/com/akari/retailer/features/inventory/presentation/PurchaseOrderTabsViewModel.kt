package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PurchaseOrderTabsState(
    val orders: List<PurchaseOrder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class PurchaseOrderTabsEvent {
    data object LoadOrders : PurchaseOrderTabsEvent()
    data object ClearError : PurchaseOrderTabsEvent()
}

class PurchaseOrderTabsViewModel(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository,
    private val supplierRepository: SupplierRepository,
    private val sharedViewModel: PurchaseOrderSharedViewModel
) : ViewModel() {

    private val _state = MutableStateFlow(PurchaseOrderTabsState())
    val state: StateFlow<PurchaseOrderTabsState> = _state.asStateFlow()

    init {
        loadOrders()
    }

    fun handleEvent(event: PurchaseOrderTabsEvent) {
        when (event) {
            PurchaseOrderTabsEvent.LoadOrders -> loadOrders()
            PurchaseOrderTabsEvent.ClearError -> _state.value = _state.value.copy(error = null)
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                var supplierMap = mapOf<String, String>()
                try {
                    val suppliers = supplierRepository.getSuppliers().first()
                    supplierMap = suppliers.associate { it.id to it.name }
                } catch (e: Exception) {
                    // Suppliers failed
                }
                
                val movements = stockRepository.getAllMovements().first()
                val purchaseMovements = movements.filter { it.type == com.akari.retailer.features.inventory.domain.models.MovementType.PURCHASE }
                
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
                        supplierName = supplierMap[movement.userId] ?: "Supplier",
                        items = listOf(item),
                        status = PurchaseOrderStatus.RECEIVED,
                        orderDate = movement.createdAt,
                        notes = movement.reason
                    )
                }
                
                // Store in shared ViewModel so detail screen can access
                sharedViewModel.setOrders(orders)
                
                _state.value = _state.value.copy(
                    orders = orders,
                    isLoading = false,
                    error = null
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    orders = emptyList(),
                    isLoading = false,
                    error = "Failed to load data: ${e.message}"
                )
            }
        }
    }
}

class PurchaseOrderTabsViewModelFactory(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository,
    private val supplierRepository: SupplierRepository,
    private val sharedViewModel: PurchaseOrderSharedViewModel
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderTabsViewModel::class.java)) {
            return PurchaseOrderTabsViewModel(inventoryRepository, stockRepository, supplierRepository, sharedViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
