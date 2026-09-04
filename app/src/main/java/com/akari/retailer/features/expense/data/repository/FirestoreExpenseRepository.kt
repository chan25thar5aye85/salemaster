package com.akari.retailer.features.expense.data.repository

import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.akari.retailer.features.expense.domain.models.Expense
import kotlinx.coroutines.flow.Flow

class FirestoreExpenseRepository(
    private val service: FirestoreExpenseService
) : ExpenseRepository {
    
    override suspend fun addExpense(expense: Expense): Result<String> {
        return service.addExpense(expense)
    }
    
    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return service.updateExpense(expense)
    }
    
    override suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return service.deleteExpense(expenseId)
    }
    
    override fun getExpenses(): Flow<List<Expense>> {
        return service.getExpenses()
    }
    
    override fun getExpenseById(expenseId: String): Flow<Expense?> {
        return service.getExpenseById(expenseId)
    }
}
