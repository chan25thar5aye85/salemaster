package com.akari.retailer.features.money.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.usecases.ExternalTransferUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExternalTransferViewModel(
    private val accountRepository: MoneyAccountRepository,
    private val externalTransferUseCase: ExternalTransferUseCase,
    private val appContext: Context
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "external_transfer_prefs"
        private const val KEY_LAST_ACCOUNT = "last_used_account_id"
    }

    private val _state = MutableStateFlow(ExternalTransferState())
    val state: StateFlow<ExternalTransferState> = _state.asStateFlow()

    private var loadAccountsJob: Job? = null

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        loadLastUsedAccount()
        loadAccounts()
    }

    fun handleEvent(event: ExternalTransferEvent) {
        when (event) {
            is ExternalTransferEvent.LoadAccounts -> loadAccounts()
            is ExternalTransferEvent.AccountSelected -> {
                _state.value = _state.value.copy(selectedAccount = event.account)
                saveLastUsedAccount(event.account.id)
            }
            is ExternalTransferEvent.DirectionChanged -> _state.value = _state.value.copy(direction = event.direction)
            is ExternalTransferEvent.ExternalAccountNameChanged -> _state.value = _state.value.copy(externalAccountName = event.value)
            is ExternalTransferEvent.AmountChanged -> _state.value = _state.value.copy(amount = event.value)
            is ExternalTransferEvent.FeeChanged -> _state.value = _state.value.copy(fee = event.value)
            is ExternalTransferEvent.FeeTypeChanged -> _state.value = _state.value.copy(feeType = event.type)
            is ExternalTransferEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is ExternalTransferEvent.SaveTransfer -> saveTransfer()
            is ExternalTransferEvent.ClearError -> _state.value = _state.value.copy(error = null)
            is ExternalTransferEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadLastUsedAccount() {
        val lastId = prefs.getString(KEY_LAST_ACCOUNT, "") ?: ""
        _state.value = _state.value.copy(lastUsedAccountId = lastId)
    }

    private fun saveLastUsedAccount(accountId: String) {
        prefs.edit().putString(KEY_LAST_ACCOUNT, accountId).apply()
    }

    private fun loadAccounts() {
        loadAccountsJob?.cancel()
        loadAccountsJob = viewModelScope.launch {
            try {
                accountRepository.getAccounts().collect { accounts ->
                    val active = accounts.filter { it.isActive }
                    val lastUsedId = _state.value.lastUsedAccountId
                    val autoSelected = if (_state.value.selectedAccount == null && lastUsedId.isNotEmpty()) {
                        active.find { it.id == lastUsedId }
                    } else {
                        _state.value.selectedAccount
                    }
                    _state.value = _state.value.copy(
                        accounts = active,
                        selectedAccount = autoSelected
                    )
                }
            } catch (e: Exception) { }
        }
    }

    private fun saveTransfer() {
        val currentState = _state.value

        if (currentState.selectedAccount == null) {
            _state.value = _state.value.copy(error = "Select account")
            return
        }
        if (currentState.externalAccountName.isBlank()) {
            _state.value = _state.value.copy(error = "External account name is required")
            return
        }

        val amountInt = currentState.amount.toIntOrNull()
        if (amountInt == null || amountInt <= 0) {
            _state.value = _state.value.copy(error = "Enter a valid amount")
            return
        }

        val feeInt = currentState.fee.toIntOrNull() ?: 0
        if (feeInt < 0) {
            _state.value = _state.value.copy(error = "Fee cannot be negative")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            val direction = if (currentState.direction == ExternalTransferDirection.OUTGOING) {
                ExternalTransferUseCase.Direction.OUTGOING
            } else {
                ExternalTransferUseCase.Direction.INCOMING
            }

            val params = ExternalTransferUseCase.Params(
                direction = direction,
                accountId = currentState.selectedAccount.id,
                externalAccountName = currentState.externalAccountName.trim(),
                externalAccountNumber = "",
                amount = amountInt,
                fee = feeInt,
                feeType = currentState.feeType,
                description = currentState.description.trim()
            )

            val result = externalTransferUseCase.invoke(params)

            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    error = null,
                    externalAccountName = "",
                    amount = "",
                    fee = "",
                    feeType = FeeType.NONE,
                    description = ""
                )
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = result.exceptionOrNull()?.message ?: "Transfer failed"
                )
            }
        }
    }
}

class ExternalTransferViewModelFactory(
    private val accountRepository: MoneyAccountRepository,
    private val externalTransferUseCase: ExternalTransferUseCase,
    private val appContext: Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalTransferViewModel::class.java)) {
            return ExternalTransferViewModel(accountRepository, externalTransferUseCase, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
