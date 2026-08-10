package com.hninakari.salemaster.inventory.domain.repository

import com.hninakari.salemaster.inventory.model.StockMovement

interface StockMovementRepository {

    suspend fun add(movement: StockMovement)
}
