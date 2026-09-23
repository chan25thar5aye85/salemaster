package com.akari.retailer.features.sales.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.money.domain.usecases.ProcessMoneyTransactionUseCase
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.features.sales.domain.models.IncomeStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncomeEntryState(
    val amount: String = "",
    val selectedStream: IncomeStream? = null,
    val streams: List<IncomeStream> = emptyList(),
    val accounts: List<MoneyAccount> = emptyList(),
    val paymentRows: List<PaymentRow> = listOf(
        PaymentRow(id = 1L, accountId = "default_cash", amount = "")
    ),
    val entryType: IncomeEntryType = IncomeEntryType.BUSINESS,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val lastUsedAccountId: String = "default_cash",
    val userTouchedAmounts: Boolean = false
) {
    fun getTotalAmount(): Int = amount.toIntOrNull() ?: 0
    fun getTotalPaid(): Int = paymentRows.sumOf { it.amount.toIntOrNull() ?: 0 }
    fun getRemaining(): Int = getTotalAmount() - getTotalPaid()
    fun isFullyPaid(): Boolean = getTotalPaid() == getTotalAmount() && getTotalAmount() > 0

    fun getPayments(): List<PaymentEntry> {
        return paymentRows
            .filter { it.amount.toIntOrNull()?.let { it > 0 } == true }
            .map { row ->
                PaymentEntry(
                    accountId = row.accountId,
                    amount = row.amount.toIntOrNull() ?: 0
                )
            }
    }
}

sealed class IncomeEntryEvent {
    data class AmountChanged(val value: String) : IncomeEntryEvent()
    data class StreamSelected(val stream: IncomeStream) : IncomeEntryEvent()
    data class EntryTypeChanged(val type: IncomeEntryType) : IncomeEntryEvent()
    data class DescriptionChanged(val value: String) : IncomeEntryEvent()
    data class DateChanged(val date: Long) : IncomeEntryEvent()
    data class PaymentAccountChanged(val rowId: Long, val account: MoneyAccount) : IncomeEntryEvent()
    data class PaymentAmountChanged(val rowId: Long, val amount: String) : IncomeEntryEvent()
    data object AddPaymentRow : IncomeEntryEvent()
    data class RemovePaymentRow(val rowId: Long) : IncomeEntryEvent()
    data object SaveIncome : IncomeEntryEvent()
    data object ClearError : IncomeEntryEvent()
    data object ResetSuccess : IncomeEntryEvent()
}

class IncomeEntryViewModel(
    private val incomeEntryRepository: IncomeEntryRepository,
    private val incomeStreamRepository: IncomeStreamRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val processMoneyTransactionUseCase: ProcessMoneyTransactionUseCase,
    private val paymentPreferences: PaymentPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeEntryState())
    val state: StateFlow<IncomeEntryState> = _state.asStateFlow()

    private var loadStreamsJob: Job? = null
    private var loadAccountsJob: Job? = null

    private var nextRowId = 2L

    init {
        val lastAccountId = paymentPreferences.getLastUsedAccountId()
        _state.value = _state.value.copy(
            lastUsedAccountId = lastAccountId,
            paymentRows = listOf(PaymentRow(id = 1L, accountId = lastAccountId, amount = ""))
        )
        loadStreams()
        loadAccounts()
    }

    fun handleEvent(event: IncomeEntryEvent) {
        when (event) {
            is IncomeEntryEvent.AmountChanged -> handleAmountChanged(event.value)
            is IncomeEntryEvent.StreamSelected -> _state.value = _state.value.copy(selectedStream = event.stream)
            is IncomeEntryEvent.EntryTypeChanged -> _state.value = _state.value.copy(entryType = event.type)
            is IncomeEntryEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is IncomeEntryEvent.DateChanged -> _state.value = _state.value.copy(date = event.date)
            is IncomeEntryEvent.PaymentAccountChanged -> updatePaymentAccount(event.rowId, event.account)
            is IncomeEntryEvent.PaymentAmountChanged -> updatePaymentAmount(event.rowId, event.amount)
            is IncomeEntryEvent.AddPaymentRow -> addPaymentRow()
            is IncomeEntryEvent.RemovePaymentRow -> removePaymentRow(event.rowId)
            is IncomeEntryEvent.SaveIncome -> saveIncome()
            is IncomeEntryEvent.ClearError -> _state.value = _state.value.copy(error = null)
            is IncomeEntryEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadStreams() {
        loadStreamsJob?.cancel()
        loadStreamsJob = viewModelScope.launch {
            try {
                incomeStreamRepository.getIncomeStreams().collect { streams ->
                    _state.value = _state.value.copy(streams = streams)
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadAccounts() {
        loadAccountsJob?.cancel()
        loadAccountsJob = viewModelScope.launch {
            try {
                moneyAccountRepository.getAccounts().collect { accounts ->
                    val active = accounts.filter { it.isActive }
                    _state.value = _state.value.copy(accounts = active)
                }
            } catch (e: Exception) { }
        }
    }

    private fun handleAmountChanged(value: String) {
        val previousTotal = _state.value.getTotalAmount()
        val digitsOnly = value.filter { it.isDigit() }
        _state.value = _state.value.copy(amount = digitsOnly)
        syncSinglePaymentRow(previousTotal)
    }

    /**
     * If there's exactly one payment row, keep it in sync with the total —
     * but ONLY if the row still contains the previously auto-filled value.
     */
    private fun syncSinglePaymentRow(previousTotal: Int) {
        val rows = _state.value.paymentRows
        if (rows.size != 1) return

        val currentRowAmount = rows[0].amount.toIntOrNull() ?: 0
        val wasAutoFilled = rows[0].amount.isEmpty() ||
                            currentRowAmount == previousTotal

        if (wasAutoFilled) {
            val newTotal = _state.value.getTotalAmount()
            _state.value = _state.value.copy(
                paymentRows = listOf(rows[0].copy(amount = newTotal.toString())),
                userTouchedAmounts = false
            )
        }
    }

    private fun updatePaymentAccount(rowId: Long, account: MoneyAccount) {
        val updated = _state.value.paymentRows.map { row ->
            if (row.id == rowId) row.copy(accountId = account.id) else row
        }
        _state.value = _state.value.copy(paymentRows = updated)
        paymentPreferences.setLastUsedAccountId(account.id)
    }

    private fun updatePaymentAmount(rowId: Long, amount: String) {
        val updated = _state.value.paymentRows.map { row ->
            if (row.id == rowId) row.copy(amount = amount) else row
        }
        _state.value = _state.value.copy(
            paymentRows = updated,
            userTouchedAmounts = true
        )
    }

    private fun addPaymentRow() {
        val remaining = _state.value.getRemaining()
        val lastAccountId = _state.value.paymentRows.lastOrNull()?.accountId
            ?: _state.value.lastUsedAccountId
        val newRow = PaymentRow(
            id = nextRowId++,
            accountId = lastAccountId,
            amount = if (remaining > 0) remaining.toString() else ""
        )
        _state.value = _state.value.copy(
            paymentRows = _state.value.paymentRows + newRow
        )
    }

    private fun removePaymentRow(rowId: Long) {
        if (_state.value.paymentRows.size <= 1) return
        val previousTotal = _state.value.getTotalAmount()
        _state.value = _state.value.copy(
            paymentRows = _state.value.paymentRows.filter { it.id != rowId }
        )
        syncSinglePaymentRow(previousTotal)
    }

    private fun saveIncome() {
        val currentState = _state.value

        val amountInt = currentState.amount.toIntOrNull()
        if (amountInt == null || amountInt <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }

        if (currentState.selectedStream == null) {
            _state.value = _state.value.copy(error = "Select an income stream")
            return
        }

        val payments = currentState.getPayments()
        if (payments.isEmpty()) {
            _state.value = _state.value.copy(error = "Add at least one payment")
            return
        }

        if (currentState.getTotalPaid() != amountInt) {
            _state.value = _state.value.copy(
                error = "Payments (${currentState.getTotalPaid()}) must equal amount ($amountInt)"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            val entry = IncomeEntry(
                amount = amountInt,
                incomeStreamId = currentState.selectedStream.id,
                payments = payments,
                description = currentState.description.trim(),
                type = currentState.entryType,
                date = currentState.date
            )

            val result = incomeEntryRepository.addIncomeEntry(entry)

            if (result.isSuccess) {
                val incomeId = result.getOrNull() ?: ""
                processMoneyTransactionUseCase.processIncome(
                    payments = payments,
                    incomeId = incomeId,
                    description = currentState.selectedStream.name
                )

                payments.firstOrNull()?.let {
                    paymentPreferences.setLastUsedAccountId(it.accountId)
                }

                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to save income"
                )
            }
        }
    }
}

class IncomeEntryViewModelFactory(
    private val incomeEntryRepository: IncomeEntryRepository,
    private val incomeStreamRepository: IncomeStreamRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val processMoneyTransactionUseCase: ProcessMoneyTransactionUseCase,
    private val paymentPreferences: PaymentPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeEntryViewModel::class.java)) {
            return IncomeEntryViewModel(
                incomeEntryRepository,
                incomeStreamRepository,
                moneyAccountRepository,
                processMoneyTransactionUseCase,
                paymentPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
