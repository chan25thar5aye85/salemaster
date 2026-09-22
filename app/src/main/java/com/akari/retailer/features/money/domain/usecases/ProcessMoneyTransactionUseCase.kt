package com.akari.retailer.features.money.domain.usecases

import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.akari.retailer.features.money.domain.models.PaymentEntry

class ProcessMoneyTransactionUseCase(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) {
    
    // ✅ SALE - Multiple payments
    suspend fun processSale(
        payments: List<PaymentEntry>,
        saleId: String,
        description: String = "Sale"
    ): Result<Unit> {
        return try {
            payments.forEach { payment ->
                val transaction = MoneyTransaction(
                    type = MoneyTransactionType.SALE_IN,
                    toAccountId = payment.accountId,
                    amount = payment.amount,
                    netAmount = payment.amount,
                    description = description,
                    referenceId = saleId,
                    referenceType = "SALE",
                    feeType = FeeType.NONE
                )
                transactionRepository.addTransaction(transaction)
                accountRepository.adjustBalance(payment.accountId, payment.amount)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ✅ INCOME - Multiple payments
    suspend fun processIncome(
        payments: List<PaymentEntry>,
        incomeId: String,
        description: String = "Income"
    ): Result<Unit> {
        return try {
            payments.forEach { payment ->
                val transaction = MoneyTransaction(
                    type = MoneyTransactionType.INCOME_IN,
                    toAccountId = payment.accountId,
                    amount = payment.amount,
                    netAmount = payment.amount,
                    description = description,
                    referenceId = incomeId,
                    referenceType = "INCOME",
                    feeType = FeeType.NONE
                )
                transactionRepository.addTransaction(transaction)
                accountRepository.adjustBalance(payment.accountId, payment.amount)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ✅ EXPENSE - Multiple payments
    suspend fun processExpense(
        payments: List<PaymentEntry>,
        expenseId: String,
        description: String = "Expense"
    ): Result<Unit> {
        return try {
            payments.forEach { payment ->
                val transaction = MoneyTransaction(
                    type = MoneyTransactionType.EXPENSE_OUT,
                    fromAccountId = payment.accountId,
                    amount = payment.amount,
                    netAmount = payment.amount,
                    description = description,
                    referenceId = expenseId,
                    referenceType = "EXPENSE",
                    feeType = FeeType.NONE
                )
                transactionRepository.addTransaction(transaction)
                accountRepository.adjustBalance(payment.accountId, -payment.amount)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ✅ PURCHASE - Multiple payments
    suspend fun processPurchase(
        payments: List<PaymentEntry>,
        purchaseId: String,
        description: String = "Purchase"
    ): Result<Unit> {
        return try {
            payments.forEach { payment ->
                val transaction = MoneyTransaction(
                    type = MoneyTransactionType.PURCHASE_OUT,
                    fromAccountId = payment.accountId,
                    amount = payment.amount,
                    netAmount = payment.amount,
                    description = description,
                    referenceId = purchaseId,
                    referenceType = "PURCHASE",
                    feeType = FeeType.NONE
                )
                transactionRepository.addTransaction(transaction)
                accountRepository.adjustBalance(payment.accountId, -payment.amount)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ✅ REVERT SALE
    suspend fun revertSale(
        payments: List<PaymentEntry>,
        saleId: String
    ): Result<Unit> {
        return try {
            payments.forEach { payment ->
                val transaction = MoneyTransaction(
                    type = MoneyTransactionType.ADJUSTMENT,
                    fromAccountId = payment.accountId,
                    amount = payment.amount,
                    netAmount = payment.amount,
                    description = "Sale deleted - revert",
                    referenceId = saleId,
                    referenceType = "SALE_DELETE",
                    feeType = FeeType.NONE
                )
                transactionRepository.addTransaction(transaction)
                accountRepository.adjustBalance(payment.accountId, -payment.amount)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
