package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import com.akari.retailer.features.customer.domain.models.Customer
import com.akari.retailer.features.customer.domain.usecases.GetCreditTransactionsUseCase
import com.akari.retailer.features.customer.domain.usecases.RecordCreditPaymentUseCase
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
}

class CustomerDetailViewModel(
    private val repository: CustomerRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val recordCreditPaymentUseCase: RecordCreditPaymentUseCase,
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
                getCreditTransactionsUseCase,
                paymentPreferences,
                customerId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
