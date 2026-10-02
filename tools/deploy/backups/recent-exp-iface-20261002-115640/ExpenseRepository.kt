package com.akari.retailer.features.expense.data.repository

import com.akari.retailer.features.expense.domain.models.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun addExpense(expense: Expense): Result<String>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>
    fun getExpenses(): Flow<List<Expense>>
    fun getExpenseById(expenseId: String): Flow<Expense?>
}
