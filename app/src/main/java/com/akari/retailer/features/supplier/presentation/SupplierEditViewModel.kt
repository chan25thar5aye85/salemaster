package com.akari.retailer.features.supplier.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupplierEditState(
    val id: String = "",
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val totalPurchased: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

sealed class SupplierEditEvent {
    data class NameChanged(val value: String) : SupplierEditEvent()
    data class CompanyChanged(val value: String) : SupplierEditEvent()
    data class PhoneChanged(val value: String) : SupplierEditEvent()
    data class EmailChanged(val value: String) : SupplierEditEvent()
    data class AddressChanged(val value: String) : SupplierEditEvent()
    data class NotesChanged(val value: String) : SupplierEditEvent()
    data object LoadSupplier : SupplierEditEvent()
    data object SaveSupplier : SupplierEditEvent()
    data object ClearError : SupplierEditEvent()
    data object ResetSuccess : SupplierEditEvent()
}

class SupplierEditViewModel(
    private val repository: SupplierRepository,
    private val supplierId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierEditState(id = supplierId))
    val state: StateFlow<SupplierEditState> = _state.asStateFlow()

    init {
        loadSupplier()
    }

    fun handleEvent(event: SupplierEditEvent) {
        when (event) {
            is SupplierEditEvent.NameChanged -> _state.value = _state.value.copy(name = event.value)
            is SupplierEditEvent.CompanyChanged -> _state.value = _state.value.copy(company = event.value)
            is SupplierEditEvent.PhoneChanged -> _state.value = _state.value.copy(phone = event.value)
            is SupplierEditEvent.EmailChanged -> _state.value = _state.value.copy(email = event.value)
            is SupplierEditEvent.AddressChanged -> _state.value = _state.value.copy(address = event.value)
            is SupplierEditEvent.NotesChanged -> _state.value = _state.value.copy(notes = event.value)
            SupplierEditEvent.LoadSupplier -> loadSupplier()
            SupplierEditEvent.SaveSupplier -> saveSupplier()
            SupplierEditEvent.ClearError -> _state.value = _state.value.copy(error = null)
            SupplierEditEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadSupplier() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getSupplierById(supplierId).collect { supplier ->
                    if (supplier != null) {
                        _state.value = _state.value.copy(
                            id = supplier.id,
                            name = supplier.name,
                            company = supplier.company,
                            phone = supplier.phone,
                            email = supplier.email,
                            address = supplier.address,
                            notes = supplier.notes,
                            totalPurchased = supplier.totalPurchased,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Supplier not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load supplier"
                )
            }
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
                id = currentState.id,
                name = currentState.name.trim(),
                company = currentState.company.trim(),
                phone = currentState.phone.trim(),
                email = currentState.email.trim(),
                address = currentState.address.trim(),
                notes = currentState.notes.trim(),
                totalPurchased = currentState.totalPurchased
            )
            
            val result = repository.updateSupplier(supplier)
            
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update supplier"
                )
            }
        }
    }
}

class SupplierEditViewModelFactory(
    private val repository: SupplierRepository,
    private val supplierId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierEditViewModel::class.java)) {
            return SupplierEditViewModel(repository, supplierId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
