package com.akari.retailer.features.sales.presentation.income

import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.features.sales.domain.models.IncomeStream

sealed class IncomeEntryEvent {
    data class AmountChanged(val value: String) : IncomeEntryEvent()
    data class StreamSelected(val stream: IncomeStream) : IncomeEntryEvent()
    data class EntryTypeChanged(val type: IncomeEntryType) : IncomeEntryEvent()
    data class DescriptionChanged(val value: String) : IncomeEntryEvent()
    data class DateChanged(val date: Long) : IncomeEntryEvent()
    data object SaveIncome : IncomeEntryEvent()
    data object ClearError : IncomeEntryEvent()
    data object ResetSuccess : IncomeEntryEvent()
}
