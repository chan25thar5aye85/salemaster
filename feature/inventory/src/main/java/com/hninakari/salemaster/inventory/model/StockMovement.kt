package com.hninakari.salemaster.inventory.model

import java.time.Instant

data class StockMovement(
    val id: Long,
    val productId: Long,
    val quantity: Int,
    val type: StockMovementType,
    val sourceType: StockMovementSourceType?,
    val sourceId: Long?,
    val occurredAt: Instant
)

enum class StockMovementType {
    PURCHASE,
    SALE,
    RETURN,
    DAMAGE,
    ADJUSTMENT
}

enum class StockMovementSourceType {
    PURCHASE,
    SALE,
    RETURN
}
