package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerDetailState(
    val customer: Customer? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class CustomerDetailEvent {
    data object LoadCustomer : CustomerDetailEvent()
    data object ClearError : CustomerDetailEvent()
}

class CustomerDetailViewModel(
    private val repository: CustomerRepository,
    private val customerId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerDetailState())
    val state: StateFlow<CustomerDetailState> = _state.asStateFlow()

    init {
        loadCustomer()
    }

    fun handleEvent(event: CustomerDetailEvent) {
        when (event) {
            is CustomerDetailEvent.LoadCustomer -> loadCustomer()
            is CustomerDetailEvent.ClearError -> clearError()
        }
    }

    private fun loadCustomer() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getCustomerById(customerId).collect { customer ->
                    _state.value = _state.value.copy(
                        customer = customer,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load customer"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class CustomerDetailViewModelFactory(
    private val repository: CustomerRepository,
    private val customerId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerDetailViewModel::class.java)) {
            return CustomerDetailViewModel(repository, customerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
