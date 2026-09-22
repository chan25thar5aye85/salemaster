package com.akari.retailer.features.money.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
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
            is MoneyAnalyticsEvent.TimeRangeChanged -> {
                _state.value = _state.value.copy(timeRange = event.range)
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
        val (startDate, endDate, rangeLabel) = getDateRange()
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
            rangeLabel = rangeLabel,
            isLoading = false,
            error = null
        )
    }

    private fun getDateRange(): Triple<Long, Long, String> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        return when (_state.value.timeRange) {
            MoneyAnalyticsTimeRange.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Triple(calendar.timeInMillis, now, "Today")
            }
            MoneyAnalyticsTimeRange.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Triple(calendar.timeInMillis, now, "This Week")
            }
            MoneyAnalyticsTimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Triple(calendar.timeInMillis, now, "This Month")
            }
            MoneyAnalyticsTimeRange.LAST_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                Triple(start, calendar.timeInMillis, "Last Month")
            }
            MoneyAnalyticsTimeRange.ALL -> Triple(0L, now, "All Time")
        }
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
