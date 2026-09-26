package com.akari.retailer.features.expense.data.repository

import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun addCategory(category: ExpenseCategory): Result<String>
    suspend fun updateCategory(category: ExpenseCategory): Result<Unit>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    fun getCategories(): Flow<List<ExpenseCategory>>
    fun getCategoryById(categoryId: String): Flow<ExpenseCategory?>
    suspend fun isCategoryNameExists(name: String, excludeId: String? = null): Boolean

    /** Seed one starter category if the collection is empty. */
    suspend fun seedIfEmpty(name: String, icon: String, color: String): Result<Unit>
}
