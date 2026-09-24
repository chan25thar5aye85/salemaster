package com.akari.retailer.features.customer.data.remote

import android.util.Log
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import com.akari.retailer.features.customer.domain.models.CreditTransactionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles credit transactions and keeps `customers/{id}.creditBalance` in sync.
 *
 * All writes are atomic: the balance update and the transaction doc are
 * written in a single `runTransaction`.
 */
class FirestoreCreditService {

    private val TAG = "FirestoreCredit"

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val txnsCollection = db.collection("credit_transactions")
    private val customersCollection = db.collection("customers")

    /**
     * Extend credit to a customer (a sale on credit).
     * Atomically:
     *   - increments customer.creditBalance by [amount]
     *   - writes a SALE_ON_CREDIT transaction
     * Returns the new credit transaction ID.
     */
    suspend fun extendCredit(
        customerId: String,
        amount: Int,
        saleId: String,
        description: String = ""
    ): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Amount must be > 0"))
            if (customerId.isEmpty()) return Result.failure(Exception("Customer is required"))

            val now = System.currentTimeMillis()
            val txnRef = txnsCollection.document()

            db.runTransaction { txn ->
                val customerRef = customersCollection.document(customerId)
                val customerSnap = txn.get(customerRef)
                if (!customerSnap.exists()) {
                    throw IllegalStateException("Customer not found")
                }

                // Bump balance
                txn.update(customerRef, "creditBalance", FieldValue.increment(amount.toLong()))

                // Log transaction
                txn.set(txnRef, mapOf(
                    "customerId" to customerId,
                    "type" to CreditTransactionType.SALE_ON_CREDIT.name,
                    "amount" to amount,
                    "saleId" to saleId,
                    "paymentAccountId" to "",
                    "description" to description,
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(txnRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "extendCredit failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Record a payment from a customer against their credit balance.
     * Atomically:
     *   - decrements customer.creditBalance by [amount]
     *   - increments the chosen money account's balance
     *   - writes a PAYMENT transaction
     * Returns the new credit transaction ID.
     */
    suspend fun recordPayment(
        customerId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Amount must be > 0"))
            if (customerId.isEmpty()) return Result.failure(Exception("Customer is required"))
            if (paymentAccountId.isEmpty()) return Result.failure(Exception("Payment account is required"))

            val now = System.currentTimeMillis()
            val txnRef = txnsCollection.document()
            val accountsCollection = db.collection("money_accounts")
            val moneyTxnsCollection = db.collection("money_transactions")

            db.runTransaction { txn ->
                val customerRef = customersCollection.document(customerId)
                val accountRef = accountsCollection.document(paymentAccountId)

                // Reads must come before writes
                val customerSnap = txn.get(customerRef)
                val accountSnap = txn.get(accountRef)

                if (!customerSnap.exists()) {
                    throw IllegalStateException("Customer not found")
                }
                if (!accountSnap.exists()) {
                    throw IllegalStateException("Payment account not found")
                }

                val currentBalance = (customerSnap.getLong("creditBalance") ?: 0L).toInt()
                if (amount > currentBalance) {
                    throw IllegalStateException(
                        "Payment exceeds balance. Owed: $currentBalance, Tried: $amount"
                    )
                }

                // Decrease customer credit
                txn.update(customerRef, "creditBalance", FieldValue.increment(-amount.toLong()))

                // Increase money account
                txn.update(accountRef, "currentBalance", FieldValue.increment(amount.toLong()))

                // Log credit-side transaction
                txn.set(txnRef, mapOf(
                    "customerId" to customerId,
                    "type" to CreditTransactionType.PAYMENT.name,
                    "amount" to -amount,
                    "saleId" to "",
                    "paymentAccountId" to paymentAccountId,
                    "description" to description,
                    "date" to now,
                    "createdAt" to now
                ))

                // Log money-side transaction
                txn.set(moneyTxnsCollection.document(), mapOf(
                    "type" to "INCOME_IN",
                    "fromAccountId" to "",
                    "toAccountId" to paymentAccountId,
                    "amount" to amount,
                    "fee" to 0,
                    "feeType" to "NONE",
                    "netAmount" to amount,
                    "description" to "Credit payment",
                    "referenceId" to customerId,
                    "referenceType" to "CREDIT_PAYMENT",
                    "externalAccountName" to "",
                    "externalAccountNumber" to "",
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(txnRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "recordPayment failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Refund the customer — we owe them money.
     * Atomically:
     *   - decrements customer.creditBalance by [amount] (goes negative)
     *   - debits the chosen money account (money leaves our account)
     *   - writes a REFUND transaction (negative amount)
     *   - writes a money_transactions doc
     * Returns the new credit transaction ID.
     */
    suspend fun recordRefund(
        customerId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Amount must be > 0"))
            if (customerId.isEmpty()) return Result.failure(Exception("Customer is required"))
            if (paymentAccountId.isEmpty()) return Result.failure(Exception("Refund account is required"))

            val now = System.currentTimeMillis()
            val txnRef = txnsCollection.document()
            val accountsCollection = db.collection("money_accounts")
            val moneyTxnsCollection = db.collection("money_transactions")

            db.runTransaction { txn ->
                val customerRef = customersCollection.document(customerId)
                val accountRef = accountsCollection.document(paymentAccountId)

                val customerSnap = txn.get(customerRef)
                val accountSnap = txn.get(accountRef)

                if (!customerSnap.exists()) {
                    throw IllegalStateException("Customer not found")
                }
                if (!accountSnap.exists()) {
                    throw IllegalStateException("Refund account not found")
                }

                val accountBalance = (accountSnap.getLong("currentBalance") ?: 0L).toInt()
                if (accountBalance < amount) {
                    throw IllegalStateException(
                        "Insufficient balance to refund. Available: $accountBalance, Needed: $amount"
                    )
                }

                // Customer balance goes DOWN (they now have credit with us)
                txn.update(customerRef, "creditBalance", FieldValue.increment(-amount.toLong()))

                // Money account goes DOWN (we paid them back)
                txn.update(accountRef, "currentBalance", FieldValue.increment(-amount.toLong()))

                // Log credit-side transaction (negative amount)
                txn.set(txnRef, mapOf(
                    "customerId" to customerId,
                    "type" to CreditTransactionType.REFUND.name,
                    "amount" to -amount,
                    "saleId" to "",
                    "paymentAccountId" to paymentAccountId,
                    "description" to description,
                    "date" to now,
                    "createdAt" to now
                ))

                // Log money-side transaction (outflow)
                txn.set(moneyTxnsCollection.document(), mapOf(
                    "type" to "EXPENSE_OUT",
                    "fromAccountId" to paymentAccountId,
                    "toAccountId" to "",
                    "amount" to amount,
                    "fee" to 0,
                    "feeType" to "NONE",
                    "netAmount" to amount,
                    "description" to "Customer refund",
                    "referenceId" to customerId,
                    "referenceType" to "CUSTOMER_REFUND",
                    "externalAccountName" to "",
                    "externalAccountNumber" to "",
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(txnRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "recordRefund failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * All credit transactions, sorted newest-first.
     */
    fun getTransactions(): Flow<List<CreditTransaction>> = callbackFlow {
        val listener = txnsCollection
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList()); return@addSnapshotListener
                }
                trySend(snapshot.documents.mapNotNull { mapDoc(it) })
            }
        awaitClose { listener.remove() }
    }

    /**
     * All credit transactions for one customer, sorted newest-first.
     */
    fun getTransactionsForCustomer(customerId: String): Flow<List<CreditTransaction>> = callbackFlow {
        val listener = txnsCollection
            .whereEqualTo("customerId", customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList()); return@addSnapshotListener
                }
                val list = snapshot.documents
                    .mapNotNull { mapDoc(it) }
                    .sortedByDescending { it.date }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private fun mapDoc(doc: com.google.firebase.firestore.DocumentSnapshot): CreditTransaction? {
        val data = doc.data ?: return null
        return try {
            CreditTransaction(
                id = doc.id,
                customerId = data["customerId"] as? String ?: "",
                type = try {
                    CreditTransactionType.valueOf(data["type"] as? String ?: "SALE_ON_CREDIT")
                } catch (e: Exception) {
                    CreditTransactionType.SALE_ON_CREDIT
                },
                amount = (data["amount"] as? Number)?.toInt() ?: 0,
                saleId = data["saleId"] as? String ?: "",
                paymentAccountId = data["paymentAccountId"] as? String ?: "",
                description = data["description"] as? String ?: "",
                date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
