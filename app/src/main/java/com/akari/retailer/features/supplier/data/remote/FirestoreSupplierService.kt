package com.akari.retailer.features.supplier.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.supplier.domain.models.Supplier
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSupplierService {
    
    private val TAG = "FirestoreSupplier"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("suppliers")
    
    suspend fun addSupplier(supplier: Supplier): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = if (supplier.id.isNotEmpty()) {
                collection.document(supplier.id)
            } else {
                collection.document()
            }
            
            val data = mapOf(
                "name" to supplier.name,
                "company" to supplier.company,
                "phone" to supplier.phone,
                "email" to supplier.email,
                "address" to supplier.address,
                "products" to supplier.products,
                "totalPurchased" to supplier.totalPurchased,
                "lastOrderDate" to supplier.lastOrderDate,
                "createdAt" to supplier.createdAt,
                "updatedAt" to supplier.updatedAt,
                "notes" to supplier.notes
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Add supplier error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateSupplier(supplier: Supplier): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || supplier.id.isEmpty()) {
                return Result.failure(Exception("Invalid supplier ID or Firestore not available"))
            }
            
            val data = mapOf(
                "name" to supplier.name,
                "company" to supplier.company,
                "phone" to supplier.phone,
                "email" to supplier.email,
                "address" to supplier.address,
                "products" to supplier.products,
                "totalPurchased" to supplier.totalPurchased,
                "lastOrderDate" to supplier.lastOrderDate,
                "updatedAt" to System.currentTimeMillis(),
                "notes" to supplier.notes
            )
            
            collection.document(supplier.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update supplier error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteSupplier(supplierId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(supplierId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete supplier error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getSuppliers(): Flow<List<Supplier>> = callbackFlow {
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
                
                val suppliers = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Supplier(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        company = data["company"] as? String ?: "",
                        phone = data["phone"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        address = data["address"] as? String ?: "",
                        products = (data["products"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        totalPurchased = (data["totalPurchased"] as? Number)?.toInt() ?: 0,
                        lastOrderDate = (data["lastOrderDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        notes = data["notes"] as? String ?: ""
                    )
                }
                trySend(suppliers)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getSupplierById(supplierId: String): Flow<Supplier?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(supplierId)
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
                
                val supplier = Supplier(
                    id = snapshot.id,
                    name = data["name"] as? String ?: "",
                    company = data["company"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    products = (data["products"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    totalPurchased = (data["totalPurchased"] as? Number)?.toInt() ?: 0,
                    lastOrderDate = (data["lastOrderDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    notes = data["notes"] as? String ?: ""
                )
                trySend(supplier)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun searchSuppliers(query: String): Result<List<Supplier>> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val snapshot = collection
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
            
            val suppliers = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Supplier(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    company = data["company"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    products = (data["products"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    totalPurchased = (data["totalPurchased"] as? Number)?.toInt() ?: 0,
                    lastOrderDate = (data["lastOrderDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    notes = data["notes"] as? String ?: ""
                )
            }
            Result.success(suppliers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
