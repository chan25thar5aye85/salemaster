package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.expense.data.repository.ExpenseRepository
import com.akari.retailer.features.expense.domain.models.ExpenseType
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.akari.retailer.features.sales.data.repository.IncomeEntryRepository
import com.akari.retailer.features.sales.domain.models.IncomeEntryType
import kotlinx.coroutines.flow.first

class CalculateBusinessProfitUseCase(
    private val saleRepository: SaleRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeEntryRepository: IncomeEntryRepository,
    private val moneyTransactionRepository: MoneyTransactionRepository
) {

    suspend fun invoke(): Result<ProfitData> {
        return try {
            // Get all data
            val sales = saleRepository.getSales().first()
            val expenses = expenseRepository.getExpenses().first()
            val incomeEntries = incomeEntryRepository.getIncomeEntries().first()
            val moneyTransactions = moneyTransactionRepository.getTransactions().first()
            
            // 1. Sales Revenue
            val salesRevenue = sales.sumOf { it.total }
            
            // 2. Other Business Income
            val incomeRevenue = incomeEntries
                .filter { it.type == IncomeEntryType.BUSINESS }
                .sumOf { it.amount }
            
            // 3. Transfer Fees Earned (money coming IN from fees)
            val transferFeesEarned = moneyTransactions
                .filter { 
                    it.feeType == FeeType.FEE_EARNED && 
                    it.type in listOf(
                        MoneyTransactionType.TRANSFER_IN,
                        MoneyTransactionType.TRANSFER_OUT,
                        MoneyTransactionType.EXTERNAL_IN,
                        MoneyTransactionType.EXTERNAL_OUT,
                        MoneyTransactionType.FEE_IN
                    )
                }
                .sumOf { it.fee }
            
            // 4. Transfer Fees Paid (money going OUT as fees - expense)
            val transferFeesPaid = moneyTransactions
                .filter { 
                    it.feeType == FeeType.FEE_PAID && 
                    it.type in listOf(
                        MoneyTransactionType.TRANSFER_IN,
                        MoneyTransactionType.TRANSFER_OUT,
                        MoneyTransactionType.EXTERNAL_IN,
                        MoneyTransactionType.EXTERNAL_OUT,
                        MoneyTransactionType.FEE_OUT
                    )
                }
                .sumOf { it.fee }
            
            // Total Revenue
            val totalRevenue = salesRevenue + incomeRevenue + transferFeesEarned
            
            // 5. Regular Business Expenses
            val regularBusinessExpenses = expenses.sumOf { expense ->
                when (expense.type) {
                    ExpenseType.BUSINESS -> expense.amount
                    ExpenseType.MIXED -> expense.getBusinessAmount()
                    ExpenseType.PERSONAL -> 0
                }
            }
            
            // Total Business Expenses
            val totalBusinessExpenses = regularBusinessExpenses + transferFeesPaid
            
            // Calculate Profit
            val profit = totalRevenue - totalBusinessExpenses
            val profitMargin = if (totalRevenue > 0) {
                (profit.toDouble() / totalRevenue) * 100
            } else 0.0
            
            // Personal expenses
            val personalExpenses = expenses.sumOf { 
                if (it.type == ExpenseType.PERSONAL) it.amount else 0 
            }
            
            Result.success(
                ProfitData(
                    revenue = totalRevenue,
                    salesRevenue = salesRevenue,
                    incomeRevenue = incomeRevenue,
                    transferFeesEarned = transferFeesEarned,
                    businessExpenses = totalBusinessExpenses,
                    regularBusinessExpenses = regularBusinessExpenses,
                    transferFeesPaid = transferFeesPaid,
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
    val transferFeesEarned: Int = 0,
    val businessExpenses: Int,
    val regularBusinessExpenses: Int = 0,
    val transferFeesPaid: Int = 0,
    val personalExpenses: Int,
    val profit: Int,
    val profitMargin: Double
)
