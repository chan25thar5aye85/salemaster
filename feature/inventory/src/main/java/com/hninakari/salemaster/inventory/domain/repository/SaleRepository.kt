package com.hninakari.salemaster.inventory.domain.repository

import com.hninakari.salemaster.inventory.model.Sale
import com.hninakari.salemaster.inventory.model.SaleItem

interface SaleRepository {

    suspend fun getSaleById(id: Long): Sale?

    suspend fun getSaleItems(saleId: Long): List<SaleItem>

    suspend fun updateSale(sale: Sale)
}
