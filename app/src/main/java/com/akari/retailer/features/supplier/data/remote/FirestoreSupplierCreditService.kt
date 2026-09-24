package com.akari.retailer.features.supplier.data.remote

import android.util.Log
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import com.akari.retailer.features.supplier.domain.models.SupplierTransactionType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Handles supplier payables.
 *
 * All writes are atomic: the balance update and the transaction doc go in
 * one runTransaction.
 *
 * Sign convention: suppliers.{id}.payableBalance is positive when we owe them.
 */
class FirestoreSupplierCreditService {

    private val TAG = "FirestoreSupplierCredit"

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val txnsCollection = db.collection("supplier_transactions")
    private val suppliersCollection = db.collection("suppliers")

    /**
     * Record a purchase on credit — we owe the supplier more.
     * Atomically bumps suppliers.payableBalance and writes a PURCHASE_ON_CREDIT.
     */
    suspend fun recordPurchaseOnCredit(
        supplierId: String,
        amount: Int,
        purchaseId: String,
        description: String = ""
    ): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Amount must be > 0"))
            if (supplierId.isEmpty()) return Result.failure(Exception("Supplier is required"))

            val now = System.currentTimeMillis()
            val txnRef = txnsCollection.document()

            db.runTransaction { txn ->
                val supplierRef = suppliersCollection.document(supplierId)
                val supplierSnap = txn.get(supplierRef)
                if (!supplierSnap.exists()) {
                    throw IllegalStateException("Supplier not found")
                }

                // Increase payable balance
                txn.update(supplierRef, "payableBalance", FieldValue.increment(amount.toLong()))

                // Log transaction
                txn.set(txnRef, mapOf(
                    "supplierId" to supplierId,
                    "type" to SupplierTransactionType.PURCHASE_ON_CREDIT.name,
                    "amount" to amount,
                    "purchaseId" to purchaseId,
                    "paymentAccountId" to "",
                    "description" to description,
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(txnRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "recordPurchaseOnCredit failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Record a payment to a supplier — we owe them less.
     * Atomically:
     *   - decrements suppliers.payableBalance
     *   - decrements money_accounts.{id}.currentBalance
     *   - writes a PAYMENT supplier transaction
     *   - writes a money_transactions doc
     */
    suspend fun recordPayment(
        supplierId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Amount must be > 0"))
            if (supplierId.isEmpty()) return Result.failure(Exception("Supplier is required"))
            if (paymentAccountId.isEmpty()) return Result.failure(Exception("Payment account is required"))

            val now = System.currentTimeMillis()
            val txnRef = txnsCollection.document()
            val accountsCollection = db.collection("money_accounts")
            val moneyTxnsCollection = db.collection("money_transactions")

            db.runTransaction { txn ->
                val supplierRef = suppliersCollection.document(supplierId)
                val accountRef = accountsCollection.document(paymentAccountId)

                val supplierSnap = txn.get(supplierRef)
                val accountSnap = txn.get(accountRef)

                if (!supplierSnap.exists()) {
                    throw IllegalStateException("Supplier not found")
                }
                if (!accountSnap.exists()) {
                    throw IllegalStateException("Payment account not found")
                }

                val currentPayable = (supplierSnap.getLong("payableBalance") ?: 0L).toInt()
                if (amount > currentPayable) {
                    throw IllegalStateException(
                        "Payment exceeds amount owed. Owed: $currentPayable, Tried: $amount"
                    )
                }

                // Decrease payable balance
                txn.update(supplierRef, "payableBalance", FieldValue.increment(-amount.toLong()))

                // Decrease money account
                txn.update(accountRef, "currentBalance", FieldValue.increment(-amount.toLong()))

                // Log supplier-side transaction
                txn.set(txnRef, mapOf(
                    "supplierId" to supplierId,
                    "type" to SupplierTransactionType.PAYMENT.name,
                    "amount" to -amount,
                    "purchaseId" to "",
                    "paymentAccountId" to paymentAccountId,
                    "description" to description,
                    "date" to now,
                    "createdAt" to now
                ))

                // Log money-side transaction
                txn.set(moneyTxnsCollection.document(), mapOf(
                    "type" to "PURCHASE_OUT",
                    "fromAccountId" to paymentAccountId,
                    "toAccountId" to "",
                    "amount" to amount,
                    "fee" to 0,
                    "feeType" to "NONE",
                    "netAmount" to amount,
                    "description" to "Supplier payment",
                    "referenceId" to supplierId,
                    "referenceType" to "SUPPLIER_PAYMENT",
                    "externalAccountName" to "",
                    "externalAccountNumber" to "",
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(txnRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "recordPayment failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Supplier refunded us — they owe us less (or we're now owed).
     * Atomically:
     *   - increments supplier.payableBalance by [amount] (toward zero, or positive)
     *   - increments the money account
     *   - writes a REFUND_RECEIVED supplier transaction
     *   - writes a money_transactions doc (inflow)
     */
    suspend fun recordRefundReceived(
        supplierId: String,
        amount: Int,
        paymentAccountId: String,
        description: String = ""
    ): Result<String> {
        return try {
            if (amount <= 0) return Result.failure(Exception("Amount must be > 0"))
            if (supplierId.isEmpty()) return Result.failure(Exception("Supplier is required"))
            if (paymentAccountId.isEmpty()) return Result.failure(Exception("Account is required"))

            val now = System.currentTimeMillis()
            val txnRef = txnsCollection.document()
            val accountsCollection = db.collection("money_accounts")
            val moneyTxnsCollection = db.collection("money_transactions")

            db.runTransaction { txn ->
                val supplierRef = suppliersCollection.document(supplierId)
                val accountRef = accountsCollection.document(paymentAccountId)

                val supplierSnap = txn.get(supplierRef)
                val accountSnap = txn.get(accountRef)

                if (!supplierSnap.exists()) {
                    throw IllegalStateException("Supplier not found")
                }
                if (!accountSnap.exists()) {
                    throw IllegalStateException("Account not found")
                }

                // Supplier balance goes UP (toward zero from negative, or positive)
                txn.update(supplierRef, "payableBalance", FieldValue.increment(amount.toLong()))

                // Money account goes UP (money comes back to us)
                txn.update(accountRef, "currentBalance", FieldValue.increment(amount.toLong()))

                // Log supplier-side transaction
                txn.set(txnRef, mapOf(
                    "supplierId" to supplierId,
                    "type" to SupplierTransactionType.REFUND_RECEIVED.name,
                    "amount" to amount,
                    "purchaseId" to "",
                    "paymentAccountId" to paymentAccountId,
                    "description" to description,
                    "date" to now,
                    "createdAt" to now
                ))

                // Log money-side transaction (inflow)
                txn.set(moneyTxnsCollection.document(), mapOf(
                    "type" to "INCOME_IN",
                    "fromAccountId" to "",
                    "toAccountId" to paymentAccountId,
                    "amount" to amount,
                    "fee" to 0,
                    "feeType" to "NONE",
                    "netAmount" to amount,
                    "description" to "Supplier refund",
                    "referenceId" to supplierId,
                    "referenceType" to "SUPPLIER_REFUND",
                    "externalAccountName" to "",
                    "externalAccountNumber" to "",
                    "date" to now,
                    "createdAt" to now
                ))
            }.await()

            Result.success(txnRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "recordRefundReceived failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * All supplier transactions, newest first.
     */
    fun getTransactions(): Flow<List<SupplierTransaction>> = callbackFlow {
        val listener = txnsCollection
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snapshot.documents.mapNotNull { mapDoc(it) })
            }
        awaitClose { listener.remove() }
    }

    /**
     * Transactions for one supplier, newest first.
     */
    fun getTransactionsForSupplier(supplierId: String): Flow<List<SupplierTransaction>> = callbackFlow {
        val listener = txnsCollection
            .whereEqualTo("supplierId", supplierId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snapshot.documents
                    .mapNotNull { mapDoc(it) }
                    .sortedByDescending { it.date }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private fun mapDoc(doc: com.google.firebase.firestore.DocumentSnapshot): SupplierTransaction? {
        val data = doc.data ?: return null
        return try {
            SupplierTransaction(
                id = doc.id,
                supplierId = data["supplierId"] as? String ?: "",
                type = try {
                    SupplierTransactionType.valueOf(data["type"] as? String ?: "PURCHASE_ON_CREDIT")
                } catch (e: Exception) {
                    SupplierTransactionType.PURCHASE_ON_CREDIT
                },
                amount = (data["amount"] as? Number)?.toInt() ?: 0,
                purchaseId = data["purchaseId"] as? String ?: "",
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
