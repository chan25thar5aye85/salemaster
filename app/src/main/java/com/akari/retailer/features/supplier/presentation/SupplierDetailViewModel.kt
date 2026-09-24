package com.akari.retailer.features.supplier.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.Supplier
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import com.akari.retailer.features.supplier.domain.usecases.GetSupplierTransactionsUseCase
import com.akari.retailer.features.supplier.domain.usecases.RecordSupplierPaymentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupplierDetailState(
    val supplier: Supplier? = null,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Payable
    val payableTransactions: List<SupplierTransaction> = emptyList(),

    // Payment dialog
    val showPaymentDialog: Boolean = false,
    val paymentAmount: String = "",
    val paymentNotes: String = "",
    val accounts: List<MoneyAccount> = emptyList(),
    val selectedAccount: MoneyAccount? = null,
    val isRecordingPayment: Boolean = false,
    val paymentSuccess: Boolean = false,
    val paymentError: String? = null
)

sealed class SupplierDetailEvent {
    data object LoadSupplier : SupplierDetailEvent()
    data object ClearError : SupplierDetailEvent()
    data object OpenPaymentDialog : SupplierDetailEvent()
    data object ClosePaymentDialog : SupplierDetailEvent()
    data class PaymentAmountChanged(val value: String) : SupplierDetailEvent()
    data class PaymentNotesChanged(val value: String) : SupplierDetailEvent()
    data class PaymentAccountSelected(val account: MoneyAccount) : SupplierDetailEvent()
    data object SubmitPayment : SupplierDetailEvent()
    data object ClearPaymentSuccess : SupplierDetailEvent()
}

class SupplierDetailViewModel(
    private val repository: SupplierRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val recordSupplierPaymentUseCase: RecordSupplierPaymentUseCase,
    private val getSupplierTransactionsUseCase: GetSupplierTransactionsUseCase,
    private val paymentPreferences: PaymentPreferences,
    private val supplierId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SupplierDetailState())
    val state: StateFlow<SupplierDetailState> = _state.asStateFlow()

    private var loadSupplierJob: Job? = null
    private var loadAccountsJob: Job? = null
    private var loadTransactionsJob: Job? = null

    init {
        loadSupplier()
        loadAccounts()
        loadTransactions()
    }

    fun handleEvent(event: SupplierDetailEvent) {
        when (event) {
            SupplierDetailEvent.LoadSupplier -> loadSupplier()
            SupplierDetailEvent.ClearError -> clearError()
            SupplierDetailEvent.OpenPaymentDialog -> openPaymentDialog()
            SupplierDetailEvent.ClosePaymentDialog -> closePaymentDialog()
            is SupplierDetailEvent.PaymentAmountChanged -> {
                _state.value = _state.value.copy(
                    paymentAmount = event.value.filter { it.isDigit() }
                )
            }
            is SupplierDetailEvent.PaymentNotesChanged -> {
                _state.value = _state.value.copy(paymentNotes = event.value)
            }
            is SupplierDetailEvent.PaymentAccountSelected -> {
                _state.value = _state.value.copy(selectedAccount = event.account)
            }
            SupplierDetailEvent.SubmitPayment -> submitPayment()
            SupplierDetailEvent.ClearPaymentSuccess -> {
                _state.value = _state.value.copy(paymentSuccess = false)
            }
        }
    }

    private fun loadSupplier() {
        loadSupplierJob?.cancel()
        loadSupplierJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getSupplierById(supplierId).collect { supplier ->
                    _state.value = _state.value.copy(
                        supplier = supplier,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load supplier"
                )
            }
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

    private fun loadTransactions() {
        loadTransactionsJob?.cancel()
        loadTransactionsJob = viewModelScope.launch {
            try {
                getSupplierTransactionsUseCase.forSupplier(supplierId).collect { txns ->
                    _state.value = _state.value.copy(payableTransactions = txns.take(10))
                }
            } catch (e: Exception) { }
        }
    }

    private fun openPaymentDialog() {
        val owed = _state.value.supplier?.payableBalance ?: 0
        if (owed <= 0) {
            _state.value = _state.value.copy(paymentError = "Nothing owed to this supplier")
            return
        }

        // Prefer last-used account; fall back to first active account
        val lastUsedId = paymentPreferences.getLastUsedAccountId()
        val defaultAccount = _state.value.accounts.find { it.id == lastUsedId }
            ?: _state.value.accounts.firstOrNull()

        _state.value = _state.value.copy(
            showPaymentDialog = true,
            paymentAmount = "",
            paymentNotes = "",
            selectedAccount = defaultAccount,
            paymentError = null,
            paymentSuccess = false
        )
    }

    private fun closePaymentDialog() {
        _state.value = _state.value.copy(
            showPaymentDialog = false,
            paymentAmount = "",
            paymentNotes = "",
            selectedAccount = null,
            paymentError = null
        )
    }

    private fun submitPayment() {
        val supplier = _state.value.supplier ?: return
        val amount = _state.value.paymentAmount.toIntOrNull() ?: 0
        val account = _state.value.selectedAccount

        if (amount <= 0) {
            _state.value = _state.value.copy(paymentError = "Enter a valid amount")
            return
        }
        if (amount > supplier.payableBalance) {
            _state.value = _state.value.copy(
                paymentError = "Amount exceeds owed (${supplier.payableBalance})"
            )
            return
        }
        if (account == null) {
            _state.value = _state.value.copy(paymentError = "Select a money account")
            return
        }

        _state.value = _state.value.copy(isRecordingPayment = true, paymentError = null)

        viewModelScope.launch {
            val result = recordSupplierPaymentUseCase.invoke(
                supplierId = supplier.id,
                amount = amount,
                paymentAccountId = account.id,
                description = _state.value.paymentNotes.trim().ifEmpty { "Supplier payment" }
            )
            if (result.isSuccess) {
                paymentPreferences.setLastUsedAccountId(account.id)
                _state.value = _state.value.copy(
                    isRecordingPayment = false,
                    paymentSuccess = true,
                    showPaymentDialog = false,
                    paymentAmount = "",
                    paymentNotes = ""
                )
            } else {
                _state.value = _state.value.copy(
                    isRecordingPayment = false,
                    paymentError = result.exceptionOrNull()?.message ?: "Payment failed"
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null, paymentError = null)
    }
}

class SupplierDetailViewModelFactory(
    private val repository: SupplierRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val recordSupplierPaymentUseCase: RecordSupplierPaymentUseCase,
    private val getSupplierTransactionsUseCase: GetSupplierTransactionsUseCase,
    private val paymentPreferences: PaymentPreferences,
    private val supplierId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupplierDetailViewModel::class.java)) {
            return SupplierDetailViewModel(
                repository,
                moneyAccountRepository,
                recordSupplierPaymentUseCase,
                getSupplierTransactionsUseCase,
                paymentPreferences,
                supplierId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
