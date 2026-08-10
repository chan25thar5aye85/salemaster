package com.hninakari.salemaster.inventory.domain.usecase

import com.hninakari.salemaster.inventory.domain.repository.StockMovementRepository
import com.hninakari.salemaster.inventory.domain.repository.StockRepository
import com.hninakari.salemaster.inventory.model.StockMovement
import com.hninakari.salemaster.inventory.model.StockMovementType
import java.time.Instant

class AdjustStock(
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    suspend operator fun invoke(
        productId: Long,
        quantity: Int
    ) {
        if (quantity == 0) {
            throw IllegalArgumentException(
                "Adjustment quantity cannot be zero"
            )
        }

        if (quantity > 0) {
            stockRepository.increase(
                productId = productId,
                quantity = quantity
            )
        } else {
            val decreaseQuantity = -quantity

            val availableQuantity =
                stockRepository.getQuantity(productId)

            if (availableQuantity < decreaseQuantity) {
                throw IllegalStateException(
                    "Insufficient stock for product $productId"
                )
            }

            stockRepository.decrease(
                productId = productId,
                quantity = decreaseQuantity
            )
        }

        stockMovementRepository.add(
            StockMovement(
                id = 0L,
                productId = productId,
                quantity = quantity,
                type = StockMovementType.ADJUSTMENT,
                sourceType = null,
                sourceId = null,
                occurredAt = Instant.now()
            )
        )
    }
}
