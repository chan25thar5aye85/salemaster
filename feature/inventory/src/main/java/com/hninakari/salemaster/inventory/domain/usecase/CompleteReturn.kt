package com.hninakari.salemaster.inventory.domain.usecase

import com.hninakari.salemaster.inventory.domain.repository.ReturnRepository
import com.hninakari.salemaster.inventory.domain.repository.StockMovementRepository
import com.hninakari.salemaster.inventory.domain.repository.StockRepository
import com.hninakari.salemaster.inventory.model.ReturnStatus
import com.hninakari.salemaster.inventory.model.ReturnType
import com.hninakari.salemaster.inventory.model.StockMovement
import com.hninakari.salemaster.inventory.model.StockMovementSourceType
import com.hninakari.salemaster.inventory.model.StockMovementType
import java.time.Instant

class CompleteReturn(
    private val returnRepository: ReturnRepository,
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    suspend operator fun invoke(returnId: Long) {

        val returnTransaction = returnRepository.getReturnById(returnId)
            ?: throw IllegalArgumentException("Return not found")

        if (returnTransaction.status != ReturnStatus.DRAFT) {
            throw IllegalStateException(
                "Only draft returns can be completed"
            )
        }

        val items = returnRepository.getReturnItems(returnId)

        if (items.isEmpty()) {
            throw IllegalStateException(
                "Cannot complete a return without items"
            )
        }

        for (item in items) {
            if (item.quantity <= 0) {
                throw IllegalStateException(
                    "Return quantity must be greater than zero"
                )
            }

            val stockChange = when (returnTransaction.type) {
                ReturnType.CUSTOMER -> item.quantity
                ReturnType.SUPPLIER -> -item.quantity
            }

            if (stockChange < 0) {
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
            } else {
                stockRepository.increase(
                    productId = item.productId,
                    quantity = item.quantity
                )
            }

            stockMovementRepository.add(
                StockMovement(
                    id = 0L,
                    productId = item.productId,
                    quantity = stockChange,
                    type = StockMovementType.RETURN,
                    sourceType = StockMovementSourceType.RETURN,
                    sourceId = returnTransaction.id,
                    occurredAt = Instant.now()
                )
            )
        }

        returnRepository.updateReturn(
            returnTransaction.copy(
                status = ReturnStatus.COMPLETED
            )
        )
    }
}
