package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerEditState(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val totalSpent: Int = 0,
    val totalOrders: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class CustomerEditEvent {
    data class NameChanged(val value: String) : CustomerEditEvent()
    data class PhoneChanged(val value: String) : CustomerEditEvent()
    data class EmailChanged(val value: String) : CustomerEditEvent()
    data class AddressChanged(val value: String) : CustomerEditEvent()
    data class NotesChanged(val value: String) : CustomerEditEvent()
    data object LoadCustomer : CustomerEditEvent()
    data object SaveCustomer : CustomerEditEvent()
    data object ClearError : CustomerEditEvent()
    data object ResetSuccess : CustomerEditEvent()
}

class CustomerEditViewModel(
    private val repository: CustomerRepository,
    private val customerId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerEditState(id = customerId))
    val state: StateFlow<CustomerEditState> = _state.asStateFlow()

    init {
        loadCustomer()
    }

    fun handleEvent(event: CustomerEditEvent) {
        when (event) {
            is CustomerEditEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is CustomerEditEvent.PhoneChanged -> _state.value = _state.value.copy(phone = event.value)
            is CustomerEditEvent.EmailChanged -> _state.value = _state.value.copy(email = event.value)
            is CustomerEditEvent.AddressChanged -> _state.value = _state.value.copy(address = event.value)
            is CustomerEditEvent.NotesChanged -> _state.value = _state.value.copy(notes = event.value)
            CustomerEditEvent.LoadCustomer -> loadCustomer()
            CustomerEditEvent.SaveCustomer -> saveCustomer()
            CustomerEditEvent.ClearError -> _state.value = _state.value.copy(error = null)
            CustomerEditEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadCustomer() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getCustomerById(customerId).collect { customer ->
                    if (customer != null) {
                        _state.value = _state.value.copy(
                            id = customer.id,
                            name = customer.name,
                            phone = customer.phone,
                            email = customer.email,
                            address = customer.address,
                            notes = customer.notes,
                            totalSpent = customer.totalSpent,
                            totalOrders = customer.totalOrders,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Customer not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load customer"
                )
            }
        }
    }

    private fun saveCustomer() {
        val currentState = _state.value
        
        if (currentState.name.isBlank()) {
            _state.value = _state.value.copy(error = "Customer name is required")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val customer = Customer(
                id = currentState.id,
                name = currentState.name.trim(),
                phone = currentState.phone.trim(),
                email = currentState.email.trim(),
                address = currentState.address.trim(),
                notes = currentState.notes.trim(),
                totalSpent = currentState.totalSpent,
                totalOrders = currentState.totalOrders
            )
            
            val result = repository.updateCustomer(customer)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update customer"
                )
            }
        }
    }
}

class CustomerEditViewModelFactory(
    private val repository: CustomerRepository,
    private val customerId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerEditViewModel::class.java)) {
            return CustomerEditViewModel(repository, customerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
