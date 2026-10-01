package com.akari.retailer.features.debt.presentation

import com.akari.retailer.features.customer.domain.models.AgingBucket
import com.akari.retailer.features.customer.domain.models.AgingReport
import com.akari.retailer.features.customer.domain.models.PartyAging

/**
 * Which side of the ledger the screen is currently showing.
 */
enum class DebtTab {
    RECEIVABLES,   // customers owe us
    PAYABLES       // we owe suppliers
}

/**
 * A single debtor/creditor row for display.
 */
data class DebtParty(
    val partyId: String,
    val partyName: String,
    val amount: Int,
    val oldestDays: Int
)

/**
 * Everything DebtOverviewScreen needs to render.
 *
 * Both sides are loaded simultaneously (small datasets), so tab switching
 * is instant with no re-query.
 */
data class DebtOverviewState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Which tab is active
    val tab: DebtTab = DebtTab.RECEIVABLES,

    // Receivables (customers owe us)
    val receivableTotal: Int = 0,
    val receivablePartyCount: Int = 0,
    val receivableBuckets: Map<AgingBucket, Int> = emptyMap(),
    val receivableParties: List<DebtParty> = emptyList(),

    // Payables (we owe suppliers)
    val payableTotal: Int = 0,
    val payablePartyCount: Int = 0,
    val payableBuckets: Map<AgingBucket, Int> = emptyMap(),
    val payableParties: List<DebtParty> = emptyList(),

    // ── Add-debt dialog state ──
    val showAddDialog: Boolean = false,
    val addAmount: String = "",
    val addNote: String = "",
    val selectedCustomerId: String = "",
    val selectedCustomerName: String = "",
    val selectedSupplierId: String = "",
    val selectedSupplierName: String = "",
    val isSaving: Boolean = false,
    val addError: String? = null,
    val addSuccess: Boolean = false,

    // ── Pay dialog state ──
    val showPayDialog: Boolean = false,
    val payAmount: String = "",
    val payNote: String = "",
    val paySelectedCustomerId: String = "",
    val paySelectedCustomerName: String = "",
    val paySelectedSupplierId: String = "",
    val paySelectedSupplierName: String = "",
    val payAccounts: List<com.akari.retailer.features.money.domain.models.MoneyAccount> = emptyList(),
    val paySelectedAccount: com.akari.retailer.features.money.domain.models.MoneyAccount? = null,
    val isPaying: Boolean = false,
    val payError: String? = null,
    val paySuccess: Boolean = false
) {
    val activeTotal: Int get() = if (tab == DebtTab.RECEIVABLES) receivableTotal else payableTotal
    val activeParties: List<DebtParty> get() =
        if (tab == DebtTab.RECEIVABLES) receivableParties else payableParties
    val activeBuckets: Map<AgingBucket, Int> get() =
        if (tab == DebtTab.RECEIVABLES) receivableBuckets else payableBuckets
}
