package com.akari.retailer.features.supplier.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import com.akari.retailer.features.supplier.domain.models.SupplierTransactionType
import com.akari.retailer.features.supplier.domain.usecases.GetSupplierTransactionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SupplierPayableFilter {
    ALL, PURCHASES, PAYMENTS
}

data class SupplierPayableHistoryState(
    val supplier: Supplier? = null,
    val allTransactions: List<SupplierTransaction> = emptyList(),
    val filteredTransactions: List<SupplierTransaction> = emptyList(),
    val filter: SupplierPayableFilter = SupplierPayableFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Summary
    val totalCreditReceived: Int = 0,   // sum of PURCHASE_ON_CREDIT
    val totalPaid: Int = 0,             // sum of |PAYMENT|
    val currentBalance: Int = 0
)

class SupplierPayableHistoryViewModel(
    private val supplierRepository: SupplierRepository,
    private val getSupplierTransactionsUseCase: GetSupplierTransactionsUseCase,
    private val supplierId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierPayableHistoryState())
    val state: StateFlow<SupplierPayableHistoryState> = _state.asStateFlow()

    private var loadSupplierJob: Job? = null
    private var loadTxnsJob: Job? = null

    init {
        loadSupplier()
        loadTransactions()
    }

    fun setFilter(filter: SupplierPayableFilter) {
        _state.value = _state.value.copy(filter = filter)
        applyFilter()
    }

    private fun loadSupplier() {
        loadSupplierJob?.cancel()
        loadSupplierJob = viewModelScope.launch {
            try {
                supplierRepository.getSupplierById(supplierId).collect { supplier ->
                    _state.value = _state.value.copy(supplier = supplier)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    private fun loadTransactions() {
        loadTxnsJob?.cancel()
        loadTxnsJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                getSupplierTransactionsUseCase.forSupplier(supplierId).collect { txns ->
                    val sortedDesc = txns.sortedByDescending { it.date }

                    val totalCredit = txns
                        .filter { it.type == SupplierTransactionType.PURCHASE_ON_CREDIT }
                        .sumOf { it.amount }
                    val totalPaid = txns
                        .filter { it.type == SupplierTransactionType.PAYMENT }
                        .sumOf { -it.amount }

                    _state.value = _state.value.copy(
                        allTransactions = sortedDesc,
                        totalCreditReceived = totalCredit,
                        totalPaid = totalPaid,
                        currentBalance = totalCredit - totalPaid,
                        isLoading = false
                    )
                    applyFilter()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load payables"
                )
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (_state.value.filter) {
            SupplierPayableFilter.ALL -> _state.value.allTransactions
            SupplierPayableFilter.PURCHASES -> _state.value.allTransactions.filter {
                it.type == SupplierTransactionType.PURCHASE_ON_CREDIT
            }
            SupplierPayableFilter.PAYMENTS -> _state.value.allTransactions.filter {
                it.type == SupplierTransactionType.PAYMENT
            }
        }
        _state.value = _state.value.copy(filteredTransactions = filtered)
    }
}

class SupplierPayableHistoryViewModelFactory(
    private val supplierRepository: SupplierRepository,
    private val getSupplierTransactionsUseCase: GetSupplierTransactionsUseCase,
    private val supplierId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierPayableHistoryViewModel::class.java)) {
            return SupplierPayableHistoryViewModel(
                supplierRepository,
                getSupplierTransactionsUseCase,
                supplierId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
