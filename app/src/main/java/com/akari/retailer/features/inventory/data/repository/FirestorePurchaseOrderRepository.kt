package com.akari.retailer.features.inventory.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestorePurchaseOrderRepository : PurchaseOrderRepository {
    
    private val TAG = "FirestorePurchaseOrder"
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
    
    private fun getCollection() = db?.collection("purchase_orders")
    
    override suspend fun createOrder(order: PurchaseOrder): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = collection.document()
            val orderWithId = PurchaseOrder(
                id = docRef.id,
                orderName = order.orderName,
                orderNumber = order.orderNumber,
                supplierId = order.supplierId,
                supplierName = order.supplierName,
                items = order.items,
                status = order.status,
                totalCost = order.totalCost,
                receivedCost = order.receivedCost,
                orderDate = order.orderDate,
                sentDate = order.sentDate,
                receivedDate = order.receivedDate,
                closedDate = order.closedDate,
                notes = order.notes,
                createdBy = order.createdBy,
                createdAt = order.createdAt,
                updatedAt = order.updatedAt
            )
            
            docRef.set(orderToMap(orderWithId)).await()
            Log.d(TAG, "✅ Order created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Create order error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateOrder(order: PurchaseOrder): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || order.id.isEmpty()) {
                return Result.failure(Exception("Invalid order or Firestore not available"))
            }
            
            collection.document(order.id).set(orderToMap(order)).await()
            Log.d(TAG, "✅ Order updated: ${order.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update order error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun getOrder(orderId: String): Flow<PurchaseOrder?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                trySend(mapToOrder(snapshot.id, snapshot.data ?: emptyMap()))
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun getOrderSync(orderId: String): PurchaseOrder? {
        return try {
            val collection = getCollection() ?: return null
            val snapshot = collection.document(orderId).get().await()
            if (!snapshot.exists()) return null
            mapToOrder(snapshot.id, snapshot.data ?: emptyMap())
        } catch (e: Exception) {
            Log.e(TAG, "Get order sync error: ${e.message}")
            null
        }
    }
    
    override fun getOrders(): Flow<List<PurchaseOrder>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    mapToOrder(doc.id, data)
                }
                trySend(orders)
            }
        
        awaitClose { listener.remove() }
    }
    
    override fun getOrdersByStatus(status: PurchaseOrderStatus): Flow<List<PurchaseOrder>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .whereEqualTo("status", status.name)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    mapToOrder(doc.id, data)
                }
                trySend(orders)
            }
        
        awaitClose { listener.remove() }
    }
    
    override fun getOrdersBySupplier(supplierId: String): Flow<List<PurchaseOrder>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .whereEqualTo("supplierId", supplierId)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val orders = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    mapToOrder(doc.id, data)
                }
                trySend(orders)
            }
        
        awaitClose { listener.remove() }
    }
    
    override suspend fun deleteOrder(orderId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(orderId).delete().await()
            Log.d(TAG, "✅ Order deleted: $orderId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete order error: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun updateStatus(orderId: String, newStatus: PurchaseOrderStatus): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val currentTime = System.currentTimeMillis()
            val updates: MutableMap<String, Any> = HashMap()
            updates["status"] = newStatus.name
            updates["updatedAt"] = currentTime
            
            when (newStatus) {
                PurchaseOrderStatus.SENT -> updates["sentDate"] = currentTime
                PurchaseOrderStatus.RECEIVED -> updates["receivedDate"] = currentTime
                PurchaseOrderStatus.CLOSED -> updates["closedDate"] = currentTime
                else -> {}
            }
            
            collection.document(orderId).update(updates).await()
            Log.d(TAG, "✅ Status updated: $orderId -> ${newStatus.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update status error: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun orderToMap(order: PurchaseOrder): Map<String, Any> {
        return mapOf(
            "orderName" to order.orderName,
            "orderNumber" to order.orderNumber,
            "supplierId" to order.supplierId,
            "supplierName" to order.supplierName,
            "items" to order.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "productName" to item.productName,
                    "quantity" to item.quantity,
                    "costPrice" to item.costPrice,
                    "total" to item.total,
                    "receivedQuantity" to item.receivedQuantity
                )
            },
            "status" to order.status.name,
            "totalCost" to order.totalCost,
            "receivedCost" to order.receivedCost,
            "orderDate" to order.orderDate,
            "sentDate" to order.sentDate,
            "receivedDate" to order.receivedDate,
            "closedDate" to order.closedDate,
            "notes" to order.notes,
            "createdBy" to order.createdBy,
            "createdAt" to order.createdAt,
            "updatedAt" to order.updatedAt
        )
    }
    
    private fun mapToOrder(id: String, data: Map<String, Any>): PurchaseOrder {
        val items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
            if (itemData is Map<*, *>) {
                PurchaseOrderItem(
                    productId = itemData["productId"] as? String ?: "",
                    productName = itemData["productName"] as? String ?: "",
                    quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                    costPrice = (itemData["costPrice"] as? Number)?.toInt() ?: 0,
                    total = (itemData["total"] as? Number)?.toInt() ?: 0,
                    receivedQuantity = (itemData["receivedQuantity"] as? Number)?.toInt() ?: 0
                )
            } else null
        } ?: emptyList()
        
        return PurchaseOrder(
            id = id,
            orderName = data["orderName"] as? String ?: "",
            orderNumber = data["orderNumber"] as? String ?: "",
            supplierId = data["supplierId"] as? String ?: "",
            supplierName = data["supplierName"] as? String ?: "",
            items = items,
            status = try {
                PurchaseOrderStatus.valueOf(data["status"] as? String ?: "DRAFT")
            } catch (e: Exception) {
                PurchaseOrderStatus.DRAFT
            },
            totalCost = (data["totalCost"] as? Number)?.toInt() ?: 0,
            receivedCost = (data["receivedCost"] as? Number)?.toInt() ?: 0,
            orderDate = (data["orderDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            sentDate = (data["sentDate"] as? Number)?.toLong() ?: 0,
            receivedDate = (data["receivedDate"] as? Number)?.toLong() ?: 0,
            closedDate = (data["closedDate"] as? Number)?.toLong() ?: 0,
            notes = data["notes"] as? String ?: "",
            createdBy = data["createdBy"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
