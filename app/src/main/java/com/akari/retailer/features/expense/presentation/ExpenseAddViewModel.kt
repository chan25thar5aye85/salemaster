package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseFinalizer
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.expense.domain.models.ExpenseType
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.PaymentEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpenseAddState(
    val title: String = "",
    val amount: String = "",
    val selectedCategory: ExpenseCategory? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val expenseType: ExpenseType = ExpenseType.BUSINESS,
    val businessPercentage: String = "100",
    val accounts: List<MoneyAccount> = emptyList(),
    val paymentRows: List<PaymentRow> = listOf(
        PaymentRow(id = 1L, accountId = "default_cash", amount = "")
    ),
    val description: String = "",
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
    fun isOverPaid(): Boolean = getTotalPaid() > getTotalAmount()

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

sealed class ExpenseAddEvent {
    data class TitleChanged(val value: String) : ExpenseAddEvent()
    data class AmountChanged(val value: String) : ExpenseAddEvent()
    data class CategorySelected(val category: ExpenseCategory) : ExpenseAddEvent()
    data class ExpenseTypeChanged(val type: ExpenseType) : ExpenseAddEvent()
    data class BusinessPercentageChanged(val value: String) : ExpenseAddEvent()
    data class DescriptionChanged(val value: String) : ExpenseAddEvent()

    data class PaymentAccountChanged(val rowId: Long, val account: MoneyAccount) : ExpenseAddEvent()
    data class PaymentAmountChanged(val rowId: Long, val amount: String) : ExpenseAddEvent()
    data object AddPaymentRow : ExpenseAddEvent()
    data class RemovePaymentRow(val rowId: Long) : ExpenseAddEvent()

    data object SaveExpense : ExpenseAddEvent()
    data object ClearError : ExpenseAddEvent()
    data object ResetSuccess : ExpenseAddEvent()
}

class ExpenseAddViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val expenseFinalizer: FirestoreExpenseFinalizer,
    private val paymentPreferences: PaymentPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseAddState())
    val state: StateFlow<ExpenseAddState> = _state.asStateFlow()

    private var loadCategoriesJob: Job? = null
    private var loadAccountsJob: Job? = null

    private var nextRowId = 2L

    init {
        val lastAccountId = paymentPreferences.getLastUsedAccountId()
        _state.value = _state.value.copy(
            lastUsedAccountId = lastAccountId,
            paymentRows = listOf(
                PaymentRow(id = 1L, accountId = lastAccountId, amount = "")
            )
        )
        loadCategories()
        loadAccounts()
    }

    fun handleEvent(event: ExpenseAddEvent) {
        when (event) {
            is ExpenseAddEvent.TitleChanged -> _state.value = _state.value.copy(title = event.value)
            is ExpenseAddEvent.AmountChanged -> handleAmountChanged(event.value)
            is ExpenseAddEvent.CategorySelected -> _state.value = _state.value.copy(selectedCategory = event.category)
            is ExpenseAddEvent.ExpenseTypeChanged -> _state.value = _state.value.copy(expenseType = event.type)
            is ExpenseAddEvent.BusinessPercentageChanged -> _state.value = _state.value.copy(businessPercentage = event.value)
            is ExpenseAddEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is ExpenseAddEvent.PaymentAccountChanged -> updatePaymentAccount(event.rowId, event.account)
            is ExpenseAddEvent.PaymentAmountChanged -> updatePaymentAmount(event.rowId, event.amount)
            is ExpenseAddEvent.AddPaymentRow -> addPaymentRow()
            is ExpenseAddEvent.RemovePaymentRow -> removePaymentRow(event.rowId)
            is ExpenseAddEvent.SaveExpense -> saveExpense()
            is ExpenseAddEvent.ClearError -> _state.value = _state.value.copy(error = null)
            is ExpenseAddEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadCategories() {
        loadCategoriesJob?.cancel()
        loadCategoriesJob = viewModelScope.launch {
            try {
                categoryRepository.getCategories().collect { categories ->
                    _state.value = _state.value.copy(categories = categories)
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

    private fun saveExpense() {
        val currentState = _state.value

        if (currentState.title.isBlank()) {
            _state.value = _state.value.copy(error = "Title is required")
            return
        }

        val amountInt = currentState.amount.toIntOrNull()
        if (amountInt == null || amountInt <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }

        if (currentState.selectedCategory == null) {
            _state.value = _state.value.copy(error = "Select a category")
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

        if (currentState.expenseType == ExpenseType.MIXED) {
            val percentage = currentState.businessPercentage.toIntOrNull()
            if (percentage == null || percentage < 0 || percentage > 100) {
                _state.value = _state.value.copy(error = "Business percentage must be 0-100")
                return
            }
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            val percentage = currentState.businessPercentage.toIntOrNull() ?: 100

            val expense = Expense(
                title = currentState.title.trim(),
                amount = amountInt,
                categoryId = currentState.selectedCategory.id,
                type = currentState.expenseType,
                businessPercentage = if (currentState.expenseType == ExpenseType.MIXED) percentage else 100,
                payments = payments,
                description = currentState.description.trim()
            )

            val result = expenseFinalizer.addExpense(expense)

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
                    error = result.exceptionOrNull()?.message ?: "Failed to save expense"
                )
            }
        }
    }
}

class ExpenseAddViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val expenseFinalizer: FirestoreExpenseFinalizer,
    private val paymentPreferences: PaymentPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseAddViewModel::class.java)) {
            return ExpenseAddViewModel(
                expenseRepository,
                categoryRepository,
                moneyAccountRepository,
                expenseFinalizer,
                paymentPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
