package com.akari.retailer.features.supplier.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupplierDetailState(
    val supplier: Supplier? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class SupplierDetailEvent {
    data object LoadSupplier : SupplierDetailEvent()
    data object ClearError : SupplierDetailEvent()
}

class SupplierDetailViewModel(
    private val repository: SupplierRepository,
    private val supplierId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierDetailState())
    val state: StateFlow<SupplierDetailState> = _state.asStateFlow()

    init {
        loadSupplier()
    }

    fun handleEvent(event: SupplierDetailEvent) {
        when (event) {
            is SupplierDetailEvent.LoadSupplier -> loadSupplier()
            is SupplierDetailEvent.ClearError -> clearError()
        }
    }

    private fun loadSupplier() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getSupplierById(supplierId).collect { supplier ->
                    _state.value = _state.value.copy(
                        supplier = supplier,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load supplier"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class SupplierDetailViewModelFactory(
    private val repository: SupplierRepository,
    private val supplierId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierDetailViewModel::class.java)) {
            return SupplierDetailViewModel(repository, supplierId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
