package com.akari.retailer.features.sales.presentation.income

import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import com.akari.retailer.features.sales.domain.models.IncomeStream

data class IncomeEntryState(
    val amount: String = "",
    val selectedStream: IncomeStream? = null,
    val streams: List<IncomeStream> = emptyList(),
    val accounts: List<MoneyAccount> = emptyList(),      // ✅ NEW
    val selectedAccountId: String = "default_cash",       // ✅ NEW
    val entryType: IncomeEntryType = IncomeEntryType.BUSINESS,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)
