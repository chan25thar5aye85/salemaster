package com.akari.retailer.features.sales.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreIncomeEntryService {
    
    private val TAG = "FirestoreIncomeEntry"
    private val db: FirebaseFirestore?
    private val COLLECTION = "income_entries"
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection(COLLECTION)
    
    suspend fun addIncomeEntry(entry: IncomeEntry): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = if (entry.id.isNotEmpty()) {
                collection.document(entry.id)
            } else {
                collection.document()
            }
            
            val data = mapOf(
                "amount" to entry.amount,
                "incomeStreamId" to entry.incomeStreamId,
                "description" to entry.description,
                "type" to entry.type.name,
                "date" to entry.date,
                "createdAt" to entry.createdAt,
                "updatedAt" to entry.updatedAt
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Add income entry error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateIncomeEntry(entry: IncomeEntry): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || entry.id.isEmpty()) {
                return Result.failure(Exception("Invalid entry ID or Firestore not available"))
            }
            
            val data = mapOf(
                "amount" to entry.amount,
                "incomeStreamId" to entry.incomeStreamId,
                "description" to entry.description,
                "type" to entry.type.name,
                "date" to entry.date,
                "updatedAt" to System.currentTimeMillis()
            )
            
            collection.document(entry.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update income entry error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteIncomeEntry(entryId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete income entry error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getIncomeEntries(): Flow<List<IncomeEntry>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val entries = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    IncomeEntry(
                        id = doc.id,
                        amount = (data["amount"] as? Number)?.toInt() ?: 0,
                        incomeStreamId = data["incomeStreamId"] as? String ?: "default_product_sales",
                        description = data["description"] as? String ?: "",
                        type = try {
                            IncomeEntryType.valueOf(data["type"] as? String ?: "BUSINESS")
                        } catch (e: Exception) {
                            IncomeEntryType.BUSINESS
                        },
                        date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(entries)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getIncomeEntryById(entryId: String): Flow<IncomeEntry?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(entryId)
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
                
                val entry = IncomeEntry(
                    id = snapshot.id,
                    amount = (data["amount"] as? Number)?.toInt() ?: 0,
                    incomeStreamId = data["incomeStreamId"] as? String ?: "default_product_sales",
                    description = data["description"] as? String ?: "",
                    type = try {
                        IncomeEntryType.valueOf(data["type"] as? String ?: "BUSINESS")
                    } catch (e: Exception) {
                        IncomeEntryType.BUSINESS
                    },
                    date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                trySend(entry)
            }
        
        awaitClose { listener.remove() }
    }
}
