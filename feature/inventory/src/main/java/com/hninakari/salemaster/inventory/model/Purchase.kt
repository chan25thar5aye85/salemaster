package com.hninakari.salemaster.inventory.model

import java.time.Instant

data class Purchase(
    val id: Long,
    val supplierId: Long,
    val reference: String?,
    val purchasedAt: Instant
)
