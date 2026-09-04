package com.akari.retailer.features.customer.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.customer.domain.models.Customer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCustomerService {
    
    private val TAG = "FirestoreCustomer"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("customers")
    
    suspend fun addCustomer(customer: Customer): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = if (customer.id.isNotEmpty()) {
                collection.document(customer.id)
            } else {
                collection.document()
            }
            
            val data = mapOf(
                "name" to customer.name,
                "phone" to customer.phone,
                "email" to customer.email,
                "address" to customer.address,
                "totalSpent" to customer.totalSpent,
                "totalOrders" to customer.totalOrders,
                "lastOrderDate" to customer.lastOrderDate,
                "createdAt" to customer.createdAt,
                "updatedAt" to customer.updatedAt,
                "notes" to customer.notes
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Add customer error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateCustomer(customer: Customer): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || customer.id.isEmpty()) {
                return Result.failure(Exception("Invalid customer ID or Firestore not available"))
            }
            
            val data = mapOf(
                "name" to customer.name,
                "phone" to customer.phone,
                "email" to customer.email,
                "address" to customer.address,
                "totalSpent" to customer.totalSpent,
                "totalOrders" to customer.totalOrders,
                "lastOrderDate" to customer.lastOrderDate,
                "updatedAt" to System.currentTimeMillis(),
                "notes" to customer.notes
            )
            
            collection.document(customer.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update customer error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(customerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete customer error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getCustomers(): Flow<List<Customer>> = callbackFlow {
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
                
                val customers = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Customer(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        phone = data["phone"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        address = data["address"] as? String ?: "",
                        totalSpent = (data["totalSpent"] as? Number)?.toInt() ?: 0,
                        totalOrders = (data["totalOrders"] as? Number)?.toInt() ?: 0,
                        lastOrderDate = (data["lastOrderDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        notes = data["notes"] as? String ?: ""
                    )
                }
                trySend(customers)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getCustomerById(customerId: String): Flow<Customer?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(customerId)
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
                
                val customer = Customer(
                    id = snapshot.id,
                    name = data["name"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    totalSpent = (data["totalSpent"] as? Number)?.toInt() ?: 0,
                    totalOrders = (data["totalOrders"] as? Number)?.toInt() ?: 0,
                    lastOrderDate = (data["lastOrderDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    notes = data["notes"] as? String ?: ""
                )
                trySend(customer)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun searchCustomers(query: String): Result<List<Customer>> {
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
            
            val customers = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Customer(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    address = data["address"] as? String ?: "",
                    totalSpent = (data["totalSpent"] as? Number)?.toInt() ?: 0,
                    totalOrders = (data["totalOrders"] as? Number)?.toInt() ?: 0,
                    lastOrderDate = (data["lastOrderDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    notes = data["notes"] as? String ?: ""
                )
            }
            Result.success(customers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
