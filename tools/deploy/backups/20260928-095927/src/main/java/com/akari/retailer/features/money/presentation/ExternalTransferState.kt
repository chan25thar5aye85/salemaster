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
    val fee: String = "",
    val feeType: FeeType = FeeType.NONE,
    val description: String = "",
    val lastUsedAccountId: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    fun getAmountInt(): Int = amount.toIntOrNull() ?: 0
    fun getFeeInt(): Int = fee.toIntOrNull() ?: 0
    
    fun getAccountChange(): Int {
        val amt = getAmountInt()
        val feeAmt = getFeeInt()
        return when (direction) {
            ExternalTransferDirection.OUTGOING -> {
                when (feeType) {
                    FeeType.FEE_PAID -> -(amt + feeAmt)
                    else -> -amt
                }
            }
            ExternalTransferDirection.INCOMING -> {
                when (feeType) {
                    FeeType.FEE_EARNED -> amt + feeAmt
                    FeeType.FEE_PAID -> amt - feeAmt
                    FeeType.NONE -> amt
                }
            }
        }
    }
    
    fun getNewBalance(): Int {
        val current = selectedAccount?.currentBalance ?: 0
        return current + getAccountChange()
    }
    
    fun isOutgoing(): Boolean = direction == ExternalTransferDirection.OUTGOING
}
