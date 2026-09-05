package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.domain.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryEditState(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val sku: String = "",
    val costPrice: String = "",
    val sellPrice: String = "",
    val stockQuantity: String = "",
    val minStockLevel: String = "",
    val supplierId: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class InventoryEditEvent {
    data class NameChanged(val value: String) : InventoryEditEvent()
    data class CategoryChanged(val value: String) : InventoryEditEvent()
    data class SkuChanged(val value: String) : InventoryEditEvent()
    data class CostPriceChanged(val value: String) : InventoryEditEvent()
    data class SellPriceChanged(val value: String) : InventoryEditEvent()
    data class StockQuantityChanged(val value: String) : InventoryEditEvent()
    data class MinStockLevelChanged(val value: String) : InventoryEditEvent()
    data class SupplierIdChanged(val value: String) : InventoryEditEvent()
    data object LoadProduct : InventoryEditEvent()
    data object SaveProduct : InventoryEditEvent()
    data object ClearError : InventoryEditEvent()
    data object ResetSuccess : InventoryEditEvent()
}

class InventoryEditViewModel(
    private val repository: InventoryRepository,
    private val productId: String
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryEditState(id = productId))
    val state: StateFlow<InventoryEditState> = _state.asStateFlow()

    init {
        loadProduct()
    }

    fun handleEvent(event: InventoryEditEvent) {
        when (event) {
            is InventoryEditEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is InventoryEditEvent.CategoryChanged -> _state.value = _state.value.copy(category = event.value)
            is InventoryEditEvent.SkuChanged -> _state.value = _state.value.copy(sku = event.value)
            is InventoryEditEvent.CostPriceChanged -> _state.value = _state.value.copy(costPrice = event.value)
            is InventoryEditEvent.SellPriceChanged -> _state.value = _state.value.copy(sellPrice = event.value)
            is InventoryEditEvent.StockQuantityChanged -> _state.value = _state.value.copy(stockQuantity = event.value)
            is InventoryEditEvent.MinStockLevelChanged -> _state.value = _state.value.copy(minStockLevel = event.value)
            is InventoryEditEvent.SupplierIdChanged -> _state.value = _state.value.copy(supplierId = event.value)
            InventoryEditEvent.LoadProduct -> loadProduct()
            InventoryEditEvent.SaveProduct -> saveProduct()
            InventoryEditEvent.ClearError -> _state.value = _state.value.copy(error = null)
            InventoryEditEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getProductById(productId).collect { product ->
                    if (product != null) {
                        _state.value = _state.value.copy(
                            id = product.id,
                            name = product.name,
                            category = product.category,
                            sku = product.sku,
                            costPrice = product.costPrice.toString(),
                            sellPrice = product.sellPrice.toString(),
                            stockQuantity = product.stockQuantity.toString(),
                            minStockLevel = product.minStockLevel.toString(),
                            supplierId = product.supplierId,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Product not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load product"
                )
            }
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
                id = currentState.id,
                name = currentState.name.trim(),
                category = currentState.category.trim(),
                sku = currentState.sku.trim(),
                costPrice = costPriceInt,
                sellPrice = sellPriceInt,
                stockQuantity = stockQuantityInt,
                minStockLevel = minStockLevelInt,
                supplierId = currentState.supplierId.trim()
            )
            
            val result = repository.updateProduct(product)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update product"
                )
            }
        }
    }
}

class InventoryEditViewModelFactory(
    private val repository: InventoryRepository,
    private val productId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryEditViewModel::class.java)) {
            return InventoryEditViewModel(repository, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
