package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.ExpenseType
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import kotlinx.coroutines.flow.first

class CalculateBusinessProfitUseCase(
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeEntryRepository: IncomeEntryRepository
) {

    suspend fun invoke(): Result<ProfitData> {
        return try {
            // Get all data
            val sales = saleRepository.getSales().first()
            val expenses = expenseRepository.getExpenses().first()
            val incomeEntries = incomeEntryRepository.getIncomeEntries().first()
            
            // Revenue = Sales + Income Entries (business only)
            val salesRevenue = sales.sumOf { it.total }
            val incomeRevenue = incomeEntries
                .filter { it.type == IncomeEntryType.BUSINESS }
                .sumOf { it.amount }
            val totalRevenue = salesRevenue + incomeRevenue
            
            // Business Expenses
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
                    salesRevenue = salesRevenue,
                    incomeRevenue = incomeRevenue,
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
    val salesRevenue: Int = 0,
    val incomeRevenue: Int = 0,
    val businessExpenses: Int,
    val personalExpenses: Int,
    val profit: Int,
    val profitMargin: Double
)
