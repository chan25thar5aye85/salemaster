package com.akari.retailer.features.expense.data.remote

import android.util.Log
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Atomically finalizes expense writes:
 *   - writes the expense document
 *   - applies all money payments (creates money_transactions, updates money_accounts)
 *   - on edit: reverses the previous payments, then applies the new ones
 *   - on delete: reverses the previous payments, then deletes the doc
 *
 * Every method runs a single Firestore transaction. Either everything
 * succeeds, or nothing does.
 */
class FirestoreExpenseFinalizer(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val TAG = "ExpenseFinalizer"

    suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val expensesCol = db.collection("expenses")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")

            val expenseRef = if (expense.id.isNotEmpty()) {
                expensesCol.document(expense.id)
            } else {
                expensesCol.document()
            }
            val expenseId = expenseRef.id
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                // WRITE expense doc
                txn.set(expenseRef, expenseToMap(expense, expenseId))

                // WRITE money: debit each account + log transaction
                expense.payments.forEach { payment ->
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.EXPENSE_OUT.name,
                        "fromAccountId" to payment.accountId,
                        "toAccountId" to "",
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to expense.title,
                        "referenceId" to expenseId,
                        "referenceType" to "EXPENSE",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(-payment.amount.toLong()),
                        "updatedAt", now
                    )
                }
            }.await()

            Log.d(TAG, "Expense added atomically: $expenseId")
            Result.success(expenseId)
        } catch (e: Exception) {
            Log.e(TAG, "addExpense failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            if (expense.id.isEmpty()) {
                return Result.failure(Exception("Expense ID is required for update"))
            }

            val expensesCol = db.collection("expenses")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")
            val expenseRef = expensesCol.document(expense.id)
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                // READ: existing expense
                val existingSnap = txn.get(expenseRef)
                if (!existingSnap.exists()) {
                    throw IllegalStateException("Expense not found")
                }

                val oldPayments = readPayments(existingSnap.data ?: emptyMap())

                // WRITE 1: reverse old payments (accounts += old amount, log ADJUSTMENT)
                oldPayments.forEach { payment ->
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(payment.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.ADJUSTMENT.name,
                        "fromAccountId" to "",
                        "toAccountId" to payment.accountId,
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to "Expense edit — reversal",
                        "referenceId" to expense.id,
                        "referenceType" to "EXPENSE_EDIT_REVERSAL",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                // WRITE 2: apply new payments (accounts -= new amount, log EXPENSE_OUT)
                expense.payments.forEach { payment ->
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(-payment.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.EXPENSE_OUT.name,
                        "fromAccountId" to payment.accountId,
                        "toAccountId" to "",
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to expense.title,
                        "referenceId" to expense.id,
                        "referenceType" to "EXPENSE",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                // WRITE 3: update expense doc
                txn.set(expenseRef, expenseToMap(expense, expense.id))
            }.await()

            Log.d(TAG, "Expense updated atomically: ${expense.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateExpense failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            val expensesCol = db.collection("expenses")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")
            val expenseRef = expensesCol.document(expenseId)
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                // READ: existing expense
                val existingSnap = txn.get(expenseRef)
                if (!existingSnap.exists()) {
                    // Nothing to delete — treat as success
                    return@runTransaction
                }

                val oldPayments = readPayments(existingSnap.data ?: emptyMap())

                // WRITE 1: reverse old payments
                oldPayments.forEach { payment ->
                    txn.update(
                        accountsCol.document(payment.accountId),
                        "currentBalance", FieldValue.increment(payment.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.ADJUSTMENT.name,
                        "fromAccountId" to "",
                        "toAccountId" to payment.accountId,
                        "amount" to payment.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to payment.amount,
                        "description" to "Expense delete — reversal",
                        "referenceId" to expenseId,
                        "referenceType" to "EXPENSE_DELETE_REVERSAL",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                // WRITE 2: delete expense doc
                txn.delete(expenseRef)
            }.await()

            Log.d(TAG, "Expense deleted atomically: $expenseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteExpense failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun expenseToMap(expense: Expense, id: String): Map<String, Any> {
        return mapOf(
            "title" to expense.title,
            "amount" to expense.amount,
            "categoryId" to expense.categoryId,
            "type" to expense.type.name,
            "businessPercentage" to expense.businessPercentage,
            "payments" to expense.payments.map { p ->
                mapOf("accountId" to p.accountId, "amount" to p.amount)
            },
            "description" to expense.description,
            "date" to expense.date,
            "createdAt" to if (expense.createdAt > 0) expense.createdAt else System.currentTimeMillis(),
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

        // Backward compat: old expenses have a single accountId
        val oldAccountId = data["accountId"] as? String ?: "default_cash"
        val oldAmount = (data["amount"] as? Number)?.toInt() ?: 0
        return if (oldAmount > 0) {
            listOf(PaymentEntry(accountId = oldAccountId, amount = oldAmount))
        } else emptyList()
    }
}
