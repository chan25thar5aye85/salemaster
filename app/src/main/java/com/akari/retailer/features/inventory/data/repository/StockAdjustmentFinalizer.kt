package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.domain.models.MovementType

/**
 * Atomically records a stock adjustment:
 *   - updates products.{id}.stockQuantity to the target value
 *   - writes one stock_movements doc with the previous/new stock and
 *     the user-supplied reason + notes
 *
 * Both writes happen in one Firestore transaction, so a network failure
 * can never leave the product's stock changed without a matching
 * movement record.
 *
 * The caller passes the target absolute stock, not the delta.
 */
interface StockAdjustmentFinalizer {

    /**
     * @param productId the product being adjusted
     * @param newStock the target stock level (must be >= 0)
     * @param reason short reason string (Stock Count, Damaged, etc.)
     * @param notes optional free-form notes
     * @param userId the acting user (defaults to "default")
     * @return Result.success(Unit) on success; Result.failure on any error
     */
    suspend fun adjustStock(
        productId: String,
        newStock: Int,
        reason: String,
        notes: String = "",
        userId: String = "default"
    ): Result<Unit>
}
