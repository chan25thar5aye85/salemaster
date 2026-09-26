package com.akari.retailer.features.inventory.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.domain.models.Product
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PurchaseOrderState(
    val suppliers: List<Supplier> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedSupplier: Supplier? = null,
    val orderName: String = "",
    val expectedDeliveryDate: Long = 0,
    val tempItems: List<PurchaseOrderItem> = emptyList(),
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class PurchaseOrderEvent {
    data class SupplierSelected(val supplier: Supplier) : PurchaseOrderEvent()
    data class OrderNameChanged(val value: String) : PurchaseOrderEvent()
    data class ExpectedDeliveryDateChanged(val timestamp: Long) : PurchaseOrderEvent()
    data object AddItem : PurchaseOrderEvent()
    data class UpdateItem(
        val index: Int,
        val productId: String,
        val productName: String,
        val quantity: Int,
        val costPrice: Int
    ) : PurchaseOrderEvent()
    data class RemoveItem(val index: Int) : PurchaseOrderEvent()
    data class NotesChanged(val value: String) : PurchaseOrderEvent()
    data object SavePurchase : PurchaseOrderEvent()
    data object ClearError : PurchaseOrderEvent()
    data object ResetSuccess : PurchaseOrderEvent()
}

class PurchaseOrderViewModel(
    private val inventoryRepository: InventoryRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val TAG = "PurchaseOrderVM"
    private val _state = MutableStateFlow(PurchaseOrderState())
    val state: StateFlow<PurchaseOrderState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun handleEvent(event: PurchaseOrderEvent) {
        when (event) {
            is PurchaseOrderEvent.SupplierSelected -> {
                _state.value = _state.value.copy(selectedSupplier = event.supplier, error = null)
            }
            is PurchaseOrderEvent.OrderNameChanged -> {
                _state.value = _state.value.copy(orderName = event.value)
            }
            is PurchaseOrderEvent.ExpectedDeliveryDateChanged -> {
                _state.value = _state.value.copy(expectedDeliveryDate = event.timestamp, error = null)
            }
            PurchaseOrderEvent.AddItem -> {
                val newItem = PurchaseOrderItem()
                _state.value = _state.value.copy(tempItems = _state.value.tempItems + newItem)
            }
            is PurchaseOrderEvent.UpdateItem -> {
                val updatedItems = _state.value.tempItems.toMutableList()
                val item = updatedItems[event.index].copy(
                    productId = event.productId,
                    productName = event.productName,
                    quantity = event.quantity,
                    costPrice = event.costPrice,
                    total = event.quantity * event.costPrice
                )
                updatedItems[event.index] = item
                _state.value = _state.value.copy(tempItems = updatedItems, error = null)
            }
            is PurchaseOrderEvent.RemoveItem -> {
                val updatedItems = _state.value.tempItems.toMutableList()
                updatedItems.removeAt(event.index)
                _state.value = _state.value.copy(tempItems = updatedItems)
            }
            is PurchaseOrderEvent.NotesChanged -> _state.value = _state.value.copy(notes = event.value)
            PurchaseOrderEvent.SavePurchase -> savePurchase()
            PurchaseOrderEvent.ClearError -> _state.value = _state.value.copy(error = null)
            PurchaseOrderEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val suppliers = supplierRepository.getSuppliers().first()
                _state.value = _state.value.copy(suppliers = suppliers)
            } catch (e: Exception) {
                _state.value = _state.value.copy(suppliers = emptyList())
            }
            
            try {
                val products = inventoryRepository.getProducts().first()
                _state.value = _state.value.copy(products = products)
            } catch (e: Exception) {
                _state.value = _state.value.copy(products = emptyList())
            }
        }
    }

    private fun savePurchase() {
        val currentState = _state.value
        val supplier = currentState.selectedSupplier
        
        if (currentState.orderName.isBlank()) {
            _state.value = _state.value.copy(error = "Enter an order name")
            return
        }
        
        if (supplier == null) {
            _state.value = _state.value.copy(error = "Select a supplier")
            return
        }
        
        if (currentState.tempItems.isEmpty()) {
            _state.value = _state.value.copy(error = "Add at least one item")
            return
        }
        
        if (currentState.expectedDeliveryDate == 0L) {
            _state.value = _state.value.copy(error = "Select expected delivery date")
            return
        }
        
        val invalidItems = currentState.tempItems.filter { 
            it.productId.isEmpty() || it.quantity <= 0 || it.costPrice <= 0 
        }
        if (invalidItems.isNotEmpty()) {
            _state.value = _state.value.copy(error = "Fill in all item fields correctly")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            try {
                val orderNumber = generateOrderNumber()
                val totalCost = currentState.tempItems.sumOf { it.total }
                
                val order = com.akari.retailer.features.inventory.domain.models.PurchaseOrder(
                    orderName = currentState.orderName.trim(),
                    orderNumber = orderNumber,
                    supplierId = supplier.id,
                    supplierName = supplier.name,
                    status = PurchaseOrderStatus.ORDER,
                    orderItems = currentState.tempItems.map { item ->
                        PurchaseOrderItem(
                            productId = item.productId,
                            productName = item.productName,
                            quantity = item.quantity,
                            costPrice = item.costPrice,
                            total = item.quantity * item.costPrice
                        )
                    },
                    orderTotal = totalCost,
                    expectedDeliveryDate = currentState.expectedDeliveryDate,
                    notes = currentState.notes,
                    createdBy = "default"
                )
                
                val result = purchaseOrderRepository.createOrder(order)
                
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to save purchase"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Save failed", e)
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save purchase"
                )
            }
        }
    }
    
    private fun generateOrderNumber(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "PO-$date-$random"
    }
}

class PurchaseOrderViewModelFactory(
    private val inventoryRepository: InventoryRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val supplierRepository: SupplierRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderViewModel::class.java)) {
            return PurchaseOrderViewModel(inventoryRepository, purchaseOrderRepository, supplierRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
