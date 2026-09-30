package com.akari.retailer.features.sales.data.repository

import android.util.Log
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.features.sales.domain.models.SaleItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Read-only sale access for reporting/analytics (P&L, Trends).
 * Writes go through FirestoreSaleFinalizer — this repo intentionally has
 * no add/delete paths.
 */
class FirestoreSaleRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SaleRepository {

    private val TAG = "FirestoreSaleRepo"
    private fun col() = db.collection("sales")

    override suspend fun saveSale(sale: Sale): Result<String> {
        // Not supported — sales are created via FirestoreSaleFinalizer.
        return Result.failure(IllegalStateException(
            "saveSale is not supported by FirestoreSaleRepository. " +
            "Use FirestoreSaleFinalizer.finalizeSale() instead."
        ))
    }

    override suspend fun deleteSale(saleId: String): Result<Unit> {
        return Result.failure(IllegalStateException(
            "deleteSale is not supported by FirestoreSaleRepository. " +
            "Use FirestoreSaleFinalizer.deleteSale() instead."
        ))
    }

    override fun getSales(): Flow<List<Sale>> = queryFlow(
        col().orderBy("timestamp", Query.Direction.DESCENDING)
    )

    override fun getRecentSales(limit: Int): Flow<List<Sale>> = queryFlow(
        col().orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
    )

    override fun getSalesHistory(limit: Int): Flow<List<Sale>> = queryFlow(
        col().orderBy("timestamp", Query.Direction.DESCENDING).limit(limit.toLong())
    )

    override fun getTodaySales(): Flow<List<Sale>> {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return queryFlow(
            col()
                .whereGreaterThanOrEqualTo("timestamp", cal.timeInMillis)
                .orderBy("timestamp", Query.Direction.DESCENDING)
        )
    }

    override fun getSaleById(saleId: String): Flow<Sale?> = callbackFlow {
        val listener = col().document(saleId).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "getSaleById error (transient, continuing): ${error.message}")
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                trySend(null); return@addSnapshotListener
            }
            trySend(mapDoc(snap.id, snap.data ?: emptyMap()))
        }
        awaitClose { listener.remove() }
    }

    private fun queryFlow(q: com.google.firebase.firestore.Query): Flow<List<Sale>> = callbackFlow {
        val listener = q.addSnapshotListener { snap, error ->
            if (error != null) {
                Log.w(TAG, "listener error (transient, continuing): ${error.message}")
                return@addSnapshotListener
            }
            if (snap == null) { trySend(emptyList()); return@addSnapshotListener }
            trySend(snap.documents.mapNotNull { mapDoc(it.id, it.data ?: emptyMap()) })
        }
        awaitClose { listener.remove() }
    }

    private fun mapDoc(id: String, data: Map<String, Any>): Sale? {
        return try {
            val items = (data["items"] as? List<*>)?.mapNotNull { row ->
                if (row is Map<*, *>) SaleItem(
                    productId = row["productId"] as? String ?: "",
                    quantity  = (row["quantity"] as? Number)?.toInt() ?: 0,
                    price     = (row["price"]    as? Number)?.toInt() ?: 0,
                    total     = (row["total"]    as? Number)?.toInt() ?: 0
                ) else null
            } ?: emptyList()

            val paymentsRaw = (data["payments"] as? List<*>)?.mapNotNull { row ->
                if (row is Map<*, *>) PaymentEntry(
                    accountId  = row["accountId"]  as? String ?: "default_cash",
                    amount     = (row["amount"]    as? Number)?.toInt() ?: 0,
                    customerId = row["customerId"] as? String ?: ""
                ) else null
            } ?: emptyList()

            val total = (data["total"] as? Number)?.toInt() ?: 0
            val payments = if (paymentsRaw.isEmpty()) {
                listOf(PaymentEntry(
                    accountId = data["accountId"] as? String ?: "default_cash",
                    amount = total
                ))
            } else paymentsRaw

            Sale(
                id = id,
                items = items,
                total = total,
                payments = payments,
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                cashierId = data["cashierId"] as? String ?: "default",
                notes = data["notes"] as? String ?: ""
            )
        } catch (e: Exception) { null }
    }
}
