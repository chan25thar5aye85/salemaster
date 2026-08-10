package com.hninakari.salemaster.inventory.data.mapper

import com.hninakari.salemaster.inventory.data.local.entity.ProductEntity
import com.hninakari.salemaster.inventory.model.Product

fun ProductEntity.toDomain(): Product =
    Product(
        id = id,
        name = name,
        sku = sku,
        barcode = barcode,
        sellPrice = sellPrice,
        categoryId = categoryId,
        isActive = isActive
    )

fun Product.toEntity(): ProductEntity =
    ProductEntity(
        id = id,
        name = name,
        sku = sku,
        barcode = barcode,
        sellPrice = sellPrice,
        categoryId = categoryId,
        isActive = isActive
    )
