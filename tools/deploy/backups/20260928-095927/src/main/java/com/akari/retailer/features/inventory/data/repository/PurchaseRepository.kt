package com.akari.retailer.features.inventory.data.repository

import com.akari.retailer.features.inventory.domain.models.Purchase
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {
    suspend fun createPurchase(purchase: Purchase): Result<String>
    fun getPurchases(): Flow<List<Purchase>>
    fun getPurchaseById(purchaseId: String): Flow<Purchase?>
    suspend fun deletePurchase(purchaseId: String): Result<Unit>
}
