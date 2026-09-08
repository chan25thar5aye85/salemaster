package com.akari.retailer.features.expense.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.expense.domain.models.DefaultCategories
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCategoryService {
    
    private val TAG = "FirestoreCategory"
    private val db: FirebaseFirestore?
    private val COLLECTION = "expense_categories"
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection(COLLECTION)
    
    /**
     * Add a new category
     */
    suspend fun addCategory(category: ExpenseCategory): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = if (category.id.isNotEmpty()) {
                collection.document(category.id)
            } else {
                collection.document()
            }
            
            val data = mapOf(
                "name" to category.name,
                "icon" to category.icon,
                "color" to category.color,
                "isDefault" to category.isDefault,
                "createdAt" to category.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            
            docRef.set(data).await()
            Log.d(TAG, "✅ Category added: ${docRef.id} - ${category.name}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Add category error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing category
     */
    suspend fun updateCategory(category: ExpenseCategory): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || category.id.isEmpty()) {
                return Result.failure(Exception("Invalid category or Firestore not available"))
            }
            
            // Don't allow renaming default categories
            val existing = getCategoryByIdSync(category.id)
            if (existing?.isDefault == true && category.name != existing.name) {
                return Result.failure(Exception("Cannot rename default category"))
            }
            
            val data = mapOf(
                "name" to category.name,
                "icon" to category.icon,
                "color" to category.color,
                "updatedAt" to System.currentTimeMillis()
            )
            
            collection.document(category.id).update(data).await()
            Log.d(TAG, "✅ Category updated: ${category.id} - ${category.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update category error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Delete a category by ID
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            // Check if it's a default category
            val category = getCategoryByIdSync(categoryId)
            if (category?.isDefault == true) {
                return Result.failure(Exception("Cannot delete default category"))
            }
            
            collection.document(categoryId).delete().await()
            Log.d(TAG, "✅ Category deleted: $categoryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete category error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get all categories with real-time updates
     */
    fun getCategories(): Flow<List<ExpenseCategory>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            Log.e(TAG, "❌ Firestore not available")
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Listen error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val categories = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ExpenseCategory(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        icon = data["icon"] as? String ?: "📌",
                        color = data["color"] as? String ?: "#636E72",
                        isDefault = data["isDefault"] as? Boolean ?: false,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                
                Log.d(TAG, "📡 Got ${categories.size} categories")
                trySend(categories)
            }
        
        awaitClose { 
            Log.d(TAG, "📡 Stopped listening to categories")
            listener.remove() 
        }
    }
    
    /**
     * Get a specific category by ID with real-time updates
     */
    fun getCategoryById(categoryId: String): Flow<ExpenseCategory?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(categoryId)
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
                
                val category = ExpenseCategory(
                    id = snapshot.id,
                    name = data["name"] as? String ?: "",
                    icon = data["icon"] as? String ?: "📌",
                    color = data["color"] as? String ?: "#636E72",
                    isDefault = data["isDefault"] as? Boolean ?: false,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                trySend(category)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get a category synchronously (one-time fetch)
     */
    suspend fun getCategoryByIdSync(categoryId: String): ExpenseCategory? {
        return try {
            val collection = getCollection() ?: return null
            val snapshot = collection.document(categoryId).get().await()
            
            if (!snapshot.exists()) return null
            
            val data = snapshot.data ?: return null
            ExpenseCategory(
                id = snapshot.id,
                name = data["name"] as? String ?: "",
                icon = data["icon"] as? String ?: "📌",
                color = data["color"] as? String ?: "#636E72",
                isDefault = data["isDefault"] as? Boolean ?: false,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get category sync error: ${e.message}")
            null
        }
    }
    
    /**
     * Check if a category name exists
     */
    suspend fun isCategoryNameExists(name: String, excludeId: String? = null): Boolean {
        return try {
            val collection = getCollection() ?: return false
            val snapshot = collection
                .whereEqualTo("name", name)
                .get()
                .await()
            
            snapshot.documents.any { doc ->
                doc.id != excludeId
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Check name exists error: ${e.message}")
            false
        }
    }
    
    /**
     * Seed default categories into Firestore
     */
    suspend fun seedDefaultCategories(): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            // Check if any categories exist
            val existing = collection.limit(1).get().await()
            if (!existing.isEmpty) {
                Log.d(TAG, "Categories already exist, skipping seed")
                return Result.success(Unit)
            }
            
            // Add all default categories
            DefaultCategories.list.forEach { category ->
                val docRef = collection.document(category.id)
                val data = mapOf(
                    "name" to category.name,
                    "icon" to category.icon,
                    "color" to category.color,
                    "isDefault" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data).await()
                Log.d(TAG, "✅ Seeded category: ${category.name}")
            }
            
            Log.d(TAG, "✅ All default categories seeded")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Seed categories error: ${e.message}")
            Result.failure(e)
        }
    }
}
