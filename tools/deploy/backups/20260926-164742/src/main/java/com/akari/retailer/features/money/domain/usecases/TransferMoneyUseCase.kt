package com.akari.retailer.features.money.domain.usecases

import android.util.Log
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.data.repository.MoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class TransferMoneyUseCase(
    private val accountRepository: MoneyAccountRepository,
    private val transactionRepository: MoneyTransactionRepository
) {
    private val TAG = "TransferMoneyUseCase"

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
            // ── Validation ────────────────────────────────────────────
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

            // ── Compute deltas ────────────────────────────────────────
            val sourceDeduction = when (params.feeType) {
                FeeType.FEE_PAID -> params.amount + params.fee
                else -> params.amount
            }
            val destAddition = when (params.feeType) {
                FeeType.FEE_EARNED -> params.amount + params.fee
                else -> params.amount
            }

            val db = accountRepository.firestore
            val accountsCol = db.collection("money_accounts")
            val txnsCol = db.collection("money_transactions")
            val now = System.currentTimeMillis()

            // ── Atomic transaction ────────────────────────────────────
            db.runTransaction { txn ->
                val fromRef = accountsCol.document(params.fromAccountId)
                val toRef = accountsCol.document(params.toAccountId)

                // READ (required before any WRITE in a transaction)
                val fromSnap = txn.get(fromRef)
                val toSnap = txn.get(toRef)

                if (!fromSnap.exists()) {
                    throw IllegalStateException("Source account not found")
                }
                if (!toSnap.exists()) {
                    throw IllegalStateException("Destination account not found")
                }

                val fromBalance = (fromSnap.getLong("currentBalance") ?: 0L).toInt()
                val fromName = fromSnap.getString("name") ?: ""
                val toName = toSnap.getString("name") ?: ""

                if (fromBalance < sourceDeduction) {
                    throw IllegalStateException(
                        "Insufficient balance. Available: $fromBalance, Required: $sourceDeduction"
                    )
                }

                // WRITE — source debit
                txn.update(fromRef,
                    "currentBalance", FieldValue.increment(-sourceDeduction.toLong()),
                    "updatedAt", now
                )

                // WRITE — destination credit
                txn.update(toRef,
                    "currentBalance", FieldValue.increment(destAddition.toLong()),
                    "updatedAt", now
                )

                // WRITE — outgoing transaction doc
                val outRef = txnsCol.document()
                txn.set(outRef, mapOf(
                    "type" to MoneyTransactionType.TRANSFER_OUT.name,
                    "fromAccountId" to params.fromAccountId,
                    "toAccountId" to params.toAccountId,
                    "amount" to params.amount,
                    "fee" to params.fee,
                    "feeType" to params.feeType.name,
                    "netAmount" to destAddition,
                    "description" to params.description.ifEmpty {
                        "Transfer from $fromName to $toName"
                    },
                    "referenceId" to "",
                    "referenceType" to "TRANSFER",
                    "externalAccountName" to "",
                    "externalAccountNumber" to "",
                    "date" to now,
                    "createdAt" to now
                ))

                // WRITE — incoming transaction doc
                val inRef = txnsCol.document()
                txn.set(inRef, mapOf(
                    "type" to MoneyTransactionType.TRANSFER_IN.name,
                    "fromAccountId" to params.fromAccountId,
                    "toAccountId" to params.toAccountId,
                    "amount" to params.amount,
                    "fee" to params.fee,
                    "feeType" to params.feeType.name,
                    "netAmount" to destAddition,
                    "description" to "Received from $fromName",
                    "referenceId" to "",
                    "referenceType" to "TRANSFER",
                    "externalAccountName" to "",
                    "externalAccountNumber" to "",
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Transfer failed: ${e.message}")
            Result.failure(e)
        }
    }
}
