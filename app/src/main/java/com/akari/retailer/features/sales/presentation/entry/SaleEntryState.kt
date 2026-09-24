package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.features.customer.domain.models.Customer
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.sales.domain.models.Sale

data class SaleEntryState(
    val rows: List<SaleItemRow> = listOf(
        SaleItemRow(id = 1L, isFocused = true)
    ),
    val accounts: List<MoneyAccount> = emptyList(),
    val paymentRows: List<PaymentRow> = listOf(
        PaymentRow(id = 1L, accountId = "default_cash", amount = "")
    ),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    val recentSales: List<Sale> = emptyList(),

    // ── Credit sale mode ──
    val isCreditSale: Boolean = false,
    val creditCustomer: Customer? = null,
    val creditNotes: String = "",
    val customers: List<Customer> = emptyList(),
    val showCreditCustomerPicker: Boolean = false,

    // Overpayment dialog
    val showOverpaymentDialog: Boolean = false,
    val pendingOverpaymentAmount: Int = 0,
    val overpaymentMode: OverpaymentMode = OverpaymentMode.NONE,
    val overpaymentCustomer: Customer? = null
)

enum class OverpaymentMode {
    NONE,
    CREDIT_TO_CUSTOMER,
    KEEP_IN_ACCOUNT
}

data class SaleItemRow(
    val id: Long,
    val amount: Int? = null,
    val isFocused: Boolean = false
)
