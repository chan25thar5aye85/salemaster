package com.hninakari.salemaster.inventory.model

import java.time.Instant

data class Sale(
    val id: Long,
    val reference: String?,
    val soldAt: Instant,
    val status: SaleStatus
)

enum class SaleStatus {
    DRAFT,
    COMPLETED,
    CANCELLED
}
