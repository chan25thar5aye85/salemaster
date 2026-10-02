package com.akari.retailer.features.money.domain.usecases

import android.util.Log
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class ExternalTransferUseCase(
    private val accountRepository: MoneyAccountRepository
    // transactionRepository was unused — writes go through accountRepository.firestore
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

            // ── Compute main account change ────────────────────────
            // The fee is a SEPARATE transaction that lands in the Cash
            // account, so it is NOT included in the main account's delta.
            val accountChange: Int = when (params.direction) {
                Direction.OUTGOING -> -params.amount
                Direction.INCOMING -> params.amount
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

                // ── Fee leg — separate transaction to Cash ────────────
                // Only when the user entered a fee > 0.
                // FEE_EARNED: cash increases by fee.
                // FEE_PAID:   cash decreases by fee.
                if (params.fee > 0 && params.feeType != FeeType.NONE) {
                    val cashRef = accountsCol.document("default_cash")
                    txn.get(cashRef)   // read before write (transaction rule)

                    val feeDelta = when (params.feeType) {
                        FeeType.FEE_EARNED -> params.fee.toLong()
                        FeeType.FEE_PAID -> -params.fee.toLong()
                        FeeType.NONE -> 0L
                    }

                    if (feeDelta != 0L) {
                        txn.update(
                            cashRef,
                            "currentBalance", FieldValue.increment(feeDelta),
                            "updatedAt", now
                        )

                        txn.set(txnsCol.document(), mapOf(
                            "type" to if (params.feeType == FeeType.FEE_EARNED)
                                MoneyTransactionType.FEE_IN.name
                            else
                                MoneyTransactionType.FEE_OUT.name,
                            "fromAccountId" to if (params.feeType == FeeType.FEE_PAID) "default_cash" else "",
                            "toAccountId" to if (params.feeType == FeeType.FEE_EARNED) "default_cash" else "",
                            "amount" to params.fee,
                            "fee" to 0,
                            "feeType" to "NONE",
                            "netAmount" to feeDelta,
                            "description" to "External transfer fee — ${params.externalAccountName}",
                            "referenceId" to (txRef.id),
                            "referenceType" to "EXTERNAL_TRANSFER_FEE",
                            "externalAccountName" to params.externalAccountName,
                            "externalAccountNumber" to "",
                            "date" to now,
                            "createdAt" to now
                        ))
                    }
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "External transfer failed: ${e.message}")
            Result.failure(e)
        }
    }
}
