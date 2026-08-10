package com.hninakari.salemaster.inventory.data.repository

import com.hninakari.salemaster.inventory.data.local.dao.ProductDao
import com.hninakari.salemaster.inventory.data.mapper.toDomain
import com.hninakari.salemaster.inventory.data.mapper.toEntity
import com.hninakari.salemaster.inventory.domain.repository.ProductRepository
import com.hninakari.salemaster.inventory.model.Product

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {

    override suspend fun getAll(): List<Product> =
        productDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: Long): Product? =
        productDao.getById(id)?.toDomain()

    override suspend fun create(product: Product): Long =
        productDao.insert(product.toEntity())

    override suspend fun update(product: Product) {
        productDao.update(product.toEntity())
    }

    override suspend fun delete(id: Long) {
        productDao.deleteById(id)
    }
}
