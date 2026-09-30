package com.akari.retailer.features.inventory.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.inventory.domain.models.MovementType
import com.akari.retailer.features.inventory.domain.models.StockMovement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreStockService {
    
    private val TAG = "FirestoreStock"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("stock_movements")
    
    suspend fun addMovement(movement: StockMovement): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = collection.document()
            
            val data = mapOf(
                "productId" to movement.productId,
                "type" to movement.type.name,
                "quantity" to movement.quantity,
                "previousStock" to movement.previousStock,
                "newStock" to movement.newStock,
                "reason" to movement.reason,
                "saleId" to movement.saleId,
                "purchaseOrderId" to movement.purchaseOrderId,
                "createdAt" to movement.createdAt,
                "userId" to movement.userId
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Add movement error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteMovement(movementId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(movementId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete movement error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getMovementsForProduct(productId: String): Flow<List<StockMovement>> = callbackFlow {
        try {
            val collection = getCollection()
            if (collection == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            val listener = collection
                .whereEqualTo("productId", productId)
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    if (querySnapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val movements = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        StockMovement(
                            id = doc.id,
                            productId = data["productId"] as? String ?: "",
                            type = try {
                                MovementType.valueOf(data["type"] as? String ?: "ADJUSTMENT")
                            } catch (e: Exception) {
                                MovementType.ADJUSTMENT
                            },
                            quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                            previousStock = (data["previousStock"] as? Number)?.toInt() ?: 0,
                            newStock = (data["newStock"] as? Number)?.toInt() ?: 0,
                            reason = data["reason"] as? String ?: "",
                            saleId = data["saleId"] as? String ?: "",
                            purchaseOrderId = data["purchaseOrderId"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            userId = data["userId"] as? String ?: ""
                        )
                    }
                    trySend(movements.sortedByDescending { it.createdAt })
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getMovementsForProduct: ${e.message}")
            trySend(emptyList())
            close()
        }
    }
    
    fun getAllMovements(): Flow<List<StockMovement>> = callbackFlow {
        try {
            val collection = getCollection()
            if (collection == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            
            val listener = collection
                .addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    if (querySnapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val movements = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        StockMovement(
                            id = doc.id,
                            productId = data["productId"] as? String ?: "",
                            type = try {
                                MovementType.valueOf(data["type"] as? String ?: "ADJUSTMENT")
                            } catch (e: Exception) {
                                MovementType.ADJUSTMENT
                            },
                            quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                            previousStock = (data["previousStock"] as? Number)?.toInt() ?: 0,
                            newStock = (data["newStock"] as? Number)?.toInt() ?: 0,
                            reason = data["reason"] as? String ?: "",
                            saleId = data["saleId"] as? String ?: "",
                            purchaseOrderId = data["purchaseOrderId"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            userId = data["userId"] as? String ?: ""
                        )
                    }
                    trySend(movements.sortedByDescending { it.createdAt })
                }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAllMovements: ${e.message}")
            trySend(emptyList())
            close()
        }
    }
    
    suspend fun getMovementsForProductSync(productId: String): Result<List<StockMovement>> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.success(emptyList())
            }
            
            val snapshot = collection
                .whereEqualTo("productId", productId)
                .get()
                .await()
            
            val movements = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                StockMovement(
                    id = doc.id,
                    productId = data["productId"] as? String ?: "",
                    type = try {
                        MovementType.valueOf(data["type"] as? String ?: "ADJUSTMENT")
                    } catch (e: Exception) {
                        MovementType.ADJUSTMENT
                    },
                    quantity = (data["quantity"] as? Number)?.toInt() ?: 0,
                    previousStock = (data["previousStock"] as? Number)?.toInt() ?: 0,
                    newStock = (data["newStock"] as? Number)?.toInt() ?: 0,
                    reason = data["reason"] as? String ?: "",
                    saleId = data["saleId"] as? String ?: "",
                    purchaseOrderId = data["purchaseOrderId"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    userId = data["userId"] as? String ?: ""
                )
            }
            Result.success(movements.sortedByDescending { it.createdAt })
        } catch (e: Exception) {
            Log.e(TAG, "Error in getMovementsForProductSync: ${e.message}")
            Result.success(emptyList())
        }
    }
}
