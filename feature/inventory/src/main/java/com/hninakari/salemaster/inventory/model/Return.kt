package com.hninakari.salemaster.inventory.model

import java.time.Instant

data class Return(
    val id: Long,
    val type: ReturnType,
    val referenceId: Long?,
    val returnedAt: Instant,
    val status: ReturnStatus
)

enum class ReturnType {
    CUSTOMER,
    SUPPLIER
}

enum class ReturnStatus {
    DRAFT,
    COMPLETED,
    CANCELLED
}
