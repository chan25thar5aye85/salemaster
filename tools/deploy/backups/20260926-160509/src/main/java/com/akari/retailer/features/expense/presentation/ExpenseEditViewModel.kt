package com.akari.retailer.features.expense.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseFinalizer
import com.akari.retailer.features.expense.data.repository.CategoryRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.PaymentEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ExpenseEditState(
    val id: String = "",
    val title: String = "",
    val amount: String = "",
    val selectedCategory: ExpenseCategory? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val description: String = "",
    val accounts: List<MoneyAccount> = emptyList(),
    val paymentRows: List<PaymentRow> = listOf(
        PaymentRow(id = 1L, accountId = "default_cash", amount = "")
    ),
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

sealed class ExpenseEditEvent {
    data class TitleChanged(val value: String) : ExpenseEditEvent()
    data class AmountChanged(val value: String) : ExpenseEditEvent()
    data class CategorySelected(val category: ExpenseCategory) : ExpenseEditEvent()
    data class DescriptionChanged(val value: String) : ExpenseEditEvent()
    data class PaymentAccountChanged(val rowId: Long, val account: MoneyAccount) : ExpenseEditEvent()
    data class PaymentAmountChanged(val rowId: Long, val amount: String) : ExpenseEditEvent()
    data object AddPaymentRow : ExpenseEditEvent()
    data class RemovePaymentRow(val rowId: Long) : ExpenseEditEvent()
    data object LoadExpense : ExpenseEditEvent()
    data object SaveExpense : ExpenseEditEvent()
    data object ClearError : ExpenseEditEvent()
    data object ResetSuccess : ExpenseEditEvent()
}

class ExpenseEditViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val expenseFinalizer: FirestoreExpenseFinalizer,
    private val paymentPreferences: PaymentPreferences,
    private val expenseId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseEditState(id = expenseId))
    val state: StateFlow<ExpenseEditState> = _state.asStateFlow()

    private var loadExpenseJob: Job? = null
    private var loadCategoriesJob: Job? = null
    private var loadAccountsJob: Job? = null

    private var nextPaymentRowId = 2L
    private var userTouchedAmounts = false

    init {
        loadCategories()
        loadAccounts()
        loadExpense()
    }

    fun handleEvent(event: ExpenseEditEvent) {
        when (event) {
            is ExpenseEditEvent.TitleChanged -> _state.value = _state.value.copy(title = event.value)
            is ExpenseEditEvent.AmountChanged -> handleAmountChanged(event.value)
            is ExpenseEditEvent.CategorySelected -> _state.value = _state.value.copy(selectedCategory = event.category)
            is ExpenseEditEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is ExpenseEditEvent.PaymentAccountChanged -> updatePaymentAccount(event.rowId, event.account)
            is ExpenseEditEvent.PaymentAmountChanged -> updatePaymentAmount(event.rowId, event.amount)
            is ExpenseEditEvent.AddPaymentRow -> addPaymentRow()
            is ExpenseEditEvent.RemovePaymentRow -> removePaymentRow(event.rowId)
            ExpenseEditEvent.LoadExpense -> loadExpense()
            ExpenseEditEvent.SaveExpense -> saveExpense()
            ExpenseEditEvent.ClearError -> _state.value = _state.value.copy(error = null)
            ExpenseEditEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
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
                    _state.value = _state.value.copy(accounts = accounts.filter { it.isActive })
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadExpense() {
        loadExpenseJob?.cancel()
        loadExpenseJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val expense = expenseRepository.getExpenseById(expenseId).first()
                if (expense != null) {
                    val category = _state.value.categories.find { it.id == expense.categoryId }

                    // Map existing payments to payment rows
                    val rows = if (expense.payments.isNotEmpty()) {
                        expense.payments.mapIndexed { index, p ->
                            PaymentRow(
                                id = (index + 1).toLong(),
                                accountId = p.accountId,
                                amount = p.amount.toString()
                            )
                        }
                    } else {
                        listOf(PaymentRow(id = 1L, accountId = "default_cash", amount = expense.amount.toString()))
                    }
                    nextPaymentRowId = (rows.maxOfOrNull { it.id } ?: 1L) + 1
                    userTouchedAmounts = true  // don't auto-overwrite loaded values

                    _state.value = _state.value.copy(
                        id = expense.id,
                        title = expense.title,
                        amount = expense.amount.toString(),
                        selectedCategory = category,
                        description = expense.description,
                        paymentRows = rows,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Expense not found"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load expense"
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

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            // Fetch current version to preserve fields not editable on this screen
            // (type, businessPercentage, date).
            val existing = try {
                expenseRepository.getExpenseById(expenseId).first()
            } catch (e: Exception) { null }

            if (existing == null) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Expense not found"
                )
                return@launch
            }

            val updated = existing.copy(
                title = currentState.title.trim(),
                amount = amountInt,
                categoryId = currentState.selectedCategory.id,
                description = currentState.description.trim(),
                payments = payments,
                updatedAt = System.currentTimeMillis()
            )

            val result = expenseFinalizer.updateExpense(updated)

            if (result.isSuccess) {
                payments.firstOrNull()?.let {
                }
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to update expense"
                )
            }
        }
    }
}

class ExpenseEditViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val expenseFinalizer: FirestoreExpenseFinalizer,
    private val paymentPreferences: PaymentPreferences,
    private val expenseId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseEditViewModel::class.java)) {
            return ExpenseEditViewModel(
                expenseRepository,
                categoryRepository,
                moneyAccountRepository,
                expenseFinalizer,
                paymentPreferences,
                expenseId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
