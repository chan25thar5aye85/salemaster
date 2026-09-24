package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import com.akari.retailer.features.customer.domain.models.Customer
import com.akari.retailer.features.customer.domain.usecases.GetCreditTransactionsUseCase
import com.akari.retailer.features.customer.domain.usecases.RecordCreditPaymentUseCase
import com.akari.retailer.features.customer.domain.usecases.RecordCreditRefundUseCase
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

data class CustomerDetailState(
    val customer: Customer? = null,
    val isLoading: Boolean = true,
    val error: String? = null,

    // Credit
    val creditTransactions: List<CreditTransaction> = emptyList(),

    // Payment dialog (they pay us)
    val showPaymentDialog: Boolean = false,
    val paymentAmount: String = "",
    val paymentNotes: String = "",
    val accounts: List<MoneyAccount> = emptyList(),
    val selectedAccount: MoneyAccount? = null,
    val isRecordingPayment: Boolean = false,
    val paymentSuccess: Boolean = false,
    val paymentError: String? = null,

    // Refund dialog (we pay them)
    val showRefundDialog: Boolean = false,
    val refundAmount: String = "",
    val refundNotes: String = "",
    val selectedRefundAccount: MoneyAccount? = null,
    val isRecordingRefund: Boolean = false,
    val refundSuccess: Boolean = false,
    val refundError: String? = null
)

sealed class CustomerDetailEvent {
    data object LoadCustomer : CustomerDetailEvent()
    data object ClearError : CustomerDetailEvent()
    data object OpenPaymentDialog : CustomerDetailEvent()
    data object ClosePaymentDialog : CustomerDetailEvent()
    data class PaymentAmountChanged(val value: String) : CustomerDetailEvent()
    data class PaymentNotesChanged(val value: String) : CustomerDetailEvent()
    data class PaymentAccountSelected(val account: MoneyAccount) : CustomerDetailEvent()
    data object SubmitPayment : CustomerDetailEvent()
    data object ClearPaymentSuccess : CustomerDetailEvent()

    // Refund events
    data object OpenRefundDialog : CustomerDetailEvent()
    data object CloseRefundDialog : CustomerDetailEvent()
    data class RefundAmountChanged(val value: String) : CustomerDetailEvent()
    data class RefundNotesChanged(val value: String) : CustomerDetailEvent()
    data class RefundAccountSelected(val account: MoneyAccount) : CustomerDetailEvent()
    data object SubmitRefund : CustomerDetailEvent()
    data object ClearRefundSuccess : CustomerDetailEvent()
}

class CustomerDetailViewModel(
    private val repository: CustomerRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val recordCreditPaymentUseCase: RecordCreditPaymentUseCase,
    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,
    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,
    private val paymentPreferences: PaymentPreferences,
    private val customerId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerDetailState())
    val state: StateFlow<CustomerDetailState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var loadAccountsJob: Job? = null
    private var loadCreditJob: Job? = null

    init {
        loadCustomer()
        loadAccounts()
        loadCreditTransactions()
        refreshFromServer()
    }

    /**
     * Firestore's Android SDK serves cached values first. After a sale on
     * credit, the creditBalance was written by a different ViewModel — the
     * cache on this screen may still be stale. Force a server read so the
     * listener downstream picks up the fresh value.
     */
    private fun refreshFromServer() {
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("customers")
                    .document(customerId)
                    .get(Source.SERVER)
                    .await()
                // The existing listener on getCustomerById will now emit the fresh value
            } catch (e: Exception) {
                // Ignore — fallback to whatever the listener emits
            }
        }
    }

    fun handleEvent(event: CustomerDetailEvent) {
        when (event) {
            is CustomerDetailEvent.LoadCustomer -> loadCustomer()
            is CustomerDetailEvent.ClearError -> clearError()
            is CustomerDetailEvent.OpenPaymentDialog -> openPaymentDialog()
            is CustomerDetailEvent.ClosePaymentDialog -> closePaymentDialog()
            is CustomerDetailEvent.PaymentAmountChanged -> _state.value = _state.value.copy(paymentAmount = event.value.filter { it.isDigit() })
            is CustomerDetailEvent.PaymentNotesChanged -> _state.value = _state.value.copy(paymentNotes = event.value)
            is CustomerDetailEvent.PaymentAccountSelected -> _state.value = _state.value.copy(selectedAccount = event.account)
            is CustomerDetailEvent.SubmitPayment -> submitPayment()
            is CustomerDetailEvent.ClearPaymentSuccess -> _state.value = _state.value.copy(paymentSuccess = false)
            is CustomerDetailEvent.OpenRefundDialog -> openRefundDialog()
            is CustomerDetailEvent.CloseRefundDialog -> closeRefundDialog()
            is CustomerDetailEvent.RefundAmountChanged -> _state.value = _state.value.copy(refundAmount = event.value.filter { it.isDigit() })
            is CustomerDetailEvent.RefundNotesChanged -> _state.value = _state.value.copy(refundNotes = event.value)
            is CustomerDetailEvent.RefundAccountSelected -> _state.value = _state.value.copy(selectedRefundAccount = event.account)
            is CustomerDetailEvent.SubmitRefund -> submitRefund()
            is CustomerDetailEvent.ClearRefundSuccess -> _state.value = _state.value.copy(refundSuccess = false)
        }
    }

    private fun openRefundDialog() {
        // Prefer last-used account
        val lastUsedId = paymentPreferences.getLastUsedAccountId()
        val defaultAccount = _state.value.accounts.find { it.id == lastUsedId }
            ?: _state.value.accounts.firstOrNull()

        _state.value = _state.value.copy(
            showRefundDialog = true,
            refundAmount = "",
            refundNotes = "",
            selectedRefundAccount = defaultAccount,
            refundError = null,
            refundSuccess = false
        )
    }

    private fun closeRefundDialog() {
        _state.value = _state.value.copy(
            showRefundDialog = false,
            refundAmount = "",
            refundNotes = "",
            selectedRefundAccount = null,
            refundError = null
        )
    }

    private fun submitRefund() {
        val customer = _state.value.customer ?: return
        val amount = _state.value.refundAmount.toIntOrNull() ?: 0
        val account = _state.value.selectedRefundAccount

        if (amount <= 0) {
            _state.value = _state.value.copy(refundError = "Enter a valid amount")
            return
        }
        if (account == null) {
            _state.value = _state.value.copy(refundError = "Select a money account")
            return
        }

        _state.value = _state.value.copy(isRecordingRefund = true, refundError = null)

        viewModelScope.launch {
            val result = recordCreditRefundUseCase.invoke(
                customerId = customer.id,
                amount = amount,
                paymentAccountId = account.id,
                description = _state.value.refundNotes.trim().ifEmpty { "Customer refund" }
            )
            if (result.isSuccess) {
                paymentPreferences.setLastUsedAccountId(account.id)
                _state.value = _state.value.copy(
                    isRecordingRefund = false,
                    refundSuccess = true,
                    showRefundDialog = false,
                    refundAmount = "",
                    refundNotes = ""
                )
            } else {
                _state.value = _state.value.copy(
                    isRecordingRefund = false,
                    refundError = result.exceptionOrNull()?.message ?: "Refund failed"
                )
            }
        }
    }

    private fun loadCustomer() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getCustomerById(customerId).collect { customer ->
                    _state.value = _state.value.copy(
                        customer = customer,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load customer"
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

    private fun loadCreditTransactions() {
        loadCreditJob?.cancel()
        loadCreditJob = viewModelScope.launch {
            try {
                getCreditTransactionsUseCase.forCustomer(customerId).collect { txns ->
                    _state.value = _state.value.copy(creditTransactions = txns.take(10))
                }
            } catch (e: Exception) { }
        }
    }

    private fun openPaymentDialog() {
        if ((_state.value.customer?.creditBalance ?: 0) <= 0) {
            _state.value = _state.value.copy(paymentError = "Customer has no outstanding credit")
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
        val customer = _state.value.customer ?: return
        val amount = _state.value.paymentAmount.toIntOrNull() ?: 0
        val account = _state.value.selectedAccount

        if (amount <= 0) {
            _state.value = _state.value.copy(paymentError = "Enter a valid amount")
            return
        }
        if (amount > customer.creditBalance) {
            _state.value = _state.value.copy(
                paymentError = "Amount exceeds owed (${customer.creditBalance})"
            )
            return
        }
        if (account == null) {
            _state.value = _state.value.copy(paymentError = "Select a money account")
            return
        }

        _state.value = _state.value.copy(isRecordingPayment = true, paymentError = null)

        viewModelScope.launch {
            val result = recordCreditPaymentUseCase.invoke(
                customerId = customer.id,
                amount = amount,
                paymentAccountId = account.id,
                description = _state.value.paymentNotes.trim().ifEmpty { "Credit payment" }
            )
            if (result.isSuccess) {
                // Remember for next time
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

class CustomerDetailViewModelFactory(
    private val repository: CustomerRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val recordCreditPaymentUseCase: RecordCreditPaymentUseCase,
    private val recordCreditRefundUseCase: RecordCreditRefundUseCase,
    private val getCreditTransactionsUseCase: GetCreditTransactionsUseCase,
    private val paymentPreferences: PaymentPreferences,
    private val customerId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerDetailViewModel::class.java)) {
            return CustomerDetailViewModel(
                repository,
                moneyAccountRepository,
                recordCreditPaymentUseCase,
                recordCreditRefundUseCase,
                getCreditTransactionsUseCase,
                paymentPreferences,
                customerId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
