package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType

enum class TransactionTimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL
}

data class MoneyTransactionsState(
    val transactions: List<MoneyTransaction> = emptyList(),
    val filteredTransactions: List<MoneyTransaction> = emptyList(),
    val accounts: List<MoneyAccount> = emptyList(),
    
    // Filters
    val selectedAccountId: String = "",          // Empty = all accounts
    val selectedType: MoneyTransactionType? = null,  // Null = all types
    val timeRange: TransactionTimeRange = TransactionTimeRange.ALL,
    val searchQuery: String = "",
    
    // Summary
    val totalIn: Int = 0,        // Total money in
    val totalOut: Int = 0,       // Total money out
    val totalFeePaid: Int = 0,   // Total fees paid
    val totalFeeEarned: Int = 0, // Total fees earned
    
    // UI State
    val isLoading: Boolean = true,
    val error: String? = null,
    val showFilterSheet: Boolean = false
)
