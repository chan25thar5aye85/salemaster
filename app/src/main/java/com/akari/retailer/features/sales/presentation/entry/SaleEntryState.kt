package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.features.sales.domain.models.PaymentMethod
import com.akari.retailer.features.sales.domain.models.Sale

data class SaleEntryState(
    val rows: List<SaleItemRow> = listOf(
        SaleItemRow(
            id = 1L,
            isFocused = true
        )
    ),
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
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
