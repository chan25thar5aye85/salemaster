package com.akari.retailer.features.money.presentation

import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.money.domain.models.MoneyTransactionType

sealed class MoneyTransactionsEvent {
    data object LoadTransactions : MoneyTransactionsEvent()
    data object RefreshTransactions : MoneyTransactionsEvent()
    data object ClearError : MoneyTransactionsEvent()

    data class AccountFilterChanged(val accountId: String) : MoneyTransactionsEvent()
    data class TypeFilterChanged(val type: MoneyTransactionType?) : MoneyTransactionsEvent()
    data class TimeFilterChanged(val filter: TimeFilter) : MoneyTransactionsEvent()
    data class SearchQueryChanged(val query: String) : MoneyTransactionsEvent()

    data object ClearFilters : MoneyTransactionsEvent()
    data object ToggleFilterSheet : MoneyTransactionsEvent()
}
