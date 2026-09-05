package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InventoryListViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryListState())
    val state: StateFlow<InventoryListState> = _state.asStateFlow()

    init {
        loadProducts()
    }

    fun handleEvent(event: InventoryListEvent) {
        when (event) {
            is InventoryListEvent.LoadProducts -> loadProducts()
            is InventoryListEvent.RefreshProducts -> refreshProducts()
            is InventoryListEvent.DeleteProduct -> deleteProduct(event.productId)
            is InventoryListEvent.ClearError -> clearError()
            is InventoryListEvent.SearchQueryChanged -> searchQueryChanged(event.query)
            is InventoryListEvent.ClearSearch -> clearSearch()
            is InventoryListEvent.ToggleLowStockFilter -> toggleLowStockFilter()
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getProducts().collect { products ->
                    _state.value = _state.value.copy(
                        allProducts = products,
                        isLoading = false,
                        error = null,
                        totalProducts = products.size
                    )
                    applyFilters()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load products"
                )
            }
        }
    }

    private fun refreshProducts() {
        loadProducts()
    }

    private fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = repository.deleteProduct(productId)
                if (result.isSuccess) {
                    loadProducts()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete product"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete product"
                )
            }
        }
    }

    private fun searchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applyFilters()
    }

    private fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
        applyFilters()
    }

    private fun toggleLowStockFilter() {
        _state.value = _state.value.copy(showLowStockOnly = !_state.value.showLowStockOnly)
        applyFilters()
    }

    private fun applyFilters() {
        val query = _state.value.searchQuery.lowercase().trim()
        val allProducts = _state.value.allProducts
        val showLowStockOnly = _state.value.showLowStockOnly
        
        var filtered = allProducts
        
        if (query.isNotEmpty()) {
            filtered = filtered.filter { product ->
                product.name.lowercase().contains(query) ||
                product.category.lowercase().contains(query) ||
                product.sku.lowercase().contains(query)
            }
        }
        
        if (showLowStockOnly) {
            filtered = filtered.filter { it.isLowStock || it.isOutOfStock }
        }
        
        _state.value = _state.value.copy(products = filtered)
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
