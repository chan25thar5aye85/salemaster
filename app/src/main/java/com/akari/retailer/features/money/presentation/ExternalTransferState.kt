package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyAccount

enum class ExternalTransferDirection {
    OUTGOING,   // Money going out (you pay)
    INCOMING    // Money coming in (you receive)
}

data class ExternalTransferState(
    val accounts: List<MoneyAccount> = emptyList(),
    val selectedAccount: MoneyAccount? = null,
    val direction: ExternalTransferDirection = ExternalTransferDirection.OUTGOING,
    val externalAccountName: String = "",
    val externalAccountNumber: String = "",
    val amount: String = "",
    val fee: String = "",
    val feeType: FeeType = FeeType.NONE,
    val description: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    fun getAmountInt(): Int = amount.toIntOrNull() ?: 0
    fun getFeeInt(): Int = fee.toIntOrNull() ?: 0
    
    // Calculate net effect on selected account
    fun getAccountChange(): Int {
        val amt = getAmountInt()
        val feeAmt = getFeeInt()
        return when (direction) {
            ExternalTransferDirection.OUTGOING -> {
                // Money leaving account
                when (feeType) {
                    FeeType.FEE_PAID -> -(amt + feeAmt)  // Pay amount + fee
                    else -> -amt                          // Just amount
                }
            }
            ExternalTransferDirection.INCOMING -> {
                // Money entering account
                when (feeType) {
                    FeeType.FEE_EARNED -> amt + feeAmt   // Receive amount + fee
                    FeeType.FEE_PAID -> amt - feeAmt      // Receive amount - fee
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
