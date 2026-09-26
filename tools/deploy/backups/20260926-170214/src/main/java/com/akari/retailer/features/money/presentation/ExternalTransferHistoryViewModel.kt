package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.FeeType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExternalTransferHistoryViewModel(
    private val transactionRepository: MoneyTransactionRepository,
    private val accountRepository: MoneyAccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExternalTransferHistoryState())
    val state: StateFlow<ExternalTransferHistoryState> = _state.asStateFlow()

    private var loadTransfersJob: Job? = null
    private var loadAccountsJob: Job? = null

    init {
        loadAccounts()
        loadTransfers()
    }

    fun handleEvent(event: ExternalTransferHistoryEvent) {
        when (event) {
            is ExternalTransferHistoryEvent.LoadTransfers -> loadTransfers()
            is ExternalTransferHistoryEvent.RefreshTransfers -> loadTransfers()
            is ExternalTransferHistoryEvent.TimeFilterChanged -> {
                _state.value = _state.value.copy(timeFilter = event.filter)
                applyFilters()
            }
            is ExternalTransferHistoryEvent.AccountFilterChanged -> {
                _state.value = _state.value.copy(selectedAccountId = event.accountId)
                applyFilters()
            }
            is ExternalTransferHistoryEvent.ExternalFilterChanged -> {
                _state.value = _state.value.copy(selectedExternalName = event.externalName)
                applyFilters()
            }
            is ExternalTransferHistoryEvent.ClearFilters -> {
                _state.value = _state.value.copy(
                    selectedAccountId = "",
                    selectedExternalName = ""
                )
                applyFilters()
            }
            is ExternalTransferHistoryEvent.ClearError ->
                _state.value = _state.value.copy(error = null)
        }
    }

    private fun loadAccounts() {
        loadAccountsJob?.cancel()
        loadAccountsJob = viewModelScope.launch {
            try {
                accountRepository.getAccounts().collect { accounts ->
                    _state.value = _state.value.copy(accounts = accounts.filter { it.isActive })
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadTransfers() {
        loadTransfersJob?.cancel()
        loadTransfersJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                transactionRepository.getExternalTransfers().collect { transfers ->
                    val distinctExternalNames = transfers
                        .map { it.externalAccountName }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()

                    _state.value = _state.value.copy(
                        transfers = transfers,
                        externalAccountNames = distinctExternalNames,
                        isLoading = false,
                        error = null
                    )
                    applyFilters()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load external transfers"
                )
            }
        }
    }

    private fun applyFilters() {
        val s = _state.value
        val range = s.timeFilter.resolveRange()

        var filtered = s.transfers.filter { it.date in range.first..range.last }

        // Account (route) filter — matches whichever side of the transfer uses our account
        if (s.selectedAccountId.isNotEmpty()) {
            filtered = filtered.filter { txn ->
                txn.fromAccountId == s.selectedAccountId ||
                txn.toAccountId == s.selectedAccountId
            }
        }

        // External account name filter
        if (s.selectedExternalName.isNotEmpty()) {
            filtered = filtered.filter { it.externalAccountName == s.selectedExternalName }
        }

        val sentTxns = filtered.filter { it.type == MoneyTransactionType.EXTERNAL_OUT }
        val receivedTxns = filtered.filter { it.type == MoneyTransactionType.EXTERNAL_IN }

        val totalSent = sentTxns.sumOf { it.amount }
        val totalReceived = receivedTxns.sumOf { it.amount }

        val feesPaid = filtered
            .filter { it.feeType == FeeType.FEE_PAID }
            .sumOf { it.fee }
        val feesEarned = filtered
            .filter { it.feeType == FeeType.FEE_EARNED }
            .sumOf { it.fee }

        val grouped = filtered.groupBy { txn ->
            txn.externalAccountName.ifEmpty { txn.externalAccountNumber.ifEmpty { "Unknown" } }
        }

        val byAccount = grouped.map { (name, txns) ->
            val sent = txns.filter { it.type == MoneyTransactionType.EXTERNAL_OUT }.sumOf { it.amount }
            val received = txns.filter { it.type == MoneyTransactionType.EXTERNAL_IN }.sumOf { it.amount }
            ExternalAccountSummary(
                name = name,
                accountNumber = txns.firstOrNull()?.externalAccountNumber ?: "",
                sent = sent,
                received = received,
                count = txns.size
            )
        }.sortedByDescending { it.sent + it.received }

        _state.value = _state.value.copy(
            filteredTransfers = filtered,
            totalSent = totalSent,
            totalReceived = totalReceived,
            sentCount = sentTxns.size,
            receivedCount = receivedTxns.size,
            feesPaid = feesPaid,
            feesEarned = feesEarned,
            netFlow = totalReceived - totalSent,
            byExternalAccount = byAccount,
            isLoading = false
        )
    }
}

class ExternalTransferHistoryViewModelFactory(
    private val transactionRepository: MoneyTransactionRepository,
    private val accountRepository: MoneyAccountRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalTransferHistoryViewModel::class.java)) {
            return ExternalTransferHistoryViewModel(transactionRepository, accountRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
