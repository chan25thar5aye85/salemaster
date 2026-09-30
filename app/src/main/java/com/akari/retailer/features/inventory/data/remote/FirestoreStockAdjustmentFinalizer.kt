package com.akari.retailer.features.inventory.data.remote

import android.util.Log
import com.akari.retailer.features.inventory.data.repository.StockAdjustmentFinalizer
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreStockAdjustmentFinalizer(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : StockAdjustmentFinalizer {

    private val TAG = "StockAdjFinalizer"

    private val productsCol get() = db.collection("products")
    private val movementsCol get() = db.collection("stock_movements")

    override suspend fun adjustStock(
        productId: String,
        newStock: Int,
        reason: String,
        notes: String,
        userId: String
    ): Result<Unit> {
        return try {
            require(productId.isNotEmpty()) { "productId is required" }
            require(newStock >= 0) { "newStock must be >= 0" }
            require(reason.isNotBlank()) { "reason is required" }

            val now = System.currentTimeMillis()
            val productRef = productsCol.document(productId)

            db.runTransaction { txn ->
                // READ first — transaction rule requires all reads before writes.
                val snap = txn.get(productRef)
                if (!snap.exists()) {
                    throw IllegalStateException("Product not found: $productId")
                }

                val previousStock = (snap.getLong("stockQuantity") ?: 0L).toInt()
                if (previousStock == newStock) {
                    // Nothing to do — don't write a zero-delta movement.
                    return@runTransaction
                }
                val delta = newStock - previousStock

                // WRITE — product
                txn.update(
                    productRef,
                    "stockQuantity", newStock,
                    "updatedAt", now
                )

                // WRITE — movement
                txn.set(movementsCol.document(), mapOf(
                    "productId"       to productId,
                    "type"            to MovementType.ADJUSTMENT.name,
                    "quantity"        to delta,
                    "previousStock"   to previousStock,
                    "newStock"        to newStock,
                    "reason"          to reason,
                    "notes"           to notes,
                    "saleId"          to "",
                    "purchaseOrderId" to "",
                    "createdAt"       to now,
                    "userId"          to userId
                ))
            }.await()

            Log.d(TAG, "Stock adjusted atomically: $productId -> $newStock")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "adjustStock failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
