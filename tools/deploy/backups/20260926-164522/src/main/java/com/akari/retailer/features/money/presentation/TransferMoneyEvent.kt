package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyAccount

sealed class TransferMoneyEvent {
    data object LoadAccounts : TransferMoneyEvent()
    data class FromAccountSelected(val account: MoneyAccount) : TransferMoneyEvent()
    data class ToAccountSelected(val account: MoneyAccount) : TransferMoneyEvent()
    data class AmountChanged(val value: String) : TransferMoneyEvent()
    data class FeeChanged(val value: String) : TransferMoneyEvent()
    data class FeeTypeChanged(val type: FeeType) : TransferMoneyEvent()
    data class DescriptionChanged(val value: String) : TransferMoneyEvent()
    data object SaveTransfer : TransferMoneyEvent()
    data object ClearError : TransferMoneyEvent()
    data object ResetSuccess : TransferMoneyEvent()
}
