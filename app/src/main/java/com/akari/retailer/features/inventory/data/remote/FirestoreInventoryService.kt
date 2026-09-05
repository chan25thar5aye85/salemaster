package com.akari.retailer.features.inventory.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.inventory.domain.models.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreInventoryService {
    
    private val TAG = "FirestoreInventory"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("products")
    
    suspend fun addProduct(product: Product): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = if (product.id.isNotEmpty()) {
                collection.document(product.id)
            } else {
                collection.document()
            }
            
            val data = mapOf(
                "name" to product.name,
                "category" to product.category,
                "sku" to product.sku,
                "costPrice" to product.costPrice,
                "sellPrice" to product.sellPrice,
                "stockQuantity" to product.stockQuantity,
                "minStockLevel" to product.minStockLevel,
                "supplierId" to product.supplierId,
                "createdAt" to product.createdAt,
                "updatedAt" to product.updatedAt
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Add product error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || product.id.isEmpty()) {
                return Result.failure(Exception("Invalid product ID or Firestore not available"))
            }
            
            val data = mapOf(
                "name" to product.name,
                "category" to product.category,
                "sku" to product.sku,
                "costPrice" to product.costPrice,
                "sellPrice" to product.sellPrice,
                "stockQuantity" to product.stockQuantity,
                "minStockLevel" to product.minStockLevel,
                "supplierId" to product.supplierId,
                "updatedAt" to System.currentTimeMillis()
            )
            
            collection.document(product.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update product error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete product error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getProducts(): Flow<List<Product>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val products = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Product(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        sku = data["sku"] as? String ?: "",
                        costPrice = (data["costPrice"] as? Number)?.toInt() ?: 0,
                        sellPrice = (data["sellPrice"] as? Number)?.toInt() ?: 0,
                        stockQuantity = (data["stockQuantity"] as? Number)?.toInt() ?: 0,
                        minStockLevel = (data["minStockLevel"] as? Number)?.toInt() ?: 0,
                        supplierId = data["supplierId"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(products)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getProductById(productId: String): Flow<Product?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val data = snapshot.data ?: run {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val product = Product(
                    id = snapshot.id,
                    name = data["name"] as? String ?: "",
                    category = data["category"] as? String ?: "",
                    sku = data["sku"] as? String ?: "",
                    costPrice = (data["costPrice"] as? Number)?.toInt() ?: 0,
                    sellPrice = (data["sellPrice"] as? Number)?.toInt() ?: 0,
                    stockQuantity = (data["stockQuantity"] as? Number)?.toInt() ?: 0,
                    minStockLevel = (data["minStockLevel"] as? Number)?.toInt() ?: 0,
                    supplierId = data["supplierId"] as? String ?: "",
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                trySend(product)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getProductByIdSync(productId: String): Result<Product> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val document = collection.document(productId)
            val snapshot = document.get().await()
            
            if (!snapshot.exists()) {
                return Result.failure(Exception("Product not found"))
            }
            
            val data = snapshot.data ?: return Result.failure(Exception("Product data is null"))
            
            val product = Product(
                id = snapshot.id,
                name = data["name"] as? String ?: "",
                category = data["category"] as? String ?: "",
                sku = data["sku"] as? String ?: "",
                costPrice = (data["costPrice"] as? Number)?.toInt() ?: 0,
                sellPrice = (data["sellPrice"] as? Number)?.toInt() ?: 0,
                stockQuantity = (data["stockQuantity"] as? Number)?.toInt() ?: 0,
                minStockLevel = (data["minStockLevel"] as? Number)?.toInt() ?: 0,
                supplierId = data["supplierId"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
