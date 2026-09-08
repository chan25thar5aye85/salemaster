package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.ExpenseType
import kotlinx.coroutines.flow.first

class CalculateBusinessProfitUseCase(
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository
) {
    suspend fun invoke(): Result<ProfitData> {
        return try {
            // Get first emission from flows (one-time fetch)
            val sales = saleRepository.getSales().first()
            val expenses = expenseRepository.getExpenses().first()
            
            val totalRevenue = sales.sumOf { it.total }
            
            // Only count business expenses
            val totalBusinessExpenses = expenses.sumOf { expense ->
                when (expense.type) {
                    ExpenseType.BUSINESS -> expense.amount
                    ExpenseType.MIXED -> expense.getBusinessAmount()
                    ExpenseType.PERSONAL -> 0
                }
            }
            
            val profit = totalRevenue - totalBusinessExpenses
            val profitMargin = if (totalRevenue > 0) {
                (profit.toDouble() / totalRevenue) * 100
            } else 0.0
            
            val personalExpenses = expenses.sumOf { 
                if (it.type == ExpenseType.PERSONAL) it.amount else 0 
            }
            
            Result.success(
                ProfitData(
                    revenue = totalRevenue,
                    businessExpenses = totalBusinessExpenses,
                    personalExpenses = personalExpenses,
                    profit = profit,
                    profitMargin = profitMargin
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ProfitData(
    val revenue: Int,
    val businessExpenses: Int,
    val personalExpenses: Int,
    val profit: Int,
    val profitMargin: Double
)
