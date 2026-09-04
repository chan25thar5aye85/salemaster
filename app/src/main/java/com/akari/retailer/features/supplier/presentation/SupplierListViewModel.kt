package com.akari.retailer.features.supplier.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SupplierListViewModel(
    private val repository: SupplierRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierListState())
    val state: StateFlow<SupplierListState> = _state.asStateFlow()

    init {
        loadSuppliers()
    }

    fun handleEvent(event: SupplierListEvent) {
        when (event) {
            is SupplierListEvent.LoadSuppliers -> loadSuppliers()
            is SupplierListEvent.RefreshSuppliers -> refreshSuppliers()
            is SupplierListEvent.DeleteSupplier -> deleteSupplier(event.supplierId)
            is SupplierListEvent.ClearError -> clearError()
            is SupplierListEvent.SearchQueryChanged -> searchQueryChanged(event.query)
            is SupplierListEvent.ClearSearch -> clearSearch()
        }
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getSuppliers().collect { suppliers ->
                    _state.value = _state.value.copy(
                        allSuppliers = suppliers,
                        isLoading = false,
                        error = null
                    )
                    applySearch()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load suppliers"
                )
            }
        }
    }

    private fun refreshSuppliers() {
        loadSuppliers()
    }

    private fun deleteSupplier(supplierId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = repository.deleteSupplier(supplierId)
                if (result.isSuccess) {
                    loadSuppliers()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete supplier"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete supplier"
                )
            }
        }
    }

    private fun searchQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        applySearch()
    }

    private fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "")
        applySearch()
    }

    private fun applySearch() {
        val query = _state.value.searchQuery.lowercase().trim()
        val allSuppliers = _state.value.allSuppliers
        
        val filtered = if (query.isEmpty()) {
            allSuppliers
        } else {
            allSuppliers.filter { supplier ->
                supplier.name.lowercase().contains(query) ||
                supplier.company.lowercase().contains(query) ||
                supplier.phone.lowercase().contains(query) ||
                supplier.email.lowercase().contains(query)
            }
        }
        
        _state.value = _state.value.copy(suppliers = filtered)
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
