package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import com.akari.retailer.features.customer.domain.models.CreditTransactionType
import com.akari.retailer.features.customer.domain.models.Customer
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.usecases.GetCreditTransactionsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CreditHistoryFilter {
    ALL, SALES, PAYMENTS
}

data class CreditHistoryState(
    val customer: Customer? = null,
    val allTransactions: List<CreditTransaction> = emptyList(),
    val filteredTransactions: List<CreditTransaction> = emptyList(),
    val filter: CreditHistoryFilter = CreditHistoryFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Summary
    val totalCreditGiven: Int = 0,
    val totalPaid: Int = 0,
    val currentBalance: Int = 0
)

class CreditHistoryViewModel(
    private val customerRepository: CustomerRepository,
    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,
    private val customerId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CreditHistoryState())
    val state: StateFlow<CreditHistoryState> = _state.asStateFlow()

    private var loadCustomerJob: Job? = null
    private var loadTxnsJob: Job? = null

    init {
        loadCustomer()
        loadTransactions()
    }

    fun setFilter(filter: CreditHistoryFilter) {
        _state.value = _state.value.copy(filter = filter)
        applyFilter()
    }

    private fun loadCustomer() {
        loadCustomerJob?.cancel()
        loadCustomerJob = viewModelScope.launch {
            try {
                customerRepository.getCustomerById(customerId).collect { customer ->
                    _state.value = _state.value.copy(customer = customer)
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
                getCreditTransactionsUseCase.forCustomer(customerId).collect { txns ->
                    val sortedDesc = txns.sortedByDescending { it.date }
                    val totalGiven = txns.filter { it.type == CreditTransactionType.SALE_ON_CREDIT }
                        .sumOf { it.amount }
                    val totalPaid = txns.filter {
                        it.type == CreditTransactionType.PAYMENT ||
                        it.type == CreditTransactionType.REFUND
                    }.sumOf { -it.amount }  // both are negative

                    _state.value = _state.value.copy(
                        allTransactions = sortedDesc,
                        totalCreditGiven = totalGiven,
                        totalPaid = totalPaid,
                        currentBalance = totalGiven - totalPaid,
                        isLoading = false
                    )
                    applyFilter()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load history"
                )
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (_state.value.filter) {
            CreditHistoryFilter.ALL -> _state.value.allTransactions
            CreditHistoryFilter.SALES -> _state.value.allTransactions.filter {
                it.type == CreditTransactionType.SALE_ON_CREDIT
            }
            CreditHistoryFilter.PAYMENTS -> _state.value.allTransactions.filter {
                it.type == CreditTransactionType.PAYMENT ||
                it.type == CreditTransactionType.REFUND
            }
        }
        _state.value = _state.value.copy(filteredTransactions = filtered)
    }
}

class CreditHistoryViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,
    private val customerId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreditHistoryViewModel::class.java)) {
            return CreditHistoryViewModel(customerRepository, getCreditTransactionsUseCase, customerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
