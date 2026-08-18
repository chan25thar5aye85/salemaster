package com.hninakari.saletracker.sales.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hninakari.saletracker.sales.data.models.PaymentMethod
import com.hninakari.saletracker.sales.data.models.Sales
import java.time.LocalDateTime

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey
    val id: String,
    val amount: Double,
    val paymentMethod: String,
    val saleDate: String
) {
    fun toDomain(): Sales {
        return Sales(
            id = id,
            amount = amount,
            paymentMethod = PaymentMethod.valueOf(paymentMethod),
            saleDate = LocalDateTime.parse(saleDate)
        )
    }

    companion object {
        fun fromDomain(sale: Sales): SaleEntity {
            return SaleEntity(
                id = sale.id,
                amount = sale.amount,
                paymentMethod = sale.paymentMethod.name,
                saleDate = sale.saleDate.toString()
            )
        }
    }
}
