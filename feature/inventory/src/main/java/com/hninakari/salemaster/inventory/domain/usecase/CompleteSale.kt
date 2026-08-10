package com.hninakari.salemaster.inventory.domain.usecase

import com.hninakari.salemaster.inventory.domain.repository.SaleRepository
import com.hninakari.salemaster.inventory.domain.repository.StockMovementRepository
import com.hninakari.salemaster.inventory.domain.repository.StockRepository
import com.hninakari.salemaster.inventory.model.SaleStatus
import com.hninakari.salemaster.inventory.model.StockMovement
import com.hninakari.salemaster.inventory.model.StockMovementSourceType
import com.hninakari.salemaster.inventory.model.StockMovementType
import java.time.Instant

class CompleteSale(
    private val saleRepository: SaleRepository,
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    suspend operator fun invoke(saleId: Long) {

        val sale = saleRepository.getSaleById(saleId)
            ?: throw IllegalArgumentException("Sale not found")

        if (sale.status != SaleStatus.DRAFT) {
            throw IllegalStateException(
                "Only draft sales can be completed"
            )
        }

        val items = saleRepository.getSaleItems(saleId)

        if (items.isEmpty()) {
            throw IllegalStateException(
                "Cannot complete a sale without items"
            )
        }

        for (item in items) {
            if (item.quantity <= 0) {
                throw IllegalStateException(
                    "Sale quantity must be greater than zero"
                )
            }

            val availableQuantity =
                stockRepository.getQuantity(item.productId)

            if (availableQuantity < item.quantity) {
                throw IllegalStateException(
                    "Insufficient stock for product ${item.productId}"
                )
            }

            stockRepository.decrease(
                productId = item.productId,
                quantity = item.quantity
            )

            stockMovementRepository.add(
                StockMovement(
                    id = 0L,
                    productId = item.productId,
                    quantity = -item.quantity,
                    type = StockMovementType.SALE,
                    sourceType = StockMovementSourceType.SALE,
                    sourceId = sale.id,
                    occurredAt = Instant.now()
                )
            )
        }

        saleRepository.updateSale(
            sale.copy(status = SaleStatus.COMPLETED)
        )
    }
}
