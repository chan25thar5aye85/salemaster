package com.akari.retailer.features.money.domain.usecases

import android.util.Log
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class ExternalTransferUseCase(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) {
    private val TAG = "ExternalTransferUseCase"

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
            // ── Validation ────────────────────────────────────────────
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

            // ── Compute account change ───────────────────────────────
            val accountChange: Int = when (params.direction) {
                Direction.OUTGOING -> when (params.feeType) {
                    FeeType.FEE_PAID -> -(params.amount + params.fee)
                    else -> -params.amount
                }
                Direction.INCOMING -> when (params.feeType) {
                    FeeType.FEE_EARNED -> params.amount + params.fee
                    FeeType.FEE_PAID -> params.amount - params.fee
                    FeeType.NONE -> params.amount
                }
            }

            val db = accountRepository.firestore
            val accountsCol = db.collection("money_accounts")
            val txnsCol = db.collection("money_transactions")
            val now = System.currentTimeMillis()

            // ── Atomic transaction ────────────────────────────────────
            db.runTransaction { txn ->
                val accRef = accountsCol.document(params.accountId)

                // READ
                val accSnap = txn.get(accRef)
                if (!accSnap.exists()) {
                    throw IllegalStateException("Account not found")
                }
                val currentBalance = (accSnap.getLong("currentBalance") ?: 0L).toInt()

                // For OUTGOING, check sufficient balance
                if (params.direction == Direction.OUTGOING) {
                    val required = if (params.feeType == FeeType.FEE_PAID)
                        params.amount + params.fee else params.amount
                    if (currentBalance < required) {
                        throw IllegalStateException(
                            "Insufficient balance. Available: $currentBalance, Required: $required"
                        )
                    }
                }

                // WRITE — balance
                txn.update(accRef,
                    "currentBalance", FieldValue.increment(accountChange.toLong()),
                    "updatedAt", now
                )

                // WRITE — transaction doc
                val txType = when (params.direction) {
                    Direction.OUTGOING -> MoneyTransactionType.EXTERNAL_OUT
                    Direction.INCOMING -> MoneyTransactionType.EXTERNAL_IN
                }
                val txRef = txnsCol.document()
                txn.set(txRef, mapOf(
                    "type" to txType.name,
                    "fromAccountId" to if (params.direction == Direction.OUTGOING) params.accountId else "",
                    "toAccountId" to if (params.direction == Direction.INCOMING) params.accountId else "",
                    "amount" to params.amount,
                    "fee" to params.fee,
                    "feeType" to params.feeType.name,
                    "netAmount" to accountChange,
                    "description" to params.description.ifEmpty {
                        if (params.direction == Direction.OUTGOING)
                            "External transfer to ${params.externalAccountName}"
                        else
                            "External transfer from ${params.externalAccountName}"
                    },
                    "referenceId" to "",
                    "referenceType" to "EXTERNAL_TRANSFER",
                    "externalAccountName" to params.externalAccountName,
                    "externalAccountNumber" to params.externalAccountNumber,
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "External transfer failed: ${e.message}")
            Result.failure(e)
        }
    }
}
