package com.akari.retailer.features.inventory.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.Product
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.inventory.domain.models.StockMovement
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PurchaseOrderState(
    val suppliers: List<Supplier> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedSupplier: Supplier? = null,
    val selectedProduct: Product? = null,
    val quantity: String = "",
    val costPrice: String = "",
    val notes: String = "",
    val totalCost: Int = 0,
    val selectedStatus: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    val invoiceNumber: String = "",
    val invoiceAmount: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class PurchaseOrderEvent {
    data class SupplierSelected(val supplier: Supplier) : PurchaseOrderEvent()
    data class ProductSelected(val product: Product) : PurchaseOrderEvent()
    data class QuantityChanged(val value: String) : PurchaseOrderEvent()
    data class CostPriceChanged(val value: String) : PurchaseOrderEvent()
    data class NotesChanged(val value: String) : PurchaseOrderEvent()
    data class StatusChanged(val status: PurchaseOrderStatus) : PurchaseOrderEvent()
    data class InvoiceNumberChanged(val value: String) : PurchaseOrderEvent()
    data class InvoiceAmountChanged(val value: String) : PurchaseOrderEvent()
    data object LoadData : PurchaseOrderEvent()
    data object SavePurchase : PurchaseOrderEvent()
    data object ClearError : PurchaseOrderEvent()
    data object ResetSuccess : PurchaseOrderEvent()
}

class PurchaseOrderViewModel(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val TAG = "PurchaseOrderVM"
    private val _state = MutableStateFlow(PurchaseOrderState())
    val state: StateFlow<PurchaseOrderState> = _state.asStateFlow()

    init {
        Log.d(TAG, "ViewModel created")
        loadData()
    }

    fun handleEvent(event: PurchaseOrderEvent) {
        Log.d(TAG, "handleEvent: ${event::class.simpleName}")
        when (event) {
            is PurchaseOrderEvent.SupplierSelected -> {
                Log.d(TAG, "Supplier selected: ${event.supplier.name}")
                _state.value = _state.value.copy(selectedSupplier = event.supplier, error = null)
            }
            is PurchaseOrderEvent.ProductSelected -> {
                Log.d(TAG, "Product selected: ${event.product.name}")
                _state.value = _state.value.copy(
                    selectedProduct = event.product,
                    error = null
                )
                calculateTotal()
            }
            is PurchaseOrderEvent.QuantityChanged -> {
                _state.value = _state.value.copy(quantity = event.value)
                calculateTotal()
            }
            is PurchaseOrderEvent.CostPriceChanged -> {
                _state.value = _state.value.copy(costPrice = event.value)
                calculateTotal()
            }
            is PurchaseOrderEvent.NotesChanged -> _state.value = _state.value.copy(notes = event.value)
            is PurchaseOrderEvent.StatusChanged -> _state.value = _state.value.copy(selectedStatus = event.status)
            is PurchaseOrderEvent.InvoiceNumberChanged -> _state.value = _state.value.copy(invoiceNumber = event.value)
            is PurchaseOrderEvent.InvoiceAmountChanged -> _state.value = _state.value.copy(invoiceAmount = event.value)
            PurchaseOrderEvent.LoadData -> loadData()
            PurchaseOrderEvent.SavePurchase -> {
                Log.d(TAG, "🔥 SAVE EVENT RECEIVED!")
                savePurchase()
            }
            PurchaseOrderEvent.ClearError -> _state.value = _state.value.copy(error = null)
            PurchaseOrderEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun calculateTotal() {
        val quantity = _state.value.quantity.toIntOrNull() ?: 0
        val costPrice = _state.value.costPrice.toIntOrNull() ?: 0
        _state.value = _state.value.copy(totalCost = quantity * costPrice)
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val suppliers = supplierRepository.getSuppliers().first()
                _state.value = _state.value.copy(suppliers = suppliers)
            } catch (e: Exception) {
                _state.value = _state.value.copy(suppliers = emptyList())
            }
            
            try {
                val products = inventoryRepository.getProducts().first()
                _state.value = _state.value.copy(
                    products = products,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    products = emptyList(),
                    isLoading = false,
                    error = "Failed to load products: ${e.message}"
                )
            }
        }
    }

    private fun savePurchase() {
        Log.d(TAG, "savePurchase: STARTED!")
        val currentState = _state.value
        
        val supplier = currentState.selectedSupplier
        val product = currentState.selectedProduct
        
        if (supplier == null) {
            Log.e(TAG, "❌ No supplier selected")
            _state.value = _state.value.copy(error = "Select a supplier")
            return
        }
        
        if (product == null) {
            Log.e(TAG, "❌ No product selected")
            _state.value = _state.value.copy(error = "Select a product")
            return
        }
        
        val quantityInt = currentState.quantity.toIntOrNull()
        if (quantityInt == null || quantityInt <= 0) {
            Log.e(TAG, "❌ Invalid quantity: ${currentState.quantity}")
            _state.value = _state.value.copy(error = "Enter a valid quantity")
            return
        }
        
        val costPriceInt = currentState.costPrice.toIntOrNull()
        if (costPriceInt == null || costPriceInt <= 0) {
            Log.e(TAG, "❌ Invalid cost price: ${currentState.costPrice}")
            _state.value = _state.value.copy(error = "Enter a valid cost price")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            Log.d(TAG, "💾 Saving purchase...")
            
            try {
                val newStock = product.stockQuantity + quantityInt
                val updatedProduct = product.copy(
                    stockQuantity = newStock,
                    updatedAt = System.currentTimeMillis()
                )
                
                val updateResult = inventoryRepository.updateProduct(updatedProduct)
                if (updateResult.isFailure) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = updateResult.exceptionOrNull()?.message ?: "Failed to update product"
                    )
                    return@launch
                }
                Log.d(TAG, "✅ Product stock updated")
                
                val movement = StockMovement(
                    productId = product.id,
                    type = MovementType.PURCHASE,
                    quantity = quantityInt,
                    previousStock = product.stockQuantity,
                    newStock = newStock,
                    reason = "Purchase from ${supplier.name}",
                    userId = "default"
                )
                
                val movementResult = stockRepository.addMovement(movement)
                if (movementResult.isFailure) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = movementResult.exceptionOrNull()?.message ?: "Failed to record movement"
                    )
                    return@launch
                }
                Log.d(TAG, "✅ Stock movement recorded")
                
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
                Log.d(TAG, "✅ PURCHASE SAVED!")
                
                kotlinx.coroutines.delay(1500)
                _state.value = _state.value.copy(
                    selectedSupplier = null,
                    selectedProduct = null,
                    quantity = "",
                    costPrice = "",
                    notes = "",
                    totalCost = 0,
                    invoiceNumber = "",
                    invoiceAmount = "",
                    selectedStatus = PurchaseOrderStatus.DRAFT,
                    saveSuccess = false
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Save failed", e)
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save purchase"
                )
            }
        }
    }
}

class PurchaseOrderViewModelFactory(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository,
    private val supplierRepository: SupplierRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderViewModel::class.java)) {
            return PurchaseOrderViewModel(inventoryRepository, stockRepository, supplierRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
