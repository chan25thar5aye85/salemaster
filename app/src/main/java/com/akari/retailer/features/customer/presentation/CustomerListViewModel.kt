package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerListViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerListState())
    val state: StateFlow<CustomerListState> = _state.asStateFlow()

    init {
        loadCustomers()
    }

    fun handleEvent(event: CustomerListEvent) {
        when (event) {
            is CustomerListEvent.LoadCustomers -> loadCustomers()
            is CustomerListEvent.RefreshCustomers -> refreshCustomers()
            is CustomerListEvent.DeleteCustomer -> deleteCustomer(event.customerId)
            is CustomerListEvent.ClearError -> clearError()
            is CustomerListEvent.SearchQueryChanged -> searchQueryChanged(event.query)
            is CustomerListEvent.ClearSearch -> clearSearch()
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getCustomers().collect { customers ->
                    _state.value = _state.value.copy(
                        allCustomers = customers,
                        isLoading = false,
                        error = null
                    )
                    applySearch()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load customers"
                )
            }
        }
    }

    private fun refreshCustomers() {
        loadCustomers()
    }

    private fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val result = repository.deleteCustomer(customerId)
                if (result.isSuccess) {
                    loadCustomers()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete customer"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete customer"
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
        val allCustomers = _state.value.allCustomers
        
        val filtered = if (query.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter { customer ->
                customer.name.lowercase().contains(query) ||
                customer.phone.lowercase().contains(query) ||
                customer.email.lowercase().contains(query)
            }
        }
        
        _state.value = _state.value.copy(customers = filtered)
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
