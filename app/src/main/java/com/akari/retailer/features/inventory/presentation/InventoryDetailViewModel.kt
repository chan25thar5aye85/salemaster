package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import com.akari.retailer.features.inventory.domain.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryDetailState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class InventoryDetailEvent {
    data object LoadProduct : InventoryDetailEvent()
    data object ClearError : InventoryDetailEvent()
}

class InventoryDetailViewModel(
    private val repository: InventoryRepository,
    private val productId: String
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryDetailState())
    val state: StateFlow<InventoryDetailState> = _state.asStateFlow()

    init {
        loadProduct()
    }

    fun handleEvent(event: InventoryDetailEvent) {
        when (event) {
            is InventoryDetailEvent.LoadProduct -> loadProduct()
            is InventoryDetailEvent.ClearError -> clearError()
        }
    }

    private fun loadProduct() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getProductById(productId).collect { product ->
                    _state.value = _state.value.copy(
                        product = product,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load product"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class InventoryDetailViewModelFactory(
    private val repository: InventoryRepository,
    private val productId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryDetailViewModel::class.java)) {
            return InventoryDetailViewModel(repository, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
