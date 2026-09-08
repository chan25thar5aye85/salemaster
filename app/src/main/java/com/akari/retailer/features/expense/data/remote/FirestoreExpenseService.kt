package com.akari.retailer.features.expense.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.expense.domain.models.ExpenseType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreExpenseService {
    
    private val TAG = "FirestoreExpense"
    private val db: FirebaseFirestore?
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection("expenses")
    
    suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = if (expense.id.isNotEmpty()) {
                collection.document(expense.id)
            } else {
                collection.document()
            }
            
            val data = mapOf(
                "title" to expense.title,
                "amount" to expense.amount,
                "categoryId" to expense.categoryId,
                "type" to expense.type.name,
                "businessPercentage" to expense.businessPercentage,
                "description" to expense.description,
                "date" to expense.date,
                "createdAt" to expense.createdAt,
                "updatedAt" to expense.updatedAt
            )
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Add expense error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || expense.id.isEmpty()) {
                return Result.failure(Exception("Invalid expense ID or Firestore not available"))
            }
            
            val data = mapOf(
                "title" to expense.title,
                "amount" to expense.amount,
                "categoryId" to expense.categoryId,
                "type" to expense.type.name,
                "businessPercentage" to expense.businessPercentage,
                "description" to expense.description,
                "date" to expense.date,
                "updatedAt" to System.currentTimeMillis()
            )
            
            collection.document(expense.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update expense error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(expenseId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete expense error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getExpenses(): Flow<List<Expense>> = callbackFlow {
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
                
                val expenses = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Expense(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toInt() ?: 0,
                        categoryId = data["categoryId"] as? String ?: "default_other",
                        type = try {
                            ExpenseType.valueOf(data["type"] as? String ?: "BUSINESS")
                        } catch (e: Exception) {
                            ExpenseType.BUSINESS
                        },
                        businessPercentage = (data["businessPercentage"] as? Number)?.toInt() ?: 100,
                        description = data["description"] as? String ?: "",
                        date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(expenses)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getExpenseById(expenseId: String): Flow<Expense?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(expenseId)
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
                
                val expense = Expense(
                    id = snapshot.id,
                    title = data["title"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toInt() ?: 0,
                    categoryId = data["categoryId"] as? String ?: "default_other",
                    type = try {
                        ExpenseType.valueOf(data["type"] as? String ?: "BUSINESS")
                    } catch (e: Exception) {
                        ExpenseType.BUSINESS
                    },
                    businessPercentage = (data["businessPercentage"] as? Number)?.toInt() ?: 100,
                    description = data["description"] as? String ?: "",
                    date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                trySend(expense)
            }
        
        awaitClose { listener.remove() }
    }
}
