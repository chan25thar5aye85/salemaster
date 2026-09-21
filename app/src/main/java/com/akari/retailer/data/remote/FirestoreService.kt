package com.akari.retailer.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
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
            Log.e(TAG, "❌ Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("sales")
    
    suspend fun saveSale(sale: Sale): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
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
                "accountId" to sale.accountId,
                "timestamp" to sale.timestamp,
                "cashierId" to sale.cashierId
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Save error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getSales(): Flow<List<Sale>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val sales = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                        if (itemData is Map<*, *>) {
                            SaleItem(
                                productId = itemData["productId"] as? String ?: "",
                                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                                price = (itemData["price"] as? Number)?.toInt() ?: 0,
                                total = (itemData["total"] as? Number)?.toInt() ?: 0
                            )
                        } else null
                    } ?: emptyList()
                    
                    Sale(
                        id = doc.id,
                        items = items,
                        total = (data["total"] as? Number)?.toInt() ?: 0,
                        accountId = data["accountId"] as? String ?: "default_cash",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        cashierId = data["cashierId"] as? String ?: "default"
                    )
                }
                trySend(sales)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getTodaySales(): Flow<List<Sale>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
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
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val sales = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
                        if (itemData is Map<*, *>) {
                            SaleItem(
                                productId = itemData["productId"] as? String ?: "",
                                quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                                price = (itemData["price"] as? Number)?.toInt() ?: 0,
                                total = (itemData["total"] as? Number)?.toInt() ?: 0
                            )
                        } else null
                    } ?: emptyList()
                    
                    Sale(
                        id = doc.id,
                        items = items,
                        total = (data["total"] as? Number)?.toInt() ?: 0,
                        accountId = data["accountId"] as? String ?: "default_cash",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        cashierId = data["cashierId"] as? String ?: "default"
                    )
                }
                trySend(sales)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun deleteSale(saleId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(saleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete error: ${e.message}")
            Result.failure(e)
        }
    }
}
