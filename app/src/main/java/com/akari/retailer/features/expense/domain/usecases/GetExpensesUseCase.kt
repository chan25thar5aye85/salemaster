package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.core.usecases.NoParamUseCase
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.Expense
import kotlinx.coroutines.flow.Flow

class GetExpensesUseCase(
    private val repository: ExpenseRepository
) : NoParamUseCase<Flow<List<Expense>>> {
    
    override suspend fun invoke(): Flow<List<Expense>> {
        return repository.getExpenses()
    }
}
