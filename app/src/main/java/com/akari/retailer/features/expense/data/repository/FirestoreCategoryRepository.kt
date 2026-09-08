package com.akari.retailer.features.expense.data.repository

import com.akari.retailer.features.expense.data.remote.FirestoreCategoryService
import com.akari.retailer.features.expense.domain.models.ExpenseCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreCategoryRepository(
    private val service: FirestoreCategoryService
) : CategoryRepository {
    
    override suspend fun addCategory(category: ExpenseCategory): Result<String> {
        return service.addCategory(category)
    }
    
    override suspend fun updateCategory(category: ExpenseCategory): Result<Unit> {
        return service.updateCategory(category)
    }
    
    override suspend fun deleteCategory(categoryId: String): Result<Unit> {
        return service.deleteCategory(categoryId)
    }
    
    override fun getCategories(): Flow<List<ExpenseCategory>> {
        return service.getCategories()
    }
    
    override fun getCategoryById(categoryId: String): Flow<ExpenseCategory?> {
        return service.getCategoryById(categoryId)
    }
    
    override fun getDefaultCategories(): Flow<List<ExpenseCategory>> {
        return service.getCategories().map { categories ->
            categories.filter { it.isDefault }
        }
    }
    
    override fun getCustomCategories(): Flow<List<ExpenseCategory>> {
        return service.getCategories().map { categories ->
            categories.filter { !it.isDefault }
        }
    }
    
    override suspend fun isCategoryNameExists(name: String, excludeId: String?): Boolean {
        return service.isCategoryNameExists(name, excludeId)
    }
    
    override suspend fun seedDefaultCategories(): Result<Unit> {
        return service.seedDefaultCategories()
    }
}
