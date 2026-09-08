package com.akari.retailer.features.expense.data.repository

import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ExpenseCategory operations
 */
interface CategoryRepository {
    
    /**
     * Add a new category
     * @param category The category to add
     * @return Result with the new category ID
     */
    suspend fun addCategory(category: ExpenseCategory): Result<String>
    
    /**
     * Update an existing category
     * @param category The category with updated fields
     * @return Result indicating success/failure
     */
    suspend fun updateCategory(category: ExpenseCategory): Result<Unit>
    
    /**
     * Delete a category by ID
     * @param categoryId The ID of the category to delete
     * @return Result indicating success/failure
     */
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    
    /**
     * Get all categories (both default and custom)
     * Real-time updates via Flow
     */
    fun getCategories(): Flow<List<ExpenseCategory>>
    
    /**
     * Get a specific category by ID
     * Real-time updates via Flow
     */
    fun getCategoryById(categoryId: String): Flow<ExpenseCategory?>
    
    /**
     * Get only default/system categories
     */
    fun getDefaultCategories(): Flow<List<ExpenseCategory>>
    
    /**
     * Get only user-created custom categories
     */
    fun getCustomCategories(): Flow<List<ExpenseCategory>>
    
    /**
     * Check if a category name already exists
     */
    suspend fun isCategoryNameExists(name: String, excludeId: String? = null): Boolean
    
    /**
     * Seed default categories into Firestore
     * Only runs if no categories exist
     */
    suspend fun seedDefaultCategories(): Result<Unit>
}
