package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.Product
import com.akari.retailer.features.inventory.domain.models.StockMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StockAdjustmentState(
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val newStock: String = "",
    val reason: String = "",
    val notes: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val currentStock: Int = 0
)

sealed class StockAdjustmentEvent {
    data class ProductSelected(val product: Product) : StockAdjustmentEvent()
    data class NewStockChanged(val value: String) : StockAdjustmentEvent()
    data class ReasonChanged(val value: String) : StockAdjustmentEvent()
    data class NotesChanged(val value: String) : StockAdjustmentEvent()
    data object LoadProducts : StockAdjustmentEvent()
    data object SaveAdjustment : StockAdjustmentEvent()
    data object ClearError : StockAdjustmentEvent()
    data object ResetSuccess : StockAdjustmentEvent()
}

class StockAdjustmentViewModel(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StockAdjustmentState())
    val state: StateFlow<StockAdjustmentState> = _state.asStateFlow()

    init {
        loadProducts()
    }

    fun handleEvent(event: StockAdjustmentEvent) {
        when (event) {
            is StockAdjustmentEvent.ProductSelected -> selectProduct(event.product)
            is StockAdjustmentEvent.NewStockChanged -> _state.value = _state.value.copy(newStock = event.value)
            is StockAdjustmentEvent.ReasonChanged -> _state.value = _state.value.copy(reason = event.value)
            is StockAdjustmentEvent.NotesChanged -> _state.value = _state.value.copy(notes = event.value)
            StockAdjustmentEvent.LoadProducts -> loadProducts()
            StockAdjustmentEvent.SaveAdjustment -> saveAdjustment()
            StockAdjustmentEvent.ClearError -> _state.value = _state.value.copy(error = null)
            StockAdjustmentEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                inventoryRepository.getProducts().collect { products ->
                    _state.value = _state.value.copy(
                        products = products,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load products"
                )
            }
        }
    }

    private fun selectProduct(product: Product) {
        _state.value = _state.value.copy(
            selectedProduct = product,
            currentStock = product.stockQuantity,
            newStock = product.stockQuantity.toString(),
            error = null
        )
    }

    private fun saveAdjustment() {
        val currentState = _state.value
        val product = currentState.selectedProduct
        
        if (product == null) {
            _state.value = _state.value.copy(error = "Select a product")
            return
        }
        
        val newStockInt = currentState.newStock.toIntOrNull()
        if (newStockInt == null || newStockInt < 0) {
            _state.value = _state.value.copy(error = "Enter a valid stock quantity")
            return
        }
        
        if (currentState.reason.isBlank()) {
            _state.value = _state.value.copy(error = "Enter a reason for adjustment")
            return
        }
        
        if (newStockInt == product.stockQuantity) {
            _state.value = _state.value.copy(error = "Stock quantity is the same")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            try {
                val updatedProduct = product.copy(
                    stockQuantity = newStockInt,
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
                
                val movement = StockMovement(
                    productId = product.id,
                    type = MovementType.ADJUSTMENT,
                    quantity = newStockInt - product.stockQuantity,
                    previousStock = product.stockQuantity,
                    newStock = newStockInt,
                    reason = currentState.reason,
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
                
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
                
                // Reset form after success
                _state.value = _state.value.copy(
                    selectedProduct = null,
                    currentStock = 0,
                    newStock = "",
                    reason = "",
                    notes = ""
                )
                
                // Reload products to refresh stock display
                loadProducts()
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save adjustment"
                )
            }
        }
    }
}

class StockAdjustmentViewModelFactory(
    private val inventoryRepository: InventoryRepository,
    private val stockRepository: StockRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockAdjustmentViewModel::class.java)) {
            return StockAdjustmentViewModel(inventoryRepository, stockRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
