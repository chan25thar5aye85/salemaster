package com.akari.retailer.features.money.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.usecases.ExternalTransferUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExternalTransferViewModel(
    private val accountRepository: MoneyAccountRepository,
    private val externalTransferUseCase: ExternalTransferUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ExternalTransferState())
    val state: StateFlow<ExternalTransferState> = _state.asStateFlow()

    init {
        loadAccounts()
    }

    fun handleEvent(event: ExternalTransferEvent) {
        when (event) {
            is ExternalTransferEvent.LoadAccounts -> loadAccounts()
            is ExternalTransferEvent.AccountSelected -> _state.value = _state.value.copy(selectedAccount = event.account)
            is ExternalTransferEvent.DirectionChanged -> _state.value = _state.value.copy(direction = event.direction)
            is ExternalTransferEvent.ExternalAccountNameChanged -> _state.value = _state.value.copy(externalAccountName = event.value)
            is ExternalTransferEvent.ExternalAccountNumberChanged -> _state.value = _state.value.copy(externalAccountNumber = event.value)
            is ExternalTransferEvent.AmountChanged -> _state.value = _state.value.copy(amount = event.value)
            is ExternalTransferEvent.FeeChanged -> _state.value = _state.value.copy(fee = event.value)
            is ExternalTransferEvent.FeeTypeChanged -> _state.value = _state.value.copy(feeType = event.type)
            is ExternalTransferEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is ExternalTransferEvent.SaveTransfer -> saveTransfer()
            is ExternalTransferEvent.ClearError -> _state.value = _state.value.copy(error = null)
            is ExternalTransferEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            try {
                accountRepository.getAccounts().collect { accounts ->
                    val active = accounts.filter { it.isActive }
                    _state.value = _state.value.copy(accounts = active)
                }
            } catch (e: Exception) {
                // Handle silently
            }
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
                externalAccountNumber = currentState.externalAccountNumber.trim(),
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
                    error = null
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
    private val externalTransferUseCase: ExternalTransferUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExternalTransferViewModel::class.java)) {
            return ExternalTransferViewModel(accountRepository, externalTransferUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
