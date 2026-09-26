package com.akari.retailer.features.money.presentation

import com.akari.retailer.core.ui.components.TimeFilter
import com.akari.retailer.core.ui.components.TimeFilterPreset
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.MoneyTransaction

data class ExternalAccountSummary(
    val name: String,
    val accountNumber: String,
    val sent: Int,
    val received: Int,
    val count: Int
)

data class ExternalTransferHistoryState(
    val transfers: List<MoneyTransaction> = emptyList(),
    val filteredTransfers: List<MoneyTransaction> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Filters
    val timeFilter: TimeFilter = TimeFilter(preset = TimeFilterPreset.THIS_WEEK),
    val accounts: List<MoneyAccount> = emptyList(),
    val selectedAccountId: String = "",     // "" = All
    val externalAccountNames: List<String> = emptyList(),
    val selectedExternalName: String = "",  // "" = All

    // Totals
    val totalSent: Int = 0,
    val totalReceived: Int = 0,
    val sentCount: Int = 0,
    val receivedCount: Int = 0,
    val netFlow: Int = 0,

    /**
     * Grouped totals per external account (kept for compatibility with VM).
     */
    val byExternalAccount: List<ExternalAccountSummary> = emptyList(),
)
