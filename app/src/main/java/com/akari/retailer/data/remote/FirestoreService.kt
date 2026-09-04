package com.akari.retailer.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.core.utils.FirestoreMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirestoreService {
    
    private val TAG = "FirestoreService"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            val instance = FirebaseFirestore.getInstance()
            Log.d(TAG, "✅ Firestore instance obtained")
            instance
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("sales")
    
    suspend fun saveSale(sale: Sale): Result<String> {
        return try {
            Log.d(TAG, "Saving sale...")
            val collection = getCollection()
            if (collection == null) {
                Log.e(TAG, "❌ Firestore not available")
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = if (sale.id.isNotEmpty()) {
                collection.document(sale.id)
            } else {
                collection.document()
            }
            
            val data = FirestoreMapper.toMap(sale)
            
            docRef.set(data).await()
            Log.d(TAG, "✅ Sale saved successfully with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Save error: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Real-time listener for all sales
    fun getSales(): Flow<List<Sale>> = callbackFlow {
        Log.d(TAG, "📡 Starting getSales listener...")
        val collection = getCollection()
        if (collection == null) {
            Log.e(TAG, "❌ Firestore not available")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listen error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.d(TAG, "📡 Snapshot is null, returning empty list")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val sales = snapshot.documents.mapNotNull { doc ->
                    FirestoreMapper.documentToSale(doc)
                }
                
                Log.d(TAG, "📡 Got ${sales.size} sales from Firestore")
                trySend(sales)
            }
        
        awaitClose { 
            Log.d(TAG, "📡 Stopped listening to sales")
            listener.remove() 
        }
    }
    
    fun getTodaySales(): Flow<List<Sale>> = callbackFlow {
        Log.d(TAG, "📡 Starting getTodaySales listener...")
        val collection = getCollection()
        if (collection == null) {
            Log.e(TAG, "❌ Firestore not available")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val todayStart = getTodayStart()
        
        val listener = collection
            .whereGreaterThanOrEqualTo("timestamp", todayStart)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Today sales listen error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val sales = snapshot.documents.mapNotNull { doc ->
                    FirestoreMapper.documentToSale(doc)
                }
                
                Log.d(TAG, "📡 Got ${sales.size} today's sales from Firestore")
                trySend(sales)
            }
        
        awaitClose { 
            Log.d(TAG, "📡 Stopped listening to today's sales")
            listener.remove() 
        }
    }
    
    suspend fun deleteSale(saleId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting sale: $saleId")
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(saleId).delete().await()
            Log.d(TAG, "✅ Sale deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete error: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun getTodayStart(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
