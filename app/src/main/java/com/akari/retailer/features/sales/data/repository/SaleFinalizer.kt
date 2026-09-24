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
    suspend fun finalizeSale(sale: Sale): Result<String>
}
