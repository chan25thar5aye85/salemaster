package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.domain.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryAddState(
    val name: String = "",
    val category: String = "",
    val sku: String = "",
    val costPrice: String = "",
    val sellPrice: String = "",
    val stockQuantity: String = "",
    val minStockLevel: String = "",
    val supplierId: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class InventoryAddEvent {
    data class NameChanged(val value: String) : InventoryAddEvent()
    data class CategoryChanged(val value: String) : InventoryAddEvent()
    data class SkuChanged(val value: String) : InventoryAddEvent()
    data class CostPriceChanged(val value: String) : InventoryAddEvent()
    data class SellPriceChanged(val value: String) : InventoryAddEvent()
    data class StockQuantityChanged(val value: String) : InventoryAddEvent()
    data class MinStockLevelChanged(val value: String) : InventoryAddEvent()
    data class SupplierIdChanged(val value: String) : InventoryAddEvent()
    data object SaveProduct : InventoryAddEvent()
    data object ClearError : InventoryAddEvent()
    data object ResetSuccess : InventoryAddEvent()
}

class InventoryAddViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryAddState())
    val state: StateFlow<InventoryAddState> = _state.asStateFlow()

    fun handleEvent(event: InventoryAddEvent) {
        when (event) {
            is InventoryAddEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is InventoryAddEvent.CategoryChanged -> _state.value = _state.value.copy(category = event.value)
            is InventoryAddEvent.SkuChanged -> _state.value = _state.value.copy(sku = event.value)
            is InventoryAddEvent.CostPriceChanged -> _state.value = _state.value.copy(costPrice = event.value)
            is InventoryAddEvent.SellPriceChanged -> _state.value = _state.value.copy(sellPrice = event.value)
            is InventoryAddEvent.StockQuantityChanged -> _state.value = _state.value.copy(stockQuantity = event.value)
            is InventoryAddEvent.MinStockLevelChanged -> _state.value = _state.value.copy(minStockLevel = event.value)
            is InventoryAddEvent.SupplierIdChanged -> _state.value = _state.value.copy(supplierId = event.value)
            InventoryAddEvent.SaveProduct -> saveProduct()
            InventoryAddEvent.ClearError -> _state.value = _state.value.copy(error = null)
            InventoryAddEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun saveProduct() {
        val currentState = _state.value
        
        if (currentState.name.isBlank()) {
            _state.value = _state.value.copy(error = "Product name is required")
            return
        }
        
        val sellPriceInt = currentState.sellPrice.toIntOrNull()
        if (sellPriceInt == null || sellPriceInt <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid sell price")
            return
        }
        
        val costPriceInt = currentState.costPrice.toIntOrNull() ?: 0
        val stockQuantityInt = currentState.stockQuantity.toIntOrNull() ?: 0
        val minStockLevelInt = currentState.minStockLevel.toIntOrNull() ?: 0
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val product = Product(
                name = currentState.name.trim(),
                category = currentState.category.trim(),
                sku = currentState.sku.trim(),
                costPrice = costPriceInt,
                sellPrice = sellPriceInt,
                stockQuantity = stockQuantityInt,
                minStockLevel = minStockLevelInt,
                supplierId = currentState.supplierId.trim()
            )
            
            val result = repository.addProduct(product)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save product"
                )
            }
        }
    }
}

class InventoryAddViewModelFactory(
    private val repository: InventoryRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryAddViewModel::class.java)) {
            return InventoryAddViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
