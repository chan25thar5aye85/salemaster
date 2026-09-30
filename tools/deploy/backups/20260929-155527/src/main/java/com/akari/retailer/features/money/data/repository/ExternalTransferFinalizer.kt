package com.akari.retailer.features.money.data.repository

/**
 * Atomically deletes an external transfer and reverses its side-effects:
 *
 *   - reads the money account referenced by the transaction
 *   - reverses the account balance change (credit if outgoing, debit if incoming)
 *   - reverses the fee effect (credit back fee paid, debit back fee earned)
 *   - logs a compensating `money_transactions` doc labeled
 *     "External transfer delete — reversal"
 *   - deletes the original transfer doc
 *
 * Idempotent: if the transfer is already gone, returns success.
 */
interface ExternalTransferFinalizer {
    suspend fun deleteTransfer(transactionId: String): Result<Unit>
}
