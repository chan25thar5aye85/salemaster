package com.akari.retailer.features.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.inventory.data.repository.StockRepository
import com.akari.retailer.features.inventory.domain.models.StockMovement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StockHistoryState(
    val movements: List<StockMovement> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val currentStock: Int = 0
)

class StockHistoryViewModel(
    private val repository: StockRepository,
    private val productId: String
) : ViewModel() {

    private val _state = MutableStateFlow(StockHistoryState())
    val state: StateFlow<StockHistoryState> = _state.asStateFlow()

    init {
        loadMovements()
    }

    fun loadMovements() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getMovementsForProduct(productId).collect { movements ->
                    val totalIn = movements.filter { it.quantity > 0 }.sumOf { it.quantity }
                    val totalOut = movements.filter { it.quantity < 0 }.sumOf { kotlin.math.abs(it.quantity) }
                    val currentStock = movements.lastOrNull()?.newStock ?: 0
                    
                    _state.value = _state.value.copy(
                        movements = movements,
                        isLoading = false,
                        error = null,
                        totalIn = totalIn,
                        totalOut = totalOut,
                        currentStock = currentStock
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load stock history"
                )
            }
        }
    }
}

class StockHistoryViewModelFactory(
    private val repository: StockRepository,
    private val productId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockHistoryViewModel::class.java)) {
            return StockHistoryViewModel(repository, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
