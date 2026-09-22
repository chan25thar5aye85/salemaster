package com.akari.retailer.features.money.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.MoneyAccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MoneyAccountsViewModel(
    private val repository: MoneyAccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MoneyAccountsState())
    val state: StateFlow<MoneyAccountsState> = _state.asStateFlow()

    init {
        loadAccounts()
    }

    fun handleEvent(event: MoneyAccountsEvent) {
        when (event) {
            is MoneyAccountsEvent.LoadAccounts -> loadAccounts()
            is MoneyAccountsEvent.RefreshAccounts -> loadAccounts()
            is MoneyAccountsEvent.ClearError -> clearError()
            is MoneyAccountsEvent.ShowAddDialog -> showAddDialog()
            is MoneyAccountsEvent.ShowEditDialog -> showEditDialog(event.account)
            is MoneyAccountsEvent.DismissDialog -> dismissDialog()
            is MoneyAccountsEvent.DialogNameChanged -> updateDialogName(event.name)
            is MoneyAccountsEvent.DialogIconChanged -> updateDialogIcon(event.icon)
            is MoneyAccountsEvent.DialogColorChanged -> updateDialogColor(event.color)
            is MoneyAccountsEvent.DialogOpeningBalanceChanged -> updateDialogOpeningBalance(event.balance)
            is MoneyAccountsEvent.DialogAccountNumberChanged -> updateDialogAccountNumber(event.number)
            is MoneyAccountsEvent.DialogNotesChanged -> updateDialogNotes(event.notes)
            is MoneyAccountsEvent.DialogTypeChanged -> updateDialogType(event.type)
            is MoneyAccountsEvent.SaveAccount -> saveAccount()
            is MoneyAccountsEvent.ShowDeleteDialog -> showDeleteDialog(event.accountId)
            is MoneyAccountsEvent.DismissDeleteDialog -> dismissDeleteDialog()
            is MoneyAccountsEvent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                repository.getAccounts().collect { accounts ->
                    val active = accounts.filter { it.isActive }
                    _state.value = _state.value.copy(
                        accounts = accounts,
                        activeAccounts = active,
                        totalBalance = active.sumOf { it.currentBalance },
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load accounts"
                )
            }
        }
    }

    private fun showAddDialog() {
        _state.value = _state.value.copy(
            showDialog = true,
            editingAccount = null,
            dialogName = "",
            dialogIcon = "💵",
            dialogColor = "#4CAF50",
            dialogOpeningBalance = "0",
            dialogAccountNumber = "",
            dialogNotes = "",
            isSaving = false,           // ✅ Reset
            error = null
        )
    }

    private fun showEditDialog(account: MoneyAccount) {
        _state.value = _state.value.copy(
            showDialog = true,
            editingAccount = account,
            dialogName = account.name,
            dialogIcon = account.icon,
            dialogColor = account.color,
            dialogOpeningBalance = account.openingBalance.toString(),
            dialogAccountNumber = account.accountNumber,
            dialogNotes = account.notes,
            isSaving = false,           // ✅ Reset
            error = null
        )
    }

    private fun dismissDialog() {
        _state.value = _state.value.copy(
            showDialog = false,
            editingAccount = null,
            dialogName = "",
            dialogIcon = "💵",
            dialogColor = "#4CAF50",
            dialogOpeningBalance = "0",
            dialogAccountNumber = "",
            dialogNotes = "",
            isSaving = false,           // ✅ FIXED: Reset isSaving
            error = null
        )
    }

    private fun updateDialogName(name: String) {
        _state.value = _state.value.copy(dialogName = name)
    }

    private fun updateDialogIcon(icon: String) {
        _state.value = _state.value.copy(dialogIcon = icon)
    }

    private fun updateDialogColor(color: String) {
        _state.value = _state.value.copy(dialogColor = color)
    }

    private fun updateDialogOpeningBalance(balance: String) {
        if (balance.isEmpty() || balance.toIntOrNull() != null) {
            _state.value = _state.value.copy(dialogOpeningBalance = balance)
        }
    }

    private fun updateDialogAccountNumber(number: String) {
        _state.value = _state.value.copy(dialogAccountNumber = number)
    }

    private fun updateDialogNotes(notes: String) {
        _state.value = _state.value.copy(dialogNotes = notes)
    }

    private fun updateDialogType(type: MoneyAccountType) {
        _state.value = _state.value.copy(
            dialogIcon = when (type) {
                MoneyAccountType.CASH -> "💵"
                MoneyAccountType.MOBILE_WALLET -> "📱"
                MoneyAccountType.BANK -> "🏦"
                MoneyAccountType.CREDIT -> "💳"
                MoneyAccountType.OTHER -> "💰"
            }
        )
    }

    private fun saveAccount() {
        val name = _state.value.dialogName.trim()
        if (name.isBlank()) {
            _state.value = _state.value.copy(error = "Account name is required")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            try {
                val editing = _state.value.editingAccount
                val openingBalance = _state.value.dialogOpeningBalance.toIntOrNull() ?: 0
                
                if (editing != null) {
                    // Update existing
                    val updated = editing.copy(
                        name = name,
                        icon = _state.value.dialogIcon,
                        color = _state.value.dialogColor,
                        openingBalance = openingBalance,
                        accountNumber = _state.value.dialogAccountNumber,
                        notes = _state.value.dialogNotes,
                        updatedAt = System.currentTimeMillis()
                    )
                    val result = repository.updateAccount(updated)
                    if (result.isSuccess) {
                        dismissDialog()  // ✅ Reset isSaving happens here
                    } else {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to update account"
                        )
                    }
                } else {
                    // Add new
                    val newAccount = MoneyAccount(
                        name = name,
                        type = MoneyAccountType.OTHER,
                        icon = _state.value.dialogIcon,
                        color = _state.value.dialogColor,
                        openingBalance = openingBalance,
                        currentBalance = openingBalance,
                        accountNumber = _state.value.dialogAccountNumber,
                        notes = _state.value.dialogNotes,
                        isDefault = false
                    )
                    val result = repository.addAccount(newAccount)
                    if (result.isSuccess) {
                        dismissDialog()  // ✅ Reset isSaving happens here
                    } else {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to add account"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save account"
                )
            }
        }
    }

    private fun showDeleteDialog(accountId: String) {
        _state.value = _state.value.copy(
            showDeleteDialog = true,
            pendingDeleteId = accountId
        )
    }

    private fun dismissDeleteDialog() {
        _state.value = _state.value.copy(
            showDeleteDialog = false,
            pendingDeleteId = null
        )
    }

    private fun confirmDelete() {
        val accountId = _state.value.pendingDeleteId ?: return
        viewModelScope.launch {
            try {
                val result = repository.deleteAccount(accountId)
                if (result.isSuccess) {
                    dismissDeleteDialog()
                    loadAccounts()
                } else {
                    _state.value = _state.value.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to delete account",
                        showDeleteDialog = false,
                        pendingDeleteId = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to delete account",
                    showDeleteDialog = false,
                    pendingDeleteId = null
                )
            }
        }
    }

    private fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

class MoneyAccountsViewModelFactory(
    private val repository: MoneyAccountRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoneyAccountsViewModel::class.java)) {
            return MoneyAccountsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
