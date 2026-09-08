package com.akari.retailer.features.sales.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.sales.domain.models.DefaultIncomeStreams
import com.akari.retailer.features.sales.domain.models.IncomeStream
import com.akari.retailer.features.sales.domain.models.IncomeType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreIncomeStreamService {
    
    private val TAG = "FirestoreIncomeStream"
    private val db: FirebaseFirestore?
    private val COLLECTION = "income_streams"
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection(COLLECTION)
    
    suspend fun addIncomeStream(stream: IncomeStream): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = collection.document()
            val data = mapOf(
                "name" to stream.name,
                "type" to stream.type.name,
                "icon" to stream.icon,
                "color" to stream.color,
                "isDefault" to stream.isDefault,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Add income stream error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateIncomeStream(stream: IncomeStream): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || stream.id.isEmpty()) {
                return Result.failure(Exception("Invalid stream or Firestore not available"))
            }
            
            val existing = getIncomeStreamByIdSync(stream.id)
            if (existing?.isDefault == true && stream.name != existing.name) {
                return Result.failure(Exception("Cannot rename default income stream"))
            }
            
            val data = mapOf(
                "name" to stream.name,
                "icon" to stream.icon,
                "color" to stream.color,
                "updatedAt" to System.currentTimeMillis()
            )
            collection.document(stream.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update income stream error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteIncomeStream(streamId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val stream = getIncomeStreamByIdSync(streamId)
            if (stream?.isDefault == true) {
                return Result.failure(Exception("Cannot delete default income stream"))
            }
            
            collection.document(streamId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete income stream error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getIncomeStreams(): Flow<List<IncomeStream>> = callbackFlow {
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
                
                val streams = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    IncomeStream(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        type = try {
                            IncomeType.valueOf(data["type"] as? String ?: "PRODUCT_SALES")
                        } catch (e: Exception) {
                            IncomeType.PRODUCT_SALES
                        },
                        icon = data["icon"] as? String ?: "💰",
                        color = data["color"] as? String ?: "#4CAF50",
                        isDefault = data["isDefault"] as? Boolean ?: false,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(streams)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getIncomeStreamById(streamId: String): Flow<IncomeStream?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(streamId)
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
                
                val stream = IncomeStream(
                    id = snapshot.id,
                    name = data["name"] as? String ?: "",
                    type = try {
                        IncomeType.valueOf(data["type"] as? String ?: "PRODUCT_SALES")
                    } catch (e: Exception) {
                        IncomeType.PRODUCT_SALES
                    },
                    icon = data["icon"] as? String ?: "💰",
                    color = data["color"] as? String ?: "#4CAF50",
                    isDefault = data["isDefault"] as? Boolean ?: false,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                trySend(stream)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getIncomeStreamByIdSync(streamId: String): IncomeStream? {
        return try {
            val collection = getCollection() ?: return null
            val snapshot = collection.document(streamId).get().await()
            if (!snapshot.exists()) return null
            val data = snapshot.data ?: return null
            IncomeStream(
                id = snapshot.id,
                name = data["name"] as? String ?: "",
                type = try {
                    IncomeType.valueOf(data["type"] as? String ?: "PRODUCT_SALES")
                } catch (e: Exception) {
                    IncomeType.PRODUCT_SALES
                },
                icon = data["icon"] as? String ?: "💰",
                color = data["color"] as? String ?: "#4CAF50",
                isDefault = data["isDefault"] as? Boolean ?: false,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Get income stream sync error: ${e.message}")
            null
        }
    }
    
    suspend fun seedDefaultIncomeStreams(): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val existing = collection.limit(1).get().await()
            if (!existing.isEmpty) {
                Log.d(TAG, "Income streams already exist, skipping seed")
                return Result.success(Unit)
            }
            
            DefaultIncomeStreams.list.forEach { stream ->
                val docRef = collection.document(stream.id)
                val data = mapOf(
                    "name" to stream.name,
                    "type" to stream.type.name,
                    "icon" to stream.icon,
                    "color" to stream.color,
                    "isDefault" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data).await()
                Log.d(TAG, "✅ Seeded income stream: ${stream.name}")
            }
            
            Log.d(TAG, "✅ All default income streams seeded")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Seed income streams error: ${e.message}")
            Result.failure(e)
        }
    }
}
