package com.akari.retailer.features.sales.presentation.entry

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.sales.domain.models.PaymentMethod
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.features.sales.domain.models.SaleItem
import com.akari.retailer.core.utils.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SaleEntryViewModel(
    private val repository: SaleRepository
) : ViewModel() {

    private val TAG = "SaleEntryViewModel"
    
    private val stateManager = SaleEntryStateManager()
    private val validator = SaleEntryValidator()

    private val _state = MutableStateFlow(SaleEntryState())
    val state: StateFlow<SaleEntryState> = _state.asStateFlow()

    init {
        Log.d(TAG, "✅ SaleEntryViewModel initialized")
        loadRecentSales()
    }

    fun handleEvent(event: SaleEntryEvent) {
        when (event) {
            is SaleEntryEvent.AmountChanged -> updateAmount(event.rowId, event.value)
            is SaleEntryEvent.RowFocused -> focusRow(event.rowId)
            is SaleEntryEvent.NextPressed -> nextRow(event.rowId)
            is SaleEntryEvent.RowDeleted -> deleteRow(event.rowId)
            is SaleEntryEvent.PaymentSelected -> selectPaymentMethod(event.method)
            SaleEntryEvent.SaveSale -> saveSale()
            SaleEntryEvent.ClearError -> clearError()
            SaleEntryEvent.ResetSaveSuccess -> resetSaveSuccess()
        }
    }

    fun getTotal(): Int {
        return stateManager.getItems(_state.value).sum()
    }

    fun getFormattedTotal(): String {
        return MoneyFormatter.formatTotal(getTotal())
    }

    private fun loadRecentSales() {
        viewModelScope.launch {
            try {
                repository.getSales().collect { sales ->
                    _state.value = _state.value.copy(
                        recentSales = sales.take(2)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load recent sales: ${e.message}")
            }
        }
    }

    private fun updateAmount(rowId: Long, value: String) {
        _state.value = stateManager.updateAmount(_state.value, rowId, value)
    }

    private fun focusRow(rowId: Long) {
        _state.value = stateManager.focusRow(_state.value, rowId)
    }

    private fun nextRow(rowId: Long) {
        _state.value = stateManager.nextRow(_state.value, rowId)
    }

    private fun deleteRow(rowId: Long) {
        _state.value = stateManager.deleteRow(_state.value, rowId)
    }

    private fun selectPaymentMethod(method: PaymentMethod) {
        _state.value = stateManager.selectPaymentMethod(_state.value, method)
    }

    private fun saveSale(cashierId: String = "default") {
        val currentState = _state.value
        
        val items = stateManager.getItems(currentState)

        if (items.isEmpty()) {
            _state.value = stateManager.setError(currentState, "Add at least one item")
            return
        }

        if (items.any { it <= 0 }) {
            _state.value = stateManager.setError(currentState, "All items must have a positive amount")
            return
        }

        val total = items.sum()
        val paymentMethod = currentState.paymentMethod
        val currentRecentSales = currentState.recentSales

        Log.d(TAG, "💾 Saving sale: items=$items, total=$total, payment=$paymentMethod")

        // Convert items to SaleItem list
        val saleItems = items.map { amount ->
            SaleItem(
                productId = "", // For simple sales, no product linked
                quantity = 1,
                price = amount,
                total = amount
            )
        }

        _state.value = stateManager.resetState().copy(
            isSaving = false,
            saveSuccess = true,
            recentSales = currentRecentSales,
            paymentMethod = paymentMethod
        )

        viewModelScope.launch {
            try {
                val sale = Sale(
                    items = saleItems,
                    total = total,
                    paymentMethod = paymentMethod,
                    cashierId = cashierId
                )

                Log.d(TAG, "💾 Saving sale in background: $sale")
                val result = repository.saveSale(sale)
                if (result is com.akari.retailer.core.utils.Result.Error) {
                    Log.e(TAG, "❌ Save error: ${result.message}")
                    _state.value = _state.value.copy(
                        error = result.message
                    )
                } else {
                    Log.d(TAG, "✅ Sale saved: ${result.getOrNull()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Save error: ${e.message}")
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to save sale"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = stateManager.setError(_state.value, null)
    }

    private fun resetSaveSuccess() {
        _state.value = stateManager.setSaveSuccess(_state.value, false)
    }
}
