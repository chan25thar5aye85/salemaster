package com.akari.retailer.features.supplier.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupplierAddState(
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class SupplierAddEvent {
    data class NameChanged(val value: String) : SupplierAddEvent()
    data class CompanyChanged(val value: String) : SupplierAddEvent()
    data class PhoneChanged(val value: String) : SupplierAddEvent()
    data class EmailChanged(val value: String) : SupplierAddEvent()
    data class AddressChanged(val value: String) : SupplierAddEvent()
    data class NotesChanged(val value: String) : SupplierAddEvent()
    data object SaveSupplier : SupplierAddEvent()
    data object ClearError : SupplierAddEvent()
    data object ResetSuccess : SupplierAddEvent()
}

class SupplierAddViewModel(
    private val repository: SupplierRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierAddState())
    val state: StateFlow<SupplierAddState> = _state.asStateFlow()

    fun handleEvent(event: SupplierAddEvent) {
        when (event) {
            is SupplierAddEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is SupplierAddEvent.CompanyChanged -> _state.value = _state.value.copy(company = event.value)
            is SupplierAddEvent.PhoneChanged -> _state.value = _state.value.copy(phone = event.value)
            is SupplierAddEvent.EmailChanged -> _state.value = _state.value.copy(email = event.value)
            is SupplierAddEvent.AddressChanged -> _state.value = _state.value.copy(address = event.value)
            is SupplierAddEvent.NotesChanged -> _state.value = _state.value.copy(notes = event.value)
            SupplierAddEvent.SaveSupplier -> saveSupplier()
            SupplierAddEvent.ClearError -> _state.value = _state.value.copy(error = null)
            SupplierAddEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun saveSupplier() {
        val currentState = _state.value
        
        if (currentState.name.isBlank()) {
            _state.value = _state.value.copy(error = "Supplier name is required")
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            val supplier = Supplier(
                name = currentState.name.trim(),
                company = currentState.company.trim(),
                phone = currentState.phone.trim(),
                email = currentState.email.trim(),
                address = currentState.address.trim(),
                notes = currentState.notes.trim()
            )
            
            val result = repository.addSupplier(supplier)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save supplier"
                )
            }
        }
    }
}

class SupplierAddViewModelFactory(
    private val repository: SupplierRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierAddViewModel::class.java)) {
            return SupplierAddViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
