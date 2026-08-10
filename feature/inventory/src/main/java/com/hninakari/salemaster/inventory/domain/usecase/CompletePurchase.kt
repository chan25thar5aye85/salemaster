package com.hninakari.salemaster.inventory.domain.usecase

import com.hninakari.salemaster.inventory.domain.repository.PurchaseRepository
import com.hninakari.salemaster.inventory.domain.repository.StockMovementRepository
import com.hninakari.salemaster.inventory.domain.repository.StockRepository
import com.hninakari.salemaster.inventory.model.PurchaseStatus
import com.hninakari.salemaster.inventory.model.StockMovement
import com.hninakari.salemaster.inventory.model.StockMovementSourceType
import com.hninakari.salemaster.inventory.model.StockMovementType
import java.time.Instant

class CompletePurchase(
    private val purchaseRepository: PurchaseRepository,
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    suspend operator fun invoke(purchaseId: Long) {

        val purchase = purchaseRepository.getPurchaseById(purchaseId)
            ?: throw IllegalArgumentException("Purchase not found")

        if (purchase.status != PurchaseStatus.DRAFT) {
            throw IllegalStateException(
                "Only draft purchases can be completed"
            )
        }

        val items = purchaseRepository.getPurchaseItems(purchaseId)

        if (items.isEmpty()) {
            throw IllegalStateException(
                "Cannot complete a purchase without items"
            )
        }

        for (item in items) {
            if (item.quantity <= 0) {
                throw IllegalStateException(
                    "Purchase quantity must be greater than zero"
                )
            }

            stockRepository.increase(
                productId = item.productId,
                quantity = item.quantity
            )

            stockMovementRepository.add(
                StockMovement(
                    id = 0L,
                    productId = item.productId,
                    quantity = item.quantity,
                    type = StockMovementType.PURCHASE,
                    sourceType = StockMovementSourceType.PURCHASE,
                    sourceId = purchase.id,
                    occurredAt = Instant.now()
                )
            )
        }

        purchaseRepository.updatePurchase(
            purchase.copy(status = PurchaseStatus.COMPLETED)
        )
    }
}
