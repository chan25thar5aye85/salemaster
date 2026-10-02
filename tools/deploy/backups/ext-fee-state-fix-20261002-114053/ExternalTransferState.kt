package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyAccount

enum class ExternalTransferDirection {
    OUTGOING,
    INCOMING
}

data class ExternalTransferState(
    val accounts: List<MoneyAccount> = emptyList(),
    val selectedAccount: MoneyAccount? = null,
    val direction: ExternalTransferDirection = ExternalTransferDirection.OUTGOING,
    val externalAccountName: String = "",
    /**
     * Unique external-account names from past transfers, most recent first.
     * Drives the autocomplete dropdown on the External Account Name field.
     */
    val knownExternalNames: List<String> = emptyList(),
    val amount: String = "",
    val fee: String = "0",
    val feeType: FeeType = FeeType.NONE,
    val description: String = "",
    val lastUsedAccountId: String = "",
    /**
     * The 5 most recent external transfers, newest first. Drives the
     * Recent Transfers card below the form.
     */
    val recentTransfers: List<com.akari.retailer.features.money.domain.models.MoneyTransaction> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    fun getAmountInt(): Int = amount.toIntOrNull() ?: 0
    fun getFeeInt(): Int = fee.toIntOrNull() ?: 0
    
    /**
     * The change to the SELECTED account. The fee is a separate
     * transaction that always lands in the Cash account, so it is
     * NOT included in this number.
     */
    fun getAccountChange(): Int {
        val amt = getAmountInt()
        return when (direction) {
            ExternalTransferDirection.OUTGOING -> -amt
            ExternalTransferDirection.INCOMING -> amt
        }
    }
    
    fun getNewBalance(): Int {
        val current = selectedAccount?.currentBalance ?: 0
        return current + getAccountChange()
    }
    
    fun isOutgoing(): Boolean = direction == ExternalTransferDirection.OUTGOING
}
