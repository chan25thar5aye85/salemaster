package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyAccount

data class TransferMoneyState(
    val accounts: List<MoneyAccount> = emptyList(),
    val fromAccount: MoneyAccount? = null,
    val toAccount: MoneyAccount? = null,
    val amount: String = "",
    val fee: String = "",
    val feeType: FeeType = FeeType.NONE,
    val description: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
) {
    // Calculate net effect on source account
    fun getSourceDeduction(): Int {
        val amt = amount.toIntOrNull() ?: 0
        val feeAmt = fee.toIntOrNull() ?: 0
        return when (feeType) {
            FeeType.FEE_PAID -> amt + feeAmt
            else -> amt
        }
    }
    
    // Calculate net effect on destination account
    fun getDestinationAddition(): Int {
        val amt = amount.toIntOrNull() ?: 0
        val feeAmt = fee.toIntOrNull() ?: 0
        return when (feeType) {
            FeeType.FEE_EARNED -> amt + feeAmt
            else -> amt
        }
    }
    
    fun getFromNewBalance(): Int {
        val from = fromAccount?.currentBalance ?: 0
        return from - getSourceDeduction()
    }
    
    fun getToNewBalance(): Int {
        val to = toAccount?.currentBalance ?: 0
        return to + getDestinationAddition()
    }
}
