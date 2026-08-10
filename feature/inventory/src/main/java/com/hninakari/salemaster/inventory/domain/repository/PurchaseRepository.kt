package com.hninakari.salemaster.inventory.domain.repository

import com.hninakari.salemaster.inventory.model.Purchase
import com.hninakari.salemaster.inventory.model.PurchaseItem

interface PurchaseRepository {

    suspend fun getPurchaseById(id: Long): Purchase?

    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItem>

    suspend fun updatePurchase(purchase: Purchase)
}
