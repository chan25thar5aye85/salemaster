package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder

/**
 * Atomically finalizes a purchase from a received purchase order:
 *
 *   - writes `purchases/{orderId}` (idempotent — orderId is the purchase id)
 *   - increments stock on each product
 *   - writes stock_movements docs (with real previousStock/newStock)
 *   - creates a business expense for the full total
 *   - debits money accounts + writes money_transactions for each payment
 *   - increments supplier.totalPurchased
 *   - increments supplier.payableBalance by the credit portion
 *   - writes a supplier_transactions PURCHASE_ON_CREDIT row for the credit portion
 *   - flips the purchase order status to COMPLETED
 *
 * If any write fails, everything rolls back.
 *
 * Rejects re-finalization if the order is already COMPLETED.
 */
interface PurchaseFinalizer {

    /**
     * @param order the received purchase order to finalize
     * @param payments the money payments applied (may be empty — full credit)
     * @param receiptNumber pre-generated receipt number (e.g. "RCP-20260926-1234")
     * @return the purchase id (= order id) on success
     */
    suspend fun finalizePurchase(
        order: PurchaseOrder,
        payments: List<PaymentEntry>,
        receiptNumber: String
    ): Result<String>

    /**
     * Atomically delete a purchase and reverse every side-effect:
     *   - decrements product stock
     *   - writes reversal stock_movements
     *   - re-credits money accounts + writes ADJUSTMENT money_transactions
     *   - decrements supplier.totalPurchased
     *   - decrements supplier.payableBalance by the credit portion
     *   - writes a PAYMENT supplier_transactions reversal row for the credit portion
     *   - flips the order status back to RECEIVED
     */
    suspend fun deletePurchase(
        purchaseId: String,
        orderId: String
    ): Result<Unit>
}
