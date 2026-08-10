package com.hninakari.salemaster.inventory.domain.repository

import com.hninakari.salemaster.inventory.model.Return
import com.hninakari.salemaster.inventory.model.ReturnItem

interface ReturnRepository {

    suspend fun getReturnById(id: Long): Return?

    suspend fun getReturnItems(returnId: Long): List<ReturnItem>

    suspend fun updateReturn(returnTransaction: Return)
}
