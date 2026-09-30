package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.domain.models.Purchase
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    // REMOVED: createPurchase — the only correct way to create a purchase
    // is via PurchaseFinalizer.finalizePurchase(), which writes atomically
    // (stock + expense + money + supplier + order status).

    fun getPurchases(): Flow<List<Purchase>>
    fun getPurchaseById(purchaseId: String): Flow<Purchase?>
    suspend fun deletePurchase(purchaseId: String): Result<Unit>
}
