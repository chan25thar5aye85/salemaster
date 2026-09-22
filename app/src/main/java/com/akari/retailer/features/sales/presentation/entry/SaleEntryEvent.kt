package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.features.money.domain.models.MoneyAccount

sealed class SaleEntryEvent {
    data class AmountChanged(val rowId: Long, val value: String) : SaleEntryEvent()
    data class RowFocused(val rowId: Long) : SaleEntryEvent()
    data class NextPressed(val rowId: Long) : SaleEntryEvent()
    data class RowDeleted(val rowId: Long) : SaleEntryEvent()
    
    // Payment events
    data class PaymentAccountChanged(val rowId: Long, val account: MoneyAccount) : SaleEntryEvent()
    data class PaymentAmountChanged(val rowId: Long, val amount: String) : SaleEntryEvent()
    data object AddPaymentRow : SaleEntryEvent()
    data class RemovePaymentRow(val rowId: Long) : SaleEntryEvent()
    
    data object SaveSale : SaleEntryEvent()
    data object ClearError : SaleEntryEvent()
    data object ResetSaveSuccess : SaleEntryEvent()
}
