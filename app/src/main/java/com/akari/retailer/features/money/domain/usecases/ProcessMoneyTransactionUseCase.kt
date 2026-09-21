package com.akari.retailer.features.money.domain.usecases

import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType

class ProcessMoneyTransactionUseCase(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) {
    
    /**
     * Process a SALE - Add money to account
     */
    suspend fun processSale(
        accountId: String,
        amount: Int,
        saleId: String,
        description: String = "Sale"
    ): Result<Unit> {
        return try {
            // Record transaction
            val transaction = MoneyTransaction(
                type = MoneyTransactionType.SALE_IN,
                toAccountId = accountId,
                amount = amount,
                netAmount = amount,
                description = description,
                referenceId = saleId,
                referenceType = "SALE",
                feeType = FeeType.NONE
            )
            transactionRepository.addTransaction(transaction)
            
            // Update balance
            accountRepository.adjustBalance(accountId, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process INCOME - Add money to account
     */
    suspend fun processIncome(
        accountId: String,
        amount: Int,
        incomeId: String,
        description: String = "Income"
    ): Result<Unit> {
        return try {
            val transaction = MoneyTransaction(
                type = MoneyTransactionType.INCOME_IN,
                toAccountId = accountId,
                amount = amount,
                netAmount = amount,
                description = description,
                referenceId = incomeId,
                referenceType = "INCOME",
                feeType = FeeType.NONE
            )
            transactionRepository.addTransaction(transaction)
            
            accountRepository.adjustBalance(accountId, amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process EXPENSE - Deduct money from account
     */
    suspend fun processExpense(
        accountId: String,
        amount: Int,
        expenseId: String,
        description: String = "Expense"
    ): Result<Unit> {
        return try {
            val transaction = MoneyTransaction(
                type = MoneyTransactionType.EXPENSE_OUT,
                fromAccountId = accountId,
                amount = amount,
                netAmount = amount,
                description = description,
                referenceId = expenseId,
                referenceType = "EXPENSE",
                feeType = FeeType.NONE
            )
            transactionRepository.addTransaction(transaction)
            
            accountRepository.adjustBalance(accountId, -amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Process PURCHASE - Deduct money from account
     */
    suspend fun processPurchase(
        accountId: String,
        amount: Int,
        purchaseId: String,
        description: String = "Purchase"
    ): Result<Unit> {
        return try {
            val transaction = MoneyTransaction(
                type = MoneyTransactionType.PURCHASE_OUT,
                fromAccountId = accountId,
                amount = amount,
                netAmount = amount,
                description = description,
                referenceId = purchaseId,
                referenceType = "PURCHASE",
                feeType = FeeType.NONE
            )
            transactionRepository.addTransaction(transaction)
            
            accountRepository.adjustBalance(accountId, -amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a SALE - Revert money from account
     */
    suspend fun revertSale(
        accountId: String,
        amount: Int,
        saleId: String
    ): Result<Unit> {
        return try {
            val transaction = MoneyTransaction(
                type = MoneyTransactionType.ADJUSTMENT,
                fromAccountId = accountId,
                amount = amount,
                netAmount = amount,
                description = "Sale deleted - revert",
                referenceId = saleId,
                referenceType = "SALE_DELETE",
                feeType = FeeType.NONE
            )
            transactionRepository.addTransaction(transaction)
            
            accountRepository.adjustBalance(accountId, -amount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
