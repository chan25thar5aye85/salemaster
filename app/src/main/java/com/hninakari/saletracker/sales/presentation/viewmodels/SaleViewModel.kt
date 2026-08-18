package com.hninakari.saletracker.sales.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hninakari.saletracker.sales.data.models.PaymentMethod
import com.hninakari.saletracker.sales.data.models.Sales
import com.hninakari.saletracker.sales.data.repositories.RoomSaleRepository
import com.hninakari.saletracker.sales.data.repositories.SaleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaleUiState(
    val sales: List<Sales> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalSales: Double = 0.0,
    val saleCount: Int = 0,
    val selectedSale: Sales? = null,
    val isAddingSale: Boolean = false,
    val saleFormState: SaleFormState = SaleFormState()
)

data class SaleFormState(
    val amount: String = "",  // Amount field - starts empty
    val paymentMethod: PaymentMethod = PaymentMethod.CASH
)

class SaleViewModel(
    private val context: Context
) : ViewModel() {
    
    private val repository: SaleRepository = RoomSaleRepository(context)
    
    private val _uiState = MutableStateFlow(SaleUiState())
    val uiState: StateFlow<SaleUiState> = _uiState.asStateFlow()
    
    init {
        loadSales()
        loadAnalytics()
    }
    
    fun loadSales() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllSales().collect { salesList ->
                    _uiState.update { state ->
                        state.copy(
                            sales = salesList,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to load sales"
                    )
                }
            }
        }
    }
    
    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                val totalSales = repository.getTotalSalesAmount()
                val saleCount = repository.getSalesCount()
                
                _uiState.update { state ->
                    state.copy(
                        totalSales = totalSales,
                        saleCount = saleCount
                    )
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun addSale(sale: Sales) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.insertSale(sale)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isAddingSale = false,
                        saleFormState = SaleFormState(),  // Reset form
                        error = null
                    )
                }
                loadAnalytics()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message ?: "Failed to add sale"
                    )
                }
            }
        }
    }
    
    fun deleteSale(saleId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSale(saleId)
                loadAnalytics()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to delete sale")
                }
            }
        }
    }
    
    fun updateFormField(
        amount: String? = null,
        paymentMethod: PaymentMethod? = null
    ) {
        val currentForm = _uiState.value.saleFormState
        _uiState.update { state ->
            state.copy(
                saleFormState = currentForm.copy(
                    amount = amount ?: currentForm.amount,
                    paymentMethod = paymentMethod ?: currentForm.paymentMethod
                )
            )
        }
    }
    
    fun submitSale() {
        val form = _uiState.value.saleFormState
        val amount = form.amount.toDoubleOrNull() ?: 0.0
        
        if (amount <= 0.0) {
            _uiState.update { 
                it.copy(error = "Please enter a valid amount")
            }
            return
        }
        
        val sale = Sales(
            amount = amount,
            paymentMethod = form.paymentMethod
        )
        
        addSale(sale)
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun toggleAddSale() {
        _uiState.update { it.copy(isAddingSale = !it.isAddingSale) }
    }
}
