package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.expense.data.repository.ExpenseRepository

class DeleteExpenseUseCase(
    private val repository: ExpenseRepository
) : UseCase<String, Result<Unit>> {
    
    override suspend fun invoke(params: String): Result<Unit> {
        return try {
            if (params.isEmpty()) {
                Result.failure(Exception("Expense ID cannot be empty"))
            } else {
                repository.deleteExpense(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
