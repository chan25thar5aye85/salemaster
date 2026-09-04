package com.akari.retailer.features.sales.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.Sale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleHistoryViewModel(
    private val repository: SaleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SaleHistoryState())
    val state: StateFlow<SaleHistoryState> = _state.asStateFlow()

    private var allSales: List<Sale> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadSales()
    }

    fun handleEvent(event: SaleHistoryEvent) {
        when (event) {
            is SaleHistoryEvent.LoadSales -> loadSales()
            is SaleHistoryEvent.RefreshSales -> refreshSales()
            is SaleHistoryEvent.DeleteSale -> deleteSale(event.saleId)
            is SaleHistoryEvent.ClearError -> clearError()
            is SaleHistoryEvent.ResetDeleteSuccess -> resetDeleteSuccess()
            is SaleHistoryEvent.FilterByDate -> filterByDate(event.timestamp)
            is SaleHistoryEvent.ClearDateFilter -> clearDateFilter()
        }
    }

    private fun loadSales() {
        viewModelScope.launch {
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

    private fun refreshSales() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true)
            try {
                repository.getSales().collect { sales ->
                    allSales = sales
                    applyFilters()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Failed to refresh sales"
                )
            }
        }
    }

    private fun deleteSale(saleId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = repository.deleteSale(saleId)
                if (result.isSuccess) {
                    loadSales()
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to delete sale"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to delete sale"
                )
            }
        }
    }

    private fun filterByDate(timestamp: Long) {
        val dateLabel = dateFormat.format(Date(timestamp))
        _state.value = _state.value.copy(
            filterDate = timestamp,
            filterDateLabel = dateLabel
        )
        applyFilters()
    }

    private fun clearDateFilter() {
        _state.value = _state.value.copy(
            filterDate = null,
            filterDateLabel = ""
        )
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allSales.filter { sale ->
            var matches = true
            
            // Date filter
            _state.value.filterDate?.let { filterDate ->
                val saleDate = Date(sale.timestamp)
                val filterDateObj = Date(filterDate)
                
                // Compare day, month, year (ignore time)
                val saleCalendar = java.util.Calendar.getInstance().apply { time = saleDate }
                val filterCalendar = java.util.Calendar.getInstance().apply { time = filterDateObj }
                
                matches = matches && (
                    saleCalendar.get(java.util.Calendar.YEAR) == filterCalendar.get(java.util.Calendar.YEAR) &&
                    saleCalendar.get(java.util.Calendar.DAY_OF_YEAR) == filterCalendar.get(java.util.Calendar.DAY_OF_YEAR)
                )
            }
            
            matches
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

    private fun resetDeleteSuccess() {
        // No-op
    }
}
