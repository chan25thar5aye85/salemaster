package com.akari.retailer.features.sales.data.repository

import com.akari.retailer.features.sales.domain.models.Sale

/**
 * Atomically finalizes a sale:
 *   - writes the sale document
 *   - applies all money payments (creates money_transactions, updates money_accounts)
 *   - applies all credit payments (creates credit_transactions, updates customers.creditBalance)
 *
 * Either everything succeeds or nothing does.
 */
interface SaleFinalizer {
    /**
     * Atomically finalize a sale.
     *
     * @param sale the sale to record
     * @param overpaymentCredit if not null, the sale was overpaid by this amount.
     *         The chosen customer's creditBalance goes negative by [PaymentEntry.amount].
     *         Their balance will reflect "we owe them".
     */
    suspend fun finalizeSale(
        sale: Sale,
        overpaymentCredit: com.akari.retailer.features.money.domain.models.PaymentEntry? = null
    ): Result<String>

    /**
     * Atomically delete a sale and reverse everything it did:
     *   - reverses money payments (accounts -= amount, logs ADJUSTMENT)
     *   - reverses credit payments (customer balance -= amount, logs PAYMENT)
     *   - reverses overpayment credit (customer balance += amount, logs SALE_ON_CREDIT)
     *   - deletes the sale document
     *
     * Sales created before the overpayment-credit field existed will not
     * reverse their overpayment credit (no data to know about it).
     */
    suspend fun deleteSale(saleId: String): Result<Unit>
}
