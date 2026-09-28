package com.akari.retailer.features.sales.data.remote

import android.util.Log
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.sales.domain.models.IncomeEntry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Atomically finalizes income entry writes:
 *   - writes the income_entries document
 *   - applies all money payments (creates money_transactions, updates money_accounts)
 *   - on edit: reverses the previous payments, then applies the new ones
 *   - on delete: reverses the previous payments, then deletes the doc
 *
 * Every method runs a single Firestore transaction.
 */
class FirestoreIncomeFinalizer(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val TAG = "IncomeFinalizer"

    suspend fun addIncome(entry: IncomeEntry): Result<String> {
        return try {
            val incomeCol = db.collection("income_entries")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")

            val incomeRef = if (entry.id.isNotEmpty()) {
                incomeCol.document(entry.id)
            } else {
                incomeCol.document()
            }
            val incomeId = incomeRef.id
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                txn.set(incomeRef, entryToMap(entry, incomeId))

                entry.payments.forEach { payment ->
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.INCOME_IN.name,
                        "fromAccountId" to "",
                        "toAccountId" to payment.accountId,
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to entry.description.ifEmpty { "Income" },
                        "referenceId" to incomeId,
                        "referenceType" to "INCOME",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(payment.amount.toLong()),
                        "updatedAt", now
                    )
                }
            }.await()

            Log.d(TAG, "Income added atomically: $incomeId")
            Result.success(incomeId)
        } catch (e: Exception) {
            Log.e(TAG, "addIncome failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateIncome(entry: IncomeEntry): Result<Unit> {
        return try {
            if (entry.id.isEmpty()) {
                return Result.failure(Exception("Income ID is required for update"))
            }

            val incomeCol = db.collection("income_entries")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")
            val incomeRef = incomeCol.document(entry.id)
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                val existingSnap = txn.get(incomeRef)
                if (!existingSnap.exists()) {
                    throw IllegalStateException("Income entry not found")
                }

                val oldPayments = readPayments(existingSnap.data ?: emptyMap())

                // Reverse old payments
                oldPayments.forEach { payment ->
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(-payment.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.ADJUSTMENT.name,
                        "fromAccountId" to payment.accountId,
                        "toAccountId" to "",
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to "Income edit — reversal",
                        "referenceId" to entry.id,
                        "referenceType" to "INCOME_EDIT_REVERSAL",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                // Apply new payments
                entry.payments.forEach { payment ->
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(payment.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.INCOME_IN.name,
                        "fromAccountId" to "",
                        "toAccountId" to payment.accountId,
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to entry.description.ifEmpty { "Income" },
                        "referenceId" to entry.id,
                        "referenceType" to "INCOME",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                txn.set(incomeRef, entryToMap(entry, entry.id))
            }.await()

            Log.d(TAG, "Income updated atomically: ${entry.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateIncome failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteIncome(entryId: String): Result<Unit> {
        return try {
            val incomeCol = db.collection("income_entries")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")
            val incomeRef = incomeCol.document(entryId)
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                val existingSnap = txn.get(incomeRef)
                if (!existingSnap.exists()) {
                    return@runTransaction
                }

                val oldPayments = readPayments(existingSnap.data ?: emptyMap())

                oldPayments.forEach { payment ->
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(-payment.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.ADJUSTMENT.name,
                        "fromAccountId" to payment.accountId,
                        "toAccountId" to "",
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to "Income delete — reversal",
                        "referenceId" to entryId,
                        "referenceType" to "INCOME_DELETE_REVERSAL",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                txn.delete(incomeRef)
            }.await()

            Log.d(TAG, "Income deleted atomically: $entryId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteIncome failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun entryToMap(entry: IncomeEntry, id: String): Map<String, Any> {
        return mapOf(
            "amount" to entry.amount,
            "incomeStreamId" to entry.incomeStreamId,
            "payments" to entry.payments.map { p ->
                mapOf("accountId" to p.accountId, "amount" to p.amount)
            },
            "description" to entry.description,
            "type" to entry.type.name,
            "date" to entry.date,
            "createdAt" to if (entry.createdAt > 0) entry.createdAt else System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
    }

    private fun readPayments(data: Map<String, Any>): List<PaymentEntry> {
        val list = (data["payments"] as? List<*>)?.mapNotNull { p ->
            if (p is Map<*, *>) PaymentEntry(
                accountId = p["accountId"] as? String ?: "default_cash",
                amount = (p["amount"] as? Number)?.toInt() ?: 0
            ) else null
        } ?: emptyList()

        if (list.isNotEmpty()) return list

        val oldAccountId = data["accountId"] as? String ?: "default_cash"
        val oldAmount = (data["amount"] as? Number)?.toInt() ?: 0
        return if (oldAmount > 0) {
            listOf(PaymentEntry(accountId = oldAccountId, amount = oldAmount))
        } else emptyList()
    }
}
