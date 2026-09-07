package com.akari.retailer.features.inventory.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.akari.retailer.features.inventory.domain.models.Purchase
import com.akari.retailer.features.inventory.domain.models.PurchaseItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestorePurchaseRepository : PurchaseRepository {
    
    private val TAG = "FirestorePurchase"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            val instance = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            instance.firestoreSettings = settings
            instance
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("purchases")
    
    override suspend fun createPurchase(purchase: Purchase): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = collection.document()
            val purchaseWithId = purchase.copy(id = docRef.id)
            docRef.set(purchaseToMap(purchaseWithId)).await()
            Log.d(TAG, "✅ Purchase created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Create purchase error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun getPurchases(): Flow<List<Purchase>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("purchaseDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val purchases = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    mapToPurchase(doc.id, data)
                }
                trySend(purchases)
            }
        
        awaitClose { listener.remove() }
    }
    
    override fun getPurchaseById(purchaseId: String): Flow<Purchase?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(purchaseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                trySend(mapToPurchase(snapshot.id, snapshot.data ?: emptyMap()))
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun deletePurchase(purchaseId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(purchaseId).delete().await()
            Log.d(TAG, "✅ Purchase deleted: $purchaseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete purchase error: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun purchaseToMap(purchase: Purchase): Map<String, Any> {
        return mapOf(
            "orderId" to purchase.orderId,
            "orderName" to purchase.orderName,
            "orderNumber" to purchase.orderNumber,
            "supplierId" to purchase.supplierId,
            "supplierName" to purchase.supplierName,
            "items" to purchase.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "quantity" to item.quantity,
                    "costPrice" to item.costPrice,
                    "total" to item.total
                )
            },
            "totalCost" to purchase.totalCost,
            "purchaseDate" to purchase.purchaseDate,
            "notes" to purchase.notes,
            "receiptNumber" to purchase.receiptNumber,
            "createdAt" to purchase.createdAt
        )
    }
    
    private fun mapToPurchase(id: String, data: Map<String, Any>): Purchase {
        val items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
            if (itemData is Map<*, *>) {
                PurchaseItem(
                    productId = itemData["productId"] as? String ?: "",
                    productName = itemData["productName"] as? String ?: "",
                    quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                    costPrice = (itemData["costPrice"] as? Number)?.toInt() ?: 0,
                    total = (itemData["total"] as? Number)?.toInt() ?: 0
                )
            } else null
        } ?: emptyList()
        
        return Purchase(
            id = id,
            orderId = data["orderId"] as? String ?: "",
            orderName = data["orderName"] as? String ?: "",
            orderNumber = data["orderNumber"] as? String ?: "",
            supplierId = data["supplierId"] as? String ?: "",
            supplierName = data["supplierName"] as? String ?: "",
            items = items,
            totalCost = (data["totalCost"] as? Number)?.toInt() ?: 0,
            purchaseDate = (data["purchaseDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            notes = data["notes"] as? String ?: "",
            receiptNumber = data["receiptNumber"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
