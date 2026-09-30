package com.akari.retailer.features.money.presentation

import com.akari.retailer.core.ui.components.TimeFilter

sealed class ExternalTransferHistoryEvent {
    data object LoadTransfers : ExternalTransferHistoryEvent()
    data object RefreshTransfers : ExternalTransferHistoryEvent()
    data class TimeFilterChanged(val filter: TimeFilter) : ExternalTransferHistoryEvent()
    data class AccountFilterChanged(val accountId: String) : ExternalTransferHistoryEvent()
    data class ExternalFilterChanged(val externalName: String) : ExternalTransferHistoryEvent()
    data object ClearFilters : ExternalTransferHistoryEvent()
    data object ClearError : ExternalTransferHistoryEvent()
}
