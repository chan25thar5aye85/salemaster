package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.MoneyAccount

data class MoneyAccountsState(
    val accounts: List<MoneyAccount> = emptyList(),
    val activeAccounts: List<MoneyAccount> = emptyList(),
    val totalBalance: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDialog: Boolean = false,
    val editingAccount: MoneyAccount? = null,
    val dialogName: String = "",
    val dialogIcon: String = "💵",
    val dialogColor: String = "#4CAF50",
    val dialogOpeningBalance: String = "0",
    val dialogAccountNumber: String = "",
    val dialogNotes: String = "",
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val pendingDeleteId: String? = null
)
