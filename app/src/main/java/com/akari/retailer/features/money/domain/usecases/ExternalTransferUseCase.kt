package com.akari.retailer.features.money.domain.usecases

import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import kotlinx.coroutines.flow.first

class ExternalTransferUseCase(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) {
    enum class Direction { OUTGOING, INCOMING }
    
    data class Params(
        val direction: Direction,
        val accountId: String,
        val externalAccountName: String,
        val externalAccountNumber: String = "",
        val amount: Int,
        val fee: Int = 0,
        val feeType: FeeType = FeeType.NONE,
        val description: String = ""
    )
    
    suspend fun invoke(params: Params): Result<Unit> {
        return try {
            // Validation
            if (params.accountId.isEmpty()) {
                return Result.failure(Exception("Account is required"))
            }
            if (params.externalAccountName.isBlank()) {
                return Result.failure(Exception("External account name is required"))
            }
            if (params.amount <= 0) {
                return Result.failure(Exception("Amount must be greater than 0"))
            }
            if (params.fee < 0) {
                return Result.failure(Exception("Fee cannot be negative"))
            }
            
            // Get account
            val accounts = accountRepository.getAccounts().first()
            val account = accounts.find { it.id == params.accountId }
                ?: return Result.failure(Exception("Account not found"))
            
            // Calculate account change
            val accountChange: Int = when (params.direction) {
                Direction.OUTGOING -> {
                    when (params.feeType) {
                        FeeType.FEE_PAID -> -(params.amount + params.fee)
                        else -> -params.amount
                    }
                }
                Direction.INCOMING -> {
                    when (params.feeType) {
                        FeeType.FEE_EARNED -> params.amount + params.fee
                        FeeType.FEE_PAID -> params.amount - params.fee
                        FeeType.NONE -> params.amount
                    }
                }
            }
            
            // Check sufficient balance for outgoing
            if (params.direction == Direction.OUTGOING) {
                val required = if (params.feeType == FeeType.FEE_PAID) params.amount + params.fee else params.amount
                if (account.currentBalance < required) {
                    return Result.failure(
                        Exception("Insufficient balance. Available: ${account.currentBalance}, Required: $required")
                    )
                }
            }
            
            // Determine transaction type
            val transactionType = when (params.direction) {
                Direction.OUTGOING -> MoneyTransactionType.EXTERNAL_OUT
                Direction.INCOMING -> MoneyTransactionType.EXTERNAL_IN
            }
            
            // Record transaction
            val transaction = MoneyTransaction(
                type = transactionType,
                fromAccountId = if (params.direction == Direction.OUTGOING) params.accountId else "",
                toAccountId = if (params.direction == Direction.INCOMING) params.accountId else "",
                amount = params.amount,
                fee = params.fee,
                feeType = params.feeType,
                netAmount = accountChange,
                description = params.description.ifEmpty {
                    if (params.direction == Direction.OUTGOING) 
                        "External transfer to ${params.externalAccountName}"
                    else 
                        "External transfer from ${params.externalAccountName}"
                },
                externalAccountName = params.externalAccountName,
                externalAccountNumber = params.externalAccountNumber,
                referenceType = "EXTERNAL_TRANSFER"
            )
            transactionRepository.addTransaction(transaction)
            
            // Update account balance
            val newBalance = account.currentBalance + accountChange
            accountRepository.updateBalance(params.accountId, newBalance)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
