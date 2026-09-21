package com.akari.retailer.features.money.domain.usecases

import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import kotlinx.coroutines.flow.first

class TransferMoneyUseCase(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) {
    data class Params(
        val fromAccountId: String,
        val toAccountId: String,
        val amount: Int,
        val fee: Int = 0,
        val feeType: FeeType = FeeType.NONE,
        val description: String = ""
    )
    
    suspend fun invoke(params: Params): Result<Unit> {
        return try {
            // Validation
            if (params.fromAccountId.isEmpty()) {
                return Result.failure(Exception("Source account is required"))
            }
            if (params.toAccountId.isEmpty()) {
                return Result.failure(Exception("Destination account is required"))
            }
            if (params.fromAccountId == params.toAccountId) {
                return Result.failure(Exception("Cannot transfer to the same account"))
            }
            if (params.amount <= 0) {
                return Result.failure(Exception("Amount must be greater than 0"))
            }
            if (params.fee < 0) {
                return Result.failure(Exception("Fee cannot be negative"))
            }
            
            // Get accounts
            val accounts = accountRepository.getAccounts().first()
            val fromAccount = accounts.find { it.id == params.fromAccountId }
                ?: return Result.failure(Exception("Source account not found"))
            val toAccount = accounts.find { it.id == params.toAccountId }
                ?: return Result.failure(Exception("Destination account not found"))
            
            // Calculate source deduction
            val sourceDeduction = when (params.feeType) {
                FeeType.FEE_PAID -> params.amount + params.fee
                else -> params.amount
            }
            
            // Check sufficient balance
            if (fromAccount.currentBalance < sourceDeduction) {
                return Result.failure(
                    Exception("Insufficient balance. Available: ${fromAccount.currentBalance}, Required: $sourceDeduction")
                )
            }
            
            // Calculate destination addition
            val destAddition = when (params.feeType) {
                FeeType.FEE_EARNED -> params.amount + params.fee
                else -> params.amount
            }
            
            // Record transaction
            val transaction = MoneyTransaction(
                type = MoneyTransactionType.TRANSFER_OUT,
                fromAccountId = params.fromAccountId,
                toAccountId = params.toAccountId,
                amount = params.amount,
                fee = params.fee,
                feeType = params.feeType,
                netAmount = destAddition,
                description = params.description.ifEmpty {
                    "Transfer from ${fromAccount.name} to ${toAccount.name}"
                },
                referenceType = "TRANSFER"
            )
            transactionRepository.addTransaction(transaction)
            
            // Update source balance
            val newFromBalance = fromAccount.currentBalance - sourceDeduction
            accountRepository.updateBalance(params.fromAccountId, newFromBalance)
            
            // Update destination balance
            val newToBalance = toAccount.currentBalance + destAddition
            accountRepository.updateBalance(params.toAccountId, newToBalance)
            
            // Record incoming transaction for destination
            val inTransaction = MoneyTransaction(
                type = MoneyTransactionType.TRANSFER_IN,
                fromAccountId = params.fromAccountId,
                toAccountId = params.toAccountId,
                amount = params.amount,
                fee = params.fee,
                feeType = params.feeType,
                netAmount = destAddition,
                description = "Received from ${fromAccount.name}",
                referenceType = "TRANSFER"
            )
            transactionRepository.addTransaction(inTransaction)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
