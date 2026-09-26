package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense

class AddExpenseUseCase(
    private val repository: ExpenseRepository
) : UseCase<Expense, Result<String>> {
    
    override suspend fun invoke(params: Expense): Result<String> {
        return try {
            if (params.title.isBlank()) {
                Result.failure(Exception("Expense title is required"))
            } else if (params.amount <= 0) {
                Result.failure(Exception("Amount must be greater than 0"))
            } else {
                repository.addExpense(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
