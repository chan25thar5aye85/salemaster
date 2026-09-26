package com.akari.retailer.features.money.presentation

import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType

data class MoneyTransactionsState(
    val transactions: List<MoneyTransaction> = emptyList(),
    val filteredTransactions: List<MoneyTransaction> = emptyList(),
    val accounts: List<MoneyAccount> = emptyList(),

    val selectedAccountId: String = "",
    val selectedType: MoneyTransactionType? = null,
    val timeFilter: TimeFilter = TimeFilter(),
    val searchQuery: String = "",

    val totalIn: Int = 0,
    val totalOut: Int = 0,
    val totalFeePaid: Int = 0,
    val totalFeeEarned: Int = 0,

    val isLoading: Boolean = true,
    val error: String? = null,
    val showFilterSheet: Boolean = false
)
