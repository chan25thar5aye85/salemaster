package com.akari.retailer.features.sales.presentation.stream

import com.akari.retailer.features.sales.domain.models.IncomeStream

sealed class IncomeStreamManagementEvent {
    data object LoadStreams : IncomeStreamManagementEvent()
    data object RefreshStreams : IncomeStreamManagementEvent()
    data class DeleteStream(val streamId: String) : IncomeStreamManagementEvent()
    data object ClearError : IncomeStreamManagementEvent()
    data class SelectTab(val tabIndex: Int) : IncomeStreamManagementEvent()
    data object ShowAddDialog : IncomeStreamManagementEvent()
    data class ShowEditDialog(val stream: IncomeStream) : IncomeStreamManagementEvent()
    data object DismissDialog : IncomeStreamManagementEvent()
    data class DialogNameChanged(val name: String) : IncomeStreamManagementEvent()
    data object SaveStream : IncomeStreamManagementEvent()
}
