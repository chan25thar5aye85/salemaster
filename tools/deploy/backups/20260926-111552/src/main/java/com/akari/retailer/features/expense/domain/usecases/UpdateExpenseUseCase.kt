package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense

class UpdateExpenseUseCase(
    private val repository: ExpenseRepository
) : UseCase<Expense, Result<Unit>> {
    
    override suspend fun invoke(params: Expense): Result<Unit> {
        return try {
            if (params.title.isBlank()) {
                Result.failure(Exception("Expense title is required"))
            } else if (params.amount <= 0) {
                Result.failure(Exception("Amount must be greater than 0"))
            } else if (params.id.isEmpty()) {
                Result.failure(Exception("Expense ID cannot be empty"))
            } else {
                repository.updateExpense(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
