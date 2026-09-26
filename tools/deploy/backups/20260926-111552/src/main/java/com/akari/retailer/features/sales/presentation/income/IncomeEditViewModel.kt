package com.akari.retailer.features.sales.presentation.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.sales.data.remote.FirestoreIncomeFinalizer
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.data.repository.IncomeStreamRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.features.sales.domain.models.IncomeStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class IncomeEditState(
    val id: String = "",
    val amount: String = "",
    val selectedStream: IncomeStream? = null,
    val streams: List<IncomeStream> = emptyList(),
    val accounts: List<MoneyAccount> = emptyList(),
    val paymentRows: List<PaymentRow> = listOf(
        PaymentRow(id = 1L, accountId = "default_cash", amount = "")
    ),
    val entryType: IncomeEntryType = IncomeEntryType.BUSINESS,
    val description: String = "",
    val originalDate: Long = System.currentTimeMillis(),
    val originalCreatedAt: Long = System.currentTimeMillis(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    fun getTotalAmount(): Int = amount.toIntOrNull() ?: 0
    fun getTotalPaid(): Int = paymentRows.sumOf { it.amount.toIntOrNull() ?: 0 }
    fun isFullyPaid(): Boolean = getTotalPaid() == getTotalAmount() && getTotalAmount() > 0

    fun getPayments(): List<PaymentEntry> = paymentRows
        .filter { it.amount.toIntOrNull()?.let { v -> v > 0 } == true }
        .map { PaymentEntry(it.accountId, it.amount.toIntOrNull() ?: 0) }
}

sealed class IncomeEditEvent {
    data class AmountChanged(val value: String) : IncomeEditEvent()
    data class StreamSelected(val stream: IncomeStream) : IncomeEditEvent()
    data class EntryTypeChanged(val type: IncomeEntryType) : IncomeEditEvent()
    data class DescriptionChanged(val value: String) : IncomeEditEvent()
    data class PaymentAccountChanged(val rowId: Long, val account: MoneyAccount) : IncomeEditEvent()
    data class PaymentAmountChanged(val rowId: Long, val amount: String) : IncomeEditEvent()
    data object AddPaymentRow : IncomeEditEvent()
    data class RemovePaymentRow(val rowId: Long) : IncomeEditEvent()
    data object LoadEntry : IncomeEditEvent()
    data object SaveEntry : IncomeEditEvent()
    data object ClearError : IncomeEditEvent()
    data object ResetSuccess : IncomeEditEvent()
}

class IncomeEditViewModel(
    private val entryRepository: IncomeEntryRepository,
    private val streamRepository: IncomeStreamRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val incomeFinalizer: FirestoreIncomeFinalizer,
    private val paymentPreferences: PaymentPreferences,
    private val entryId: String
) : ViewModel() {

    private val _state = MutableStateFlow(IncomeEditState(id = entryId))
    val state: StateFlow<IncomeEditState> = _state.asStateFlow()

    private var loadEntryJob: Job? = null
    private var loadStreamsJob: Job? = null
    private var loadAccountsJob: Job? = null

    private var nextPaymentRowId = 2L
    private var userTouchedAmounts = false

    init {
        loadStreams()
        loadAccounts()
        loadEntry()
    }

    fun handleEvent(event: IncomeEditEvent) {
        when (event) {
            is IncomeEditEvent.AmountChanged -> handleAmountChanged(event.value)
            is IncomeEditEvent.StreamSelected -> _state.value = _state.value.copy(selectedStream = event.stream)
            is IncomeEditEvent.EntryTypeChanged -> _state.value = _state.value.copy(entryType = event.type)
            is IncomeEditEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is IncomeEditEvent.PaymentAccountChanged -> updatePaymentAccount(event.rowId, event.account)
            is IncomeEditEvent.PaymentAmountChanged -> updatePaymentAmount(event.rowId, event.amount)
            is IncomeEditEvent.AddPaymentRow -> addPaymentRow()
            is IncomeEditEvent.RemovePaymentRow -> removePaymentRow(event.rowId)
            IncomeEditEvent.LoadEntry -> loadEntry()
            IncomeEditEvent.SaveEntry -> saveEntry()
            IncomeEditEvent.ClearError -> _state.value = _state.value.copy(error = null)
            IncomeEditEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadStreams() {
        loadStreamsJob?.cancel()
        loadStreamsJob = viewModelScope.launch {
            try {
                streamRepository.getIncomeStreams().collect { streams ->
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
                    _state.value = _state.value.copy(accounts = accounts.filter { it.isActive })
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadEntry() {
        loadEntryJob?.cancel()
        loadEntryJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val entry = entryRepository.getIncomeEntryById(entryId).first()
                if (entry == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Income entry not found"
                    )
                    return@launch
                }

                val stream = _state.value.streams.find { it.id == entry.incomeStreamId }

                val rows = if (entry.payments.isNotEmpty()) {
                    entry.payments.mapIndexed { index, p ->
                        PaymentRow(
                            id = (index + 1).toLong(),
                            accountId = p.accountId,
                            amount = p.amount.toString()
                        )
                    }
                } else {
                    listOf(PaymentRow(id = 1L, accountId = "default_cash", amount = entry.amount.toString()))
                }
                nextPaymentRowId = (rows.maxOfOrNull { it.id } ?: 1L) + 1
                userTouchedAmounts = true

                _state.value = _state.value.copy(
                    id = entry.id,
                    amount = entry.amount.toString(),
                    selectedStream = stream,
                    paymentRows = rows,
                    entryType = entry.type,
                    description = entry.description,
                    originalDate = entry.date,
                    originalCreatedAt = entry.createdAt,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load income entry"
                )
            }
        }
    }

    private fun handleAmountChanged(value: String) {
        val previousTotal = _state.value.getTotalAmount()
        val digitsOnly = value.filter { it.isDigit() }
        _state.value = _state.value.copy(amount = digitsOnly)
        syncSinglePaymentRow(previousTotal)
    }

    private fun syncSinglePaymentRow(previousTotal: Int) {
        val rows = _state.value.paymentRows
        if (rows.size != 1) return

        val currentRowAmount = rows[0].amount.toIntOrNull() ?: 0
        val wasAutoFilled = rows[0].amount.isEmpty() || currentRowAmount == previousTotal

        if (wasAutoFilled) {
            val newTotal = _state.value.getTotalAmount()
            _state.value = _state.value.copy(
                paymentRows = listOf(rows[0].copy(amount = newTotal.toString()))
            )
            userTouchedAmounts = false
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
        val digitsOnly = amount.filter { it.isDigit() }
        val updated = _state.value.paymentRows.map { row ->
            if (row.id == rowId) row.copy(amount = digitsOnly) else row
        }
        _state.value = _state.value.copy(paymentRows = updated)
        userTouchedAmounts = true
    }

    private fun addPaymentRow() {
        val remaining = _state.value.getTotalAmount() - _state.value.getTotalPaid()
        val lastAccountId = _state.value.paymentRows.lastOrNull()?.accountId ?: "default_cash"
        val newRow = PaymentRow(
            id = nextPaymentRowId++,
            accountId = lastAccountId,
            amount = if (remaining > 0) remaining.toString() else ""
        )
        _state.value = _state.value.copy(paymentRows = _state.value.paymentRows + newRow)
    }

    private fun removePaymentRow(rowId: Long) {
        if (_state.value.paymentRows.size <= 1) return
        val previousTotal = _state.value.getTotalAmount()
        _state.value = _state.value.copy(
            paymentRows = _state.value.paymentRows.filter { it.id != rowId }
        )
        syncSinglePaymentRow(previousTotal)
    }

    private fun saveEntry() {
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

            val updated = IncomeEntry(
                id = currentState.id,
                amount = amountInt,
                incomeStreamId = currentState.selectedStream.id,
                payments = payments,
                description = currentState.description.trim(),
                type = currentState.entryType,
                date = currentState.originalDate,
                createdAt = currentState.originalCreatedAt,
                updatedAt = System.currentTimeMillis()
            )

            val result = incomeFinalizer.updateIncome(updated)

            if (result.isSuccess) {
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
                    error = result.exceptionOrNull()?.message ?: "Failed to update income entry"
                )
            }
        }
    }
}

class IncomeEditViewModelFactory(
    private val entryRepository: IncomeEntryRepository,
    private val streamRepository: IncomeStreamRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val incomeFinalizer: FirestoreIncomeFinalizer,
    private val paymentPreferences: PaymentPreferences,
    private val entryId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IncomeEditViewModel::class.java)) {
            return IncomeEditViewModel(
                entryRepository,
                streamRepository,
                moneyAccountRepository,
                incomeFinalizer,
                paymentPreferences,
                entryId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
