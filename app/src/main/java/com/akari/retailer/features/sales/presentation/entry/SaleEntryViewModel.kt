package com.akari.retailer.features.sales.presentation.entry

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.money.domain.usecases.ProcessMoneyTransactionUseCase
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.features.sales.domain.models.SaleItem
import com.akari.retailer.core.utils.MoneyFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SaleEntryViewModel(
    private val repository: SaleRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val processMoneyTransactionUseCase: ProcessMoneyTransactionUseCase,
    private val paymentPreferences: PaymentPreferences
) : ViewModel() {

    private val TAG = "SaleEntryViewModel"
    private val stateManager = SaleEntryStateManager()

    private val _state = MutableStateFlow(SaleEntryState())
    val state: StateFlow<SaleEntryState> = _state.asStateFlow()

    private var loadAccountsJob: Job? = null
    private var loadRecentSalesJob: Job? = null

    private var nextPaymentRowId = 2L
    private var userTouchedPaymentAmounts = false

    init {
        val lastAccountId = paymentPreferences.getLastUsedAccountId()
        _state.value = _state.value.copy(
            paymentRows = listOf(PaymentRow(id = 1L, accountId = lastAccountId, amount = ""))
        )
        loadRecentSales()
        loadAccounts()
    }

    fun handleEvent(event: SaleEntryEvent) {
        when (event) {
            is SaleEntryEvent.AmountChanged -> {
                val previousTotal = getTotal()
                updateAmount(event.rowId, event.value)
                syncSinglePaymentRow(previousTotal)
            }
            is SaleEntryEvent.RowFocused -> focusRow(event.rowId)
            is SaleEntryEvent.NextPressed -> nextRow(event.rowId)
            is SaleEntryEvent.RowDeleted -> {
                val previousTotal = getTotal()
                deleteRow(event.rowId)
                syncSinglePaymentRow(previousTotal)
            }
            is SaleEntryEvent.PaymentAccountChanged -> {
                updatePaymentAccount(event.rowId, event.account)
                paymentPreferences.setLastUsedAccountId(event.account.id)
            }
            is SaleEntryEvent.PaymentAmountChanged -> {
                updatePaymentAmount(event.rowId, event.amount)
                userTouchedPaymentAmounts = true
            }
            is SaleEntryEvent.AddPaymentRow -> addPaymentRow()
            is SaleEntryEvent.RemovePaymentRow -> removePaymentRow(event.rowId)
            SaleEntryEvent.SaveSale -> saveSale()
            SaleEntryEvent.ClearError -> clearError()
            SaleEntryEvent.ResetSaveSuccess -> resetSaveSuccess()
        }
    }

    fun getTotal(): Int = stateManager.getItems(_state.value).sum()
    fun getFormattedTotal(): String = MoneyFormatter.formatTotal(getTotal())

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

    private fun loadRecentSales() {
        loadRecentSalesJob?.cancel()
        loadRecentSalesJob = viewModelScope.launch {
            try {
                repository.getSales().collect { sales ->
                    _state.value = _state.value.copy(recentSales = sales.take(2))
                }
            } catch (e: Exception) { }
        }
    }

    /**
     * If there's exactly one payment row, keep it in sync with the total —
     * but ONLY if the row still contains the previously auto-filled value
     * (i.e., the user hasn't manually diverged it).
     *
     * Fixes: after deleting a 2nd row and going back to 1 row, the payment
     * row used to stay stale, blocking save.
     */
    private fun syncSinglePaymentRow(previousTotal: Int) {
        val rows = _state.value.paymentRows
        if (rows.size != 1) return

        val currentRowAmount = rows[0].amount.toIntOrNull() ?: 0
        val wasAutoFilled = rows[0].amount.isEmpty() ||
                            currentRowAmount == previousTotal

        if (wasAutoFilled) {
            val newTotal = getTotal()
            _state.value = _state.value.copy(
                paymentRows = listOf(rows[0].copy(amount = newTotal.toString()))
            )
            userTouchedPaymentAmounts = false
        }
    }

    /**
     * If there's exactly one payment row and the user hasn't manually
     * diverged it, keep it in sync with the current total.
     *
     * When [removePaymentRow] brings us back to 1 row, it resets
     * [userTouchedPaymentAmounts] so future amount changes re-enable auto-fill.
     */
    private fun autoSyncSinglePaymentRow() {
        if (userTouchedPaymentAmounts) return
        val rows = _state.value.paymentRows
        if (rows.size != 1) return
        val total = getTotal()
        _state.value = _state.value.copy(
            paymentRows = listOf(rows[0].copy(amount = total.toString()))
        )
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

    private fun updatePaymentAccount(rowId: Long, account: MoneyAccount) {
        val updated = _state.value.paymentRows.map { row ->
            if (row.id == rowId) row.copy(accountId = account.id) else row
        }
        _state.value = _state.value.copy(paymentRows = updated)
    }

    private fun updatePaymentAmount(rowId: Long, amount: String) {
        val updated = _state.value.paymentRows.map { row ->
            if (row.id == rowId) row.copy(amount = amount) else row
        }
        _state.value = _state.value.copy(paymentRows = updated)
    }

    private fun addPaymentRow() {
        val total = getTotal()
        val totalPaid = _state.value.paymentRows.sumOf { it.amount.toIntOrNull() ?: 0 }
        val remaining = total - totalPaid
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
        val updated = _state.value.paymentRows.filter { it.id != rowId }
        // When we're back to a single row, treat it as fresh so auto-sync resumes.
        if (updated.size == 1) userTouchedPaymentAmounts = false
        _state.value = _state.value.copy(paymentRows = updated)
    }

    private fun saveSale(cashierId: String = "default") {
        val currentState = _state.value
        val items = stateManager.getItems(currentState)

        if (items.isEmpty()) {
            _state.value = stateManager.setError(currentState, "Add at least one item")
            return
        }

        val total = items.sum()

        val payments = currentState.paymentRows
            .filter { it.amount.toIntOrNull()?.let { it > 0 } == true }
            .map { PaymentEntry(it.accountId, it.amount.toIntOrNull() ?: 0) }

        if (payments.isEmpty()) {
            _state.value = stateManager.setError(currentState, "Add at least one payment")
            return
        }

        if (payments.sumOf { it.amount } != total) {
            _state.value = stateManager.setError(
                currentState,
                "Payments (${payments.sumOf { it.amount }}) must equal total ($total)"
            )
            return
        }

        val currentRecentSales = currentState.recentSales
        val lastAccountId = currentState.paymentRows.firstOrNull()?.accountId ?: "default_cash"

        val saleItems = items.map { amount ->
            SaleItem(productId = "", quantity = 1, price = amount, total = amount)
        }

        _state.value = currentState.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val sale = Sale(
                    items = saleItems,
                    total = total,
                    payments = payments,
                    cashierId = cashierId
                )

                val result = repository.saveSale(sale)
                if (result.isSuccess) {
                    val saleId = result.getOrNull() ?: ""
                    processMoneyTransactionUseCase.processSale(
                        payments = payments,
                        saleId = saleId,
                        description = "Sale"
                    )
                    Log.d(TAG, "Sale saved")

                    // Reset AFTER success
                    _state.value = stateManager.resetState().copy(
                        isSaving = false,
                        saveSuccess = true,
                        recentSales = currentRecentSales,
                        accounts = currentState.accounts,
                        paymentRows = listOf(PaymentRow(1L, lastAccountId, ""))
                    )
                    userTouchedPaymentAmounts = false
                } else {
                    _state.value = currentState.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Failed"
                    )
                }
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isSaving = false,
                    error = e.message ?: "Failed"
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

class SaleEntryViewModelFactory(
    private val repository: SaleRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val processMoneyTransactionUseCase: ProcessMoneyTransactionUseCase,
    private val paymentPreferences: PaymentPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleEntryViewModel::class.java)) {
            return SaleEntryViewModel(
                repository,
                moneyAccountRepository,
                processMoneyTransactionUseCase,
                paymentPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
