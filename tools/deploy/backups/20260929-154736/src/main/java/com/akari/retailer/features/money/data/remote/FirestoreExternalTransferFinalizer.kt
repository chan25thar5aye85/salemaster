package com.akari.retailer.features.money.data.remote

import android.util.Log
import com.akari.retailer.features.money.data.repository.ExternalTransferFinalizer
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreExternalTransferFinalizer(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ExternalTransferFinalizer {

    private val TAG = "ExtTransferFinalizer"

    override suspend fun deleteTransfer(transactionId: String): Result<Unit> {
        return try {
            val txnsCol = db.collection("money_transactions")
            val accountsCol = db.collection("money_accounts")
            val txnRef = txnsCol.document(transactionId)
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                // ── 1. READ the transfer doc ──
                val snap = txn.get(txnRef)
                if (!snap.exists()) return@runTransaction   // already gone

                val data = snap.data ?: return@runTransaction
                val typeStr = data["type"] as? String ?: return@runTransaction
                val type = try { MoneyTransactionType.valueOf(typeStr) } catch (e: Exception) { return@runTransaction }

                // Only handle external transfers
                if (type != MoneyTransactionType.EXTERNAL_OUT &&
                    type != MoneyTransactionType.EXTERNAL_IN
                ) {
                    throw IllegalStateException("Not an external transfer")
                }

                val amount = (data["amount"] as? Number)?.toInt() ?: 0
                val fee = (data["fee"] as? Number)?.toInt() ?: 0
                val feeType = try {
                    FeeType.valueOf(data["feeType"] as? String ?: "NONE")
                } catch (e: Exception) { FeeType.NONE }

                val fromAccountId = data["fromAccountId"] as? String ?: ""
                val toAccountId = data["toAccountId"] as? String ?: ""

                // ── 2. Compute the reversal amount on the account ──
                // Outgoing: original debited amount + fee. Reverse = credit amount + fee.
                // Incoming: original credited amount ± fee. Reverse = debit the same.
                val accountId: String
                val reversal: Int

                when (type) {
                    MoneyTransactionType.EXTERNAL_OUT -> {
                        accountId = fromAccountId
                        reversal = when (feeType) {
                            FeeType.FEE_PAID -> amount + fee
                            else -> amount
                        }
                    }
                    MoneyTransactionType.EXTERNAL_IN -> {
                        accountId = toAccountId
                        reversal = when (feeType) {
                            FeeType.FEE_EARNED -> amount + fee
                            FeeType.FEE_PAID -> amount - fee
                            FeeType.NONE -> amount
                        }
                    }
                    else -> return@runTransaction
                }

                if (accountId.isBlank()) return@runTransaction

                // ── 3. Reversal: credit back outgoing, debit back incoming ──
                val accountRef = accountsCol.document(accountId)
                txn.get(accountRef) // read before write

                txn.update(
                    accountRef,
                    "currentBalance", FieldValue.increment(reversal.toLong()),
                    "updatedAt", now
                )

                // ── 4. Log the reversal in money_transactions ──
                txn.set(txnsCol.document(), mapOf(
                    "type"                 to MoneyTransactionType.ADJUSTMENT.name,
                    "fromAccountId"        to "",
                    "toAccountId"          to accountId,
                    "amount"               to reversal,
                    "fee"                  to 0,
                    "feeType"              to "NONE",
                    "netAmount"            to reversal,
                    "description"          to "External transfer delete — reversal",
                    "referenceId"          to transactionId,
                    "referenceType"        to "EXTERNAL_TRANSFER_DELETE_REVERSAL",
                    "externalAccountName"  to (data["externalAccountName"] as? String ?: ""),
                    "externalAccountNumber" to "",
                    "date"                 to now,
                    "createdAt"            to now
                ))

                // ── 5. Delete the original transfer doc ──
                txn.delete(txnRef)
            }.await()

            Log.d(TAG, "External transfer deleted atomically: $transactionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTransfer failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
