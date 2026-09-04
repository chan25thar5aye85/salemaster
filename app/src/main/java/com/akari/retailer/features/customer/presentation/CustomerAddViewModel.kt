package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CustomerAddState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class CustomerAddEvent {
    data class NameChanged(val value: String) : CustomerAddEvent()
    data class PhoneChanged(val value: String) : CustomerAddEvent()
    data class EmailChanged(val value: String) : CustomerAddEvent()
    data class AddressChanged(val value: String) : CustomerAddEvent()
    data class NotesChanged(val value: String) : CustomerAddEvent()
    data object SaveCustomer : CustomerAddEvent()
    data object ClearError : CustomerAddEvent()
    data object ResetSuccess : CustomerAddEvent()
}

class CustomerAddViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerAddState())
    val state: StateFlow<CustomerAddState> = _state.asStateFlow()

    fun handleEvent(event: CustomerAddEvent) {
        when (event) {
            is CustomerAddEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is CustomerAddEvent.PhoneChanged -> _state.value = _state.value.copy(phone = event.value)
            is CustomerAddEvent.EmailChanged -> _state.value = _state.value.copy(email = event.value)
            is CustomerAddEvent.AddressChanged -> _state.value = _state.value.copy(address = event.value)
            is CustomerAddEvent.NotesChanged -> _state.value = _state.value.copy(notes = event.value)
            CustomerAddEvent.SaveCustomer -> saveCustomer()
            CustomerAddEvent.ClearError -> _state.value = _state.value.copy(error = null)
            CustomerAddEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
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
                name = currentState.name.trim(),
                phone = currentState.phone.trim(),
                email = currentState.email.trim(),
                address = currentState.address.trim(),
                notes = currentState.notes.trim()
            )
            
            val result = repository.addCustomer(customer)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save customer"
                )
            }
        }
    }
}

class CustomerAddViewModelFactory(
    private val repository: CustomerRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerAddViewModel::class.java)) {
            return CustomerAddViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
