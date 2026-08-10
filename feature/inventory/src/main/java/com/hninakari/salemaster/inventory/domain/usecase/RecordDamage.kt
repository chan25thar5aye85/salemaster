package com.hninakari.salemaster.inventory.domain.usecase

import com.hninakari.salemaster.inventory.domain.repository.StockMovementRepository
import com.hninakari.salemaster.inventory.domain.repository.StockRepository
import com.hninakari.salemaster.inventory.model.StockMovement
import com.hninakari.salemaster.inventory.model.StockMovementType
import java.time.Instant

class RecordDamage(
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    suspend operator fun invoke(
        productId: Long,
        quantity: Int
    ) {
        if (quantity <= 0) {
            throw IllegalArgumentException(
                "Damage quantity must be greater than zero"
            )
        }

        val availableQuantity =
            stockRepository.getQuantity(productId)

        if (availableQuantity < quantity) {
            throw IllegalStateException(
                "Insufficient stock for product $productId"
            )
        }

        stockRepository.decrease(
            productId = productId,
            quantity = quantity
        )

        stockMovementRepository.add(
            StockMovement(
                id = 0L,
                productId = productId,
                quantity = -quantity,
                type = StockMovementType.DAMAGE,
                sourceType = null,
                sourceId = null,
                occurredAt = Instant.now()
            )
        )
    }
}
