package com.akari.retailer.features.money.presentation

import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyAccount

sealed class ExternalTransferEvent {
    data object LoadAccounts : ExternalTransferEvent()
    data class AccountSelected(val account: MoneyAccount) : ExternalTransferEvent()
    data class DirectionChanged(val direction: ExternalTransferDirection) : ExternalTransferEvent()
    data class ExternalAccountNameChanged(val value: String) : ExternalTransferEvent()
    // ❌ Removed: ExternalAccountNumberChanged - no longer used
    data class AmountChanged(val value: String) : ExternalTransferEvent()
    data class FeeChanged(val value: String) : ExternalTransferEvent()
    data class FeeTypeChanged(val type: FeeType) : ExternalTransferEvent()
    data class DescriptionChanged(val value: String) : ExternalTransferEvent()
    data object SaveTransfer : ExternalTransferEvent()
    data object ClearError : ExternalTransferEvent()
    data object ResetSuccess : ExternalTransferEvent()
}
