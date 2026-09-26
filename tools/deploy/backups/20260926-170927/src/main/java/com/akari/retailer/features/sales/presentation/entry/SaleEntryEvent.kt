package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.features.customer.domain.models.Customer
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

    // Credit-as-payment events (per-row credit)
    data class PaymentCreditSelected(val rowId: Long) : SaleEntryEvent()
    data class PaymentCustomerSelected(val rowId: Long, val customer: Customer) : SaleEntryEvent()

    // Overpayment dialog events
    data class OverpaymentModeChanged(val mode: OverpaymentMode) : SaleEntryEvent()
    data class OverpaymentCustomerSelected(val customer: Customer) : SaleEntryEvent()
    data object ConfirmOverpayment : SaleEntryEvent()
    data object DismissOverpaymentDialog : SaleEntryEvent()

    data class NotesChanged(val value: String) : SaleEntryEvent()

    data object SaveSale : SaleEntryEvent()
    data object ClearError : SaleEntryEvent()
    data object ResetSaveSuccess : SaleEntryEvent()
}
