package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.MoneyAccountType

sealed class MoneyAccountsEvent {
    data object LoadAccounts : MoneyAccountsEvent()
    data object RefreshAccounts : MoneyAccountsEvent()
    data object ClearError : MoneyAccountsEvent()
    
    // Dialog events
    data object ShowAddDialog : MoneyAccountsEvent()
    data class ShowEditDialog(val account: MoneyAccount) : MoneyAccountsEvent()
    data object DismissDialog : MoneyAccountsEvent()
    data class DialogNameChanged(val name: String) : MoneyAccountsEvent()
    data class DialogIconChanged(val icon: String) : MoneyAccountsEvent()
    data class DialogColorChanged(val color: String) : MoneyAccountsEvent()
    data class DialogOpeningBalanceChanged(val balance: String) : MoneyAccountsEvent()
    data class DialogAccountNumberChanged(val number: String) : MoneyAccountsEvent()
    data class DialogNotesChanged(val notes: String) : MoneyAccountsEvent()
    data class DialogTypeChanged(val type: MoneyAccountType) : MoneyAccountsEvent()
    data object SaveAccount : MoneyAccountsEvent()
    
    // Delete
    data class ShowDeleteDialog(val accountId: String) : MoneyAccountsEvent()
    data object DismissDeleteDialog : MoneyAccountsEvent()
    data object ConfirmDelete : MoneyAccountsEvent()
}
