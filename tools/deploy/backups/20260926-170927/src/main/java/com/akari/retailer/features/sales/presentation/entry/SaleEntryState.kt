package com.akari.retailer.features.sales.presentation.entry

import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.features.money.domain.models.CreditAccount
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

    val customers: List<Customer> = emptyList(),
    val notes: String = "",

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

val SaleEntryState.canSave: Boolean
    get() {
        val total = rows.sumOf { it.amount ?: 0 }
        if (total <= 0) return false

        val totalPaid = paymentRows.sumOf { it.amount.toIntOrNull() ?: 0 }
        if (totalPaid < total) return false

        val hasCreditWithoutCustomer = paymentRows.any {
            it.accountId == CreditAccount.ID && it.customerId.isBlank()
        }
        if (hasCreditWithoutCustomer) return false

        return true
    }

data class SaleItemRow(
    val id: Long,
    val amount: Int? = null,
    val isFocused: Boolean = false
)
