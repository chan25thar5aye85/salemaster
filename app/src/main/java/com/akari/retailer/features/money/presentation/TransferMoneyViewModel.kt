package com.akari.retailer.features.money.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.usecases.TransferMoneyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransferMoneyViewModel(
    private val accountRepository: MoneyAccountRepository,
    private val transferMoneyUseCase: TransferMoneyUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TransferMoneyState())
    val state: StateFlow<TransferMoneyState> = _state.asStateFlow()

    init {
        loadAccounts()
    }

    fun handleEvent(event: TransferMoneyEvent) {
        when (event) {
            is TransferMoneyEvent.LoadAccounts -> loadAccounts()
            is TransferMoneyEvent.FromAccountSelected -> _state.value = _state.value.copy(fromAccount = event.account)
            is TransferMoneyEvent.ToAccountSelected -> _state.value = _state.value.copy(toAccount = event.account)
            is TransferMoneyEvent.AmountChanged -> _state.value = _state.value.copy(amount = event.value)
            is TransferMoneyEvent.FeeChanged -> _state.value = _state.value.copy(fee = event.value)
            is TransferMoneyEvent.FeeTypeChanged -> _state.value = _state.value.copy(feeType = event.type)
            is TransferMoneyEvent.DescriptionChanged -> _state.value = _state.value.copy(description = event.value)
            is TransferMoneyEvent.SaveTransfer -> saveTransfer()
            is TransferMoneyEvent.ClearError -> _state.value = _state.value.copy(error = null)
            is TransferMoneyEvent.ResetSuccess -> _state.value = _state.value.copy(saveSuccess = false)
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
        
        if (currentState.fromAccount == null) {
            _state.value = _state.value.copy(error = "Select source account")
            return
        }
        if (currentState.toAccount == null) {
            _state.value = _state.value.copy(error = "Select destination account")
            return
        }
        if (currentState.fromAccount.id == currentState.toAccount.id) {
            _state.value = _state.value.copy(error = "Cannot transfer to the same account")
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
            
            val params = TransferMoneyUseCase.Params(
                fromAccountId = currentState.fromAccount.id,
                toAccountId = currentState.toAccount.id,
                amount = amountInt,
                fee = feeInt,
                feeType = currentState.feeType,
                description = currentState.description.trim()
            )
            
            val result = transferMoneyUseCase.invoke(params)
            
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

class TransferMoneyViewModelFactory(
    private val accountRepository: MoneyAccountRepository,
    private val transferMoneyUseCase: TransferMoneyUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransferMoneyViewModel::class.java)) {
            return TransferMoneyViewModel(accountRepository, transferMoneyUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
