package com.akari.retailer.core.utils

import com.google.firebase.firestore.DocumentSnapshot
import com.akari.retailer.features.sales.domain.models.PaymentMethod
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.features.sales.domain.models.SaleItem
import java.util.Date

object FirestoreMapper {
    
    private const val FIELD_ITEMS = "items"
    private const val FIELD_TOTAL = "total"
    private const val FIELD_PAYMENT_METHOD = "paymentMethod"
    private const val FIELD_TIMESTAMP = "timestamp"
    private const val FIELD_CASHIER_ID = "cashierId"
    
    fun toMap(sale: Sale): Map<String, Any> {
        val itemsMap = sale.items.map { item ->
            mapOf(
                "productId" to item.productId,
                "quantity" to item.quantity,
                "price" to item.price,
                "total" to item.total
            )
        }
        
        return mapOf(
            FIELD_ITEMS to itemsMap,
            FIELD_TOTAL to sale.total,
            FIELD_PAYMENT_METHOD to sale.paymentMethod.name,
            FIELD_TIMESTAMP to sale.timestamp,
            FIELD_CASHIER_ID to sale.cashierId
        )
    }
    
    fun toSale(documentId: String, data: Map<String, Any>): Sale? {
        return try {
            val itemsData = data[FIELD_ITEMS] as? List<*> ?: return null
            val items = itemsData.mapNotNull { itemData ->
                if (itemData is Map<*, *>) {
                    val productId = itemData["productId"] as? String ?: ""
                    val quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0
                    val price = (itemData["price"] as? Number)?.toInt() ?: 0
                    val total = (itemData["total"] as? Number)?.toInt() ?: 0
                    SaleItem(productId, quantity, price, total)
                } else {
                    null
                }
            }
            
            val total = when (val t = data[FIELD_TOTAL]) {
                is Int -> t
                is Long -> t.toInt()
                is Double -> t.toInt()
                else -> return null
            }
            
            val paymentMethodName = data[FIELD_PAYMENT_METHOD] as? String ?: "CASH"
            val paymentMethod = try {
                PaymentMethod.valueOf(paymentMethodName)
            } catch (e: IllegalArgumentException) {
                PaymentMethod.CASH
            }
            
            val timestamp = when (val ts = data[FIELD_TIMESTAMP]) {
                is Long -> ts
                is Int -> ts.toLong()
                is Date -> ts.time
                else -> System.currentTimeMillis()
            }
            
            val cashierId = data[FIELD_CASHIER_ID] as? String ?: "default"
            
            Sale(
                id = documentId,
                items = items,
                total = total,
                paymentMethod = paymentMethod,
                timestamp = timestamp,
                cashierId = cashierId
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun documentToSale(document: DocumentSnapshot): Sale? {
        val data = document.data ?: return null
        return toSale(document.id, data)
    }
}
