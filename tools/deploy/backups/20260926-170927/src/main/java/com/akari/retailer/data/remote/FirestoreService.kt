package com.akari.retailer.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.features.sales.domain.models.SaleItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService {
    
    private val TAG = "FirestoreService"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("sales")
    
    suspend fun saveSale(sale: Sale): Result<String> {
        return try {
            val collection = getCollection() ?: return Result.failure(Exception("Firestore not available"))
            
            val docRef = if (sale.id.isNotEmpty()) {
                collection.document(sale.id)
            } else {
                collection.document()
            }
            
            val data = mapOf(
                "items" to sale.items.map { item ->
                    mapOf(
                        "productId" to item.productId,
                        "quantity" to item.quantity,
                        "price" to item.price,
                        "total" to item.total
                    )
                },
                "total" to sale.total,
                "payments" to sale.payments.map { p ->
                    mapOf("accountId" to p.accountId, "amount" to p.amount)
                },
                "timestamp" to sale.timestamp,
                "cashierId" to sale.cashierId
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Save error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getSales(): Flow<List<Sale>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) { trySend(emptyList()); close(); return@callbackFlow }
        
        val listener = collection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                
                val sales = snapshot.documents.mapNotNull { doc -> mapDocToSale(doc.id, doc.data ?: emptyMap()) }
                trySend(sales)
            }
        awaitClose { listener.remove() }
    }
    
    fun getTodaySales(): Flow<List<Sale>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) { trySend(emptyList()); close(); return@callbackFlow }
        
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis
        
        val listener = collection
            .whereGreaterThanOrEqualTo("timestamp", todayStart)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                
                val sales = snapshot.documents.mapNotNull { doc -> mapDocToSale(doc.id, doc.data ?: emptyMap()) }
                trySend(sales)
            }
        awaitClose { listener.remove() }
    }
    
    fun getSaleById(saleId: String): Flow<Sale?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) { trySend(null); close(); return@callbackFlow }
        
        val listener = collection.document(saleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(mapDocToSale(snapshot.id, snapshot.data ?: emptyMap()))
            }
        awaitClose { listener.remove() }
    }

    private fun mapDocToSale(id: String, data: Map<String, Any>): Sale? {
        return try {
            val items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                if (itemData is Map<*, *>) SaleItem(
                    productId = itemData["productId"] as? String ?: "",
                    quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                    price = (itemData["price"] as? Number)?.toInt() ?: 0,
                    total = (itemData["total"] as? Number)?.toInt() ?: 0
                ) else null
            } ?: emptyList()
            
            // Read payments (with backward compat)
            val paymentsList = (data["payments"] as? List<*>)?.mapNotNull { p ->
                if (p is Map<*, *>) PaymentEntry(
                    accountId = p["accountId"] as? String ?: "default_cash",
                    amount = (p["amount"] as? Number)?.toInt() ?: 0
                ) else null
            } ?: emptyList()
            
            val finalPayments = if (paymentsList.isEmpty()) {
                listOf(PaymentEntry(
                    accountId = data["accountId"] as? String ?: "default_cash",
                    amount = (data["total"] as? Number)?.toInt() ?: 0
                ))
            } else paymentsList
            
            Sale(
                id = id,
                items = items,
                total = (data["total"] as? Number)?.toInt() ?: 0,
                payments = finalPayments,
                timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                cashierId = data["cashierId"] as? String ?: "default",
                notes = data["notes"] as? String ?: ""
            )
        } catch (e: Exception) { null }
    }
    
    suspend fun deleteSale(saleId: String): Result<Unit> {
        return try {
            val collection = getCollection() ?: return Result.failure(Exception("Firestore not available"))
            collection.document(saleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
