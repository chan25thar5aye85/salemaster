package com.akari.retailer.features.money.domain.models

/**
 * A single payment applied to a sale, expense, income, or purchase.
 *
 * For normal money payments: [accountId] is a real money account.
 * For credit payments (only used on sales): [accountId] is the special
 * [CreditAccount.ID] sentinel value, and [customerId] identifies who owes.
 */
data class PaymentEntry(
    val accountId: String = "",
    val amount: Int = 0,
    val customerId: String = ""
) {
    val isCredit: Boolean
        get() = accountId == CreditAccount.ID
}

/**
 * Reserved pseudo-account used to represent "pay on credit".
 * Never stored in Firestore — it's a UI/routing marker only.
 */
object CreditAccount {
    const val ID = "__credit__"
    const val DISPLAY_NAME = "Credit"
}
