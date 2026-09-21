package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.sales.domain.models.Sale

data class SaleEntryState(
    val rows: List<SaleItemRow> = listOf(
        SaleItemRow(
            id = 1L,
            isFocused = true
        )
    ),
    val accounts: List<MoneyAccount> = emptyList(),
    val selectedAccountId: String = "default_cash",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val recentSales: List<Sale> = emptyList()
)

data class SaleItemRow(
    val id: Long,
    val amount: Int? = null,
    val isFocused: Boolean = false
)
