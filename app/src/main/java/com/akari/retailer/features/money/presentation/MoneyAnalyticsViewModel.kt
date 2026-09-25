package com.akari.retailer.features.money.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class MoneyAnalyticsViewModel(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyAnalyticsState())
    val state: StateFlow<MoneyAnalyticsState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAnalytics()
    }

    fun handleEvent(event: MoneyAnalyticsEvent) {
        when (event) {
            is MoneyAnalyticsEvent.LoadAnalytics -> loadAnalytics()
            is MoneyAnalyticsEvent.RefreshAnalytics -> loadAnalytics()
            is MoneyAnalyticsEvent.TimeFilterChanged -> {
                _state.value = _state.value.copy(timeFilter = event.filter)
                loadAnalytics()
            }
            is MoneyAnalyticsEvent.ClearError -> _state.value = _state.value.copy(error = null)
        }
    }

    private fun loadAnalytics() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                combine(
                    accountRepository.getAccounts(),
                    transactionRepository.getTransactions()
                ) { accounts, transactions ->
                    Pair(accounts, transactions)
                }.collect { (accounts, transactions) ->
                    calculateAnalytics(accounts, transactions)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics"
                )
            }
        }
    }

    private fun calculateAnalytics(
        accounts: List<MoneyAccount>,
        transactions: List<MoneyTransaction>
    ) {
        val range = _state.value.timeFilter.resolveRange()
        val startDate = range.first
        val endDate = range.last
        val filteredTransactions = transactions.filter { it.date in startDate..endDate }

        val activeAccounts = accounts.filter { it.isActive }
        val totalBalance = activeAccounts.sumOf { it.currentBalance }

        val accountBalances = activeAccounts.map { account ->
            AccountBalance(
                account = account,
                balance = account.currentBalance,
                percentage = if (totalBalance > 0) {
                    (account.currentBalance.toDouble() / totalBalance) * 100
                } else 0.0
            )
        }.sortedByDescending { it.balance }

        val totalFeePaid = filteredTransactions
            .filter { it.feeType == FeeType.FEE_PAID }.sumOf { it.fee }
        val totalFeeEarned = filteredTransactions
            .filter { it.feeType == FeeType.FEE_EARNED }.sumOf { it.fee }

        val feeSummary = FeeSummary(
            totalPaid = totalFeePaid,
            totalEarned = totalFeeEarned,
            netFee = totalFeeEarned - totalFeePaid,
            feesByType = mapOf(
                "Fee Paid" to totalFeePaid,
                "Fee Earned" to totalFeeEarned
            )
        )

        // Internal transfers (TRANSFER_IN / TRANSFER_OUT) are excluded from
        // money-in/out summary — they move money between the user's own
        // accounts and would otherwise inflate both gross numbers.
        val totalIn = filteredTransactions
            .filter {
                it.type in listOf(
                    MoneyTransactionType.SALE_IN,
                    MoneyTransactionType.INCOME_IN,
                    MoneyTransactionType.EXTERNAL_IN,
                    MoneyTransactionType.FEE_IN
                )
            }.sumOf { it.amount }

        val totalOut = filteredTransactions
            .filter {
                it.type in listOf(
                    MoneyTransactionType.EXPENSE_OUT,
                    MoneyTransactionType.PURCHASE_OUT,
                    MoneyTransactionType.EXTERNAL_OUT,
                    MoneyTransactionType.FEE_OUT
                )
            }.sumOf { it.amount }

        val moneyFlow = MoneyFlow(
            totalIn = totalIn,
            totalOut = totalOut,
            netFlow = totalIn - totalOut,
            transactionCount = filteredTransactions.size
        )

        _state.value = _state.value.copy(
            accounts = activeAccounts,
            accountBalances = accountBalances,
            totalBalance = totalBalance,
            feeSummary = feeSummary,
            moneyFlow = moneyFlow,
            isLoading = false,
            error = null
        )
    }

}

class MoneyAnalyticsViewModelFactory(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoneyAnalyticsViewModel::class.java)) {
            return MoneyAnalyticsViewModel(accountRepository, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
