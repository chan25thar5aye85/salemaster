package com.akari.retailer.features.sales.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.sales.data.repository.SaleFinalizer
import com.akari.retailer.features.sales.domain.models.Sale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SaleHistoryViewModel(
    private val repository: SaleRepository,
    private val saleFinalizer: SaleFinalizer
) : ViewModel() {

    private val _state = MutableStateFlow(SaleHistoryState())
    val state: StateFlow<SaleHistoryState> = _state.asStateFlow()

    private var loadJob: Job? = null

    private var allSales: List<Sale> = emptyList()

    init {
        loadSales()
    }

    fun handleEvent(event: SaleHistoryEvent) {
        when (event) {
            is SaleHistoryEvent.LoadSales -> loadSales()
            is SaleHistoryEvent.RefreshSales -> refreshSales()
            is SaleHistoryEvent.DeleteSale -> deleteSale(event.saleId)
            is SaleHistoryEvent.ClearError -> clearError()
            is SaleHistoryEvent.ResetDeleteSuccess -> { }
            is SaleHistoryEvent.TimeFilterChanged -> setTimeFilter(event.filter)
        }
    }

    private fun loadSales() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getSales().collect { sales ->
                    allSales = sales
                    applyFilters()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sales"
                )
            }
        }
    }

    private fun refreshSales() = loadSales()

    private fun deleteSale(saleId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = saleFinalizer.deleteSale(saleId)
                if (result.isSuccess) loadSales()
                else _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to delete sale"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete sale"
                )
            }
        }
    }

    private fun setTimeFilter(filter: TimeFilter) {
        _state.value = _state.value.copy(timeFilter = filter)
        applyFilters()
    }

    private fun applyFilters() {
        val range = _state.value.timeFilter.resolveRange()
        val filtered = allSales.filter { sale ->
            sale.timestamp in range.first..range.last
        }
        _state.value = _state.value.copy(
            sales = filtered,
            isLoading = false,
            isRefreshing = false
        )
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class SaleHistoryViewModelFactory(
    private val repository: SaleRepository,
    private val saleFinalizer: SaleFinalizer
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleHistoryViewModel::class.java)) {
            return SaleHistoryViewModel(repository, saleFinalizer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
