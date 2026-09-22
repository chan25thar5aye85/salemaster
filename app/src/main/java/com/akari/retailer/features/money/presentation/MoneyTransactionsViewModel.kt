package com.akari.retailer.features.money.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class MoneyTransactionsViewModel(
    private val transactionRepository: MoneyTransactionRepository,
    private val accountRepository: MoneyAccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyTransactionsState())
    val state: StateFlow<MoneyTransactionsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun handleEvent(event: MoneyTransactionsEvent) {
        when (event) {
            is MoneyTransactionsEvent.LoadTransactions -> loadData()
            is MoneyTransactionsEvent.RefreshTransactions -> loadData()
            is MoneyTransactionsEvent.ClearError -> _state.value = _state.value.copy(error = null)
            is MoneyTransactionsEvent.AccountFilterChanged -> {
                _state.value = _state.value.copy(selectedAccountId = event.accountId)
                applyFilters()
            }
            is MoneyTransactionsEvent.TypeFilterChanged -> {
                _state.value = _state.value.copy(selectedType = event.type)
                applyFilters()
            }
            is MoneyTransactionsEvent.TimeRangeChanged -> {
                _state.value = _state.value.copy(timeRange = event.range)
                applyFilters()
            }
            is MoneyTransactionsEvent.SearchQueryChanged -> {
                _state.value = _state.value.copy(searchQuery = event.query)
                applyFilters()
            }
            is MoneyTransactionsEvent.ClearFilters -> {
                _state.value = _state.value.copy(
                    selectedAccountId = "",
                    selectedType = null,
                    timeRange = TransactionTimeRange.ALL,
                    searchQuery = ""
                )
                applyFilters()
            }
            is MoneyTransactionsEvent.ToggleFilterSheet -> {
                _state.value = _state.value.copy(showFilterSheet = !_state.value.showFilterSheet)
            }
        }
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                combine(
                    transactionRepository.getTransactions(),
                    accountRepository.getAccounts()
                ) { transactions, accounts ->
                    Pair(transactions, accounts)
                }.collect { (transactions, accounts) ->
                    _state.value = _state.value.copy(
                        transactions = transactions,
                        accounts = accounts,
                        isLoading = false,
                        error = null
                    )
                    applyFilters()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load transactions"
                )
            }
        }
    }

    private fun applyFilters() {
        val state = _state.value
        var filtered = state.transactions

        if (state.selectedAccountId.isNotEmpty()) {
            filtered = filtered.filter {
                it.fromAccountId == state.selectedAccountId ||
                it.toAccountId == state.selectedAccountId
            }
        }

        state.selectedType?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        val now = System.currentTimeMillis()
        val startDate = when (state.timeRange) {
            TransactionTimeRange.TODAY -> Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TransactionTimeRange.THIS_WEEK -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TransactionTimeRange.THIS_MONTH -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TransactionTimeRange.ALL -> 0L
        }
        filtered = filtered.filter { it.date in startDate..now }

        if (state.searchQuery.isNotEmpty()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.description.lowercase().contains(query) ||
                it.externalAccountName.lowercase().contains(query)
            }
        }

        val totalIn = filtered.filter {
            it.type in listOf(
                MoneyTransactionType.SALE_IN,
                MoneyTransactionType.INCOME_IN,
                MoneyTransactionType.TRANSFER_IN,
                MoneyTransactionType.EXTERNAL_IN,
                MoneyTransactionType.FEE_IN
            )
        }.sumOf { it.amount }

        val totalOut = filtered.filter {
            it.type in listOf(
                MoneyTransactionType.EXPENSE_OUT,
                MoneyTransactionType.PURCHASE_OUT,
                MoneyTransactionType.TRANSFER_OUT,
                MoneyTransactionType.EXTERNAL_OUT,
                MoneyTransactionType.FEE_OUT
            )
        }.sumOf { it.amount }

        val totalFeePaid = filtered.filter { it.feeType == FeeType.FEE_PAID }.sumOf { it.fee }
        val totalFeeEarned = filtered.filter { it.feeType == FeeType.FEE_EARNED }.sumOf { it.fee }

        _state.value = _state.value.copy(
            filteredTransactions = filtered,
            totalIn = totalIn,
            totalOut = totalOut,
            totalFeePaid = totalFeePaid,
            totalFeeEarned = totalFeeEarned
        )
    }
}

class MoneyTransactionsViewModelFactory(
    private val transactionRepository: MoneyTransactionRepository,
    private val accountRepository: MoneyAccountRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoneyTransactionsViewModel::class.java)) {
            return MoneyTransactionsViewModel(transactionRepository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
