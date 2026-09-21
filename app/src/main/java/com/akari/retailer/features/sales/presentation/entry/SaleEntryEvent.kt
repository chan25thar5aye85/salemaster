package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.sales.domain.models.PaymentMethod

sealed class SaleEntryEvent {
    data class AmountChanged(val rowId: Long, val value: String) : SaleEntryEvent()
    data class RowFocused(val rowId: Long) : SaleEntryEvent()
    data class NextPressed(val rowId: Long) : SaleEntryEvent()
    data class RowDeleted(val rowId: Long) : SaleEntryEvent()
    data class PaymentSelected(val method: PaymentMethod) : SaleEntryEvent()
    data class AccountSelected(val account: MoneyAccount) : SaleEntryEvent()  // ✅ NEW
    data object SaveSale : SaleEntryEvent()
    data object ClearError : SaleEntryEvent()
    data object ResetSaveSuccess : SaleEntryEvent()
}
