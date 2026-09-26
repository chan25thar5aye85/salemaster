package com.akari.retailer.features.sales.data.remote

import android.util.Log
import com.akari.retailer.features.money.domain.models.CreditAccount
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.sales.data.repository.SaleFinalizer
import com.akari.retailer.features.sales.domain.models.Sale
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSaleFinalizer : SaleFinalizer {

    private val TAG = "SaleFinalizer"
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override suspend fun finalizeSale(
        sale: Sale,
        overpaymentCredit: com.akari.retailer.features.money.domain.models.PaymentEntry?
    ): Result<String> {
        return try {
            val salesCol = db.collection("sales")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")
            val creditTxnsCol = db.collection("credit_transactions")
            val customersCol = db.collection("customers")

            // Pre-generate sale ID so the transaction can use it
            val saleRef = salesCol.document()
            val saleId = saleRef.id
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                // ── 1. READ: validate customer docs for credit rows ──
                val creditRows = sale.payments.filter { it.accountId == CreditAccount.ID }
                for (row in creditRows) {
                    val custRef = customersCol.document(row.customerId)
                    val custSnap = txn.get(custRef)
                    if (!custSnap.exists()) {
                        throw IllegalStateException("Customer not found: ${row.customerId}")
                    }
                }

                // Read the overpayment customer up front (transaction rule:
                // all reads must precede all writes).
                if (overpaymentCredit != null && overpaymentCredit.amount > 0) {
                    val opCustSnap = txn.get(customersCol.document(overpaymentCredit.customerId))
                    if (!opCustSnap.exists()) {
                        throw IllegalStateException("Customer not found: ${overpaymentCredit.customerId}")
                    }
                }

                // ── 2. WRITE: sale document ──
                txn.set(saleRef, mapOf(
                    "items" to sale.items.map { item ->
                        mapOf(
                            "productId" to item.productId,
                            "quantity" to item.quantity,
                            "price" to item.price,
                            "total" to item.total
                        )
                    },
                    "total" to sale.total,
                    "payments" to sale.payments.map { p ->
                        mapOf(
                            "accountId" to p.accountId,
                            "amount" to p.amount,
                            "customerId" to p.customerId
                        )
                    },
                    "timestamp" to sale.timestamp,
                    "cashierId" to sale.cashierId,
                    "notes" to sale.notes,
                    // Store overpayment credit so deleteSale can reverse it later.
                    // Old sales (before this field) simply won't have it.
                    "overpaymentCredit" to if (overpaymentCredit != null && overpaymentCredit.amount > 0) {
                        mapOf(
                            "customerId" to overpaymentCredit.customerId,
                            "amount" to overpaymentCredit.amount
                        )
                    } else null
                ))

                // ── 3. WRITE: money payments + account balances ──
                val moneyRows = sale.payments.filter { !it.isCredit }
                for (row in moneyRows) {
                    // money_transactions doc
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.SALE_IN.name,
                        "fromAccountId" to "",
                        "toAccountId" to row.accountId,
                        "amount" to row.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to row.amount,
                        "description" to "Sale",
                        "referenceId" to saleId,
                        "referenceType" to "SALE",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))

                    // account balance
                    txn.update(
                        accountsCol.document(row.accountId),
                        "currentBalance", FieldValue.increment(row.amount.toLong()),
                        "updatedAt", now
                    )
                }

                // ── 4. WRITE: credit payments + customer balances ──
                for (row in creditRows) {
                    // credit_transactions doc
                    txn.set(creditTxnsCol.document(), mapOf(
                        "customerId" to row.customerId,
                        "type" to "SALE_ON_CREDIT",
                        "amount" to row.amount,
                        "saleId" to saleId,
                        "paymentAccountId" to "",
                        "description" to "Sale on credit (partial)",
                        "date" to now,
                        "createdAt" to now
                    ))

                    // customer credit balance
                    txn.update(
                        customersCol.document(row.customerId),
                        "creditBalance", FieldValue.increment(row.amount.toLong()),
                        "updatedAt", now
                    )
                }

                // ── 5. WRITE: overpayment credit (if any) ──
                // If the customer paid more than the total, we now owe them the excess.
                if (overpaymentCredit != null && overpaymentCredit.amount > 0) {
                    // Customer credit balance goes DOWN (we owe them).
                    // The read already happened in step 1, so this is a pure write.
                    txn.update(
                        customersCol.document(overpaymentCredit.customerId),
                        "creditBalance", FieldValue.increment(-overpaymentCredit.amount.toLong()),
                        "updatedAt", now
                    )

                    // Log the overpayment credit as a REFUND transaction
                    txn.set(creditTxnsCol.document(), mapOf(
                        "customerId" to overpaymentCredit.customerId,
                        "type" to "REFUND",
                        "amount" to -overpaymentCredit.amount,
                        "saleId" to saleId,
                        "paymentAccountId" to "",
                        "description" to "Sale overpayment credit",
                        "date" to now,
                        "createdAt" to now
                    ))
                }
            }.await()

            Log.d(TAG, "Sale finalized atomically: $saleId")
            Result.success(saleId)
        } catch (e: Exception) {
            Log.e(TAG, "finalizeSale failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteSale(saleId: String): Result<Unit> {
        return try {
            val salesCol = db.collection("sales")
            val accountsCol = db.collection("money_accounts")
            val moneyTxnsCol = db.collection("money_transactions")
            val creditTxnsCol = db.collection("credit_transactions")
            val customersCol = db.collection("customers")

            val saleRef = salesCol.document(saleId)
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                // ── 1. READ: sale doc ──
                val saleSnap = txn.get(saleRef)
                if (!saleSnap.exists()) {
                    // Nothing to delete — treat as success
                    return@runTransaction
                }

                val data = saleSnap.data ?: emptyMap()

                val payments = (data["payments"] as? List<*>)?.mapNotNull { p ->
                    if (p is Map<*, *>) PaymentEntry(
                        accountId = p["accountId"] as? String ?: "",
                        amount = (p["amount"] as? Number)?.toInt() ?: 0,
                        customerId = p["customerId"] as? String ?: ""
                    ) else null
                } ?: emptyList()

                val overpaymentData = data["overpaymentCredit"] as? Map<*, *>
                val overpaymentAmount = (overpaymentData?.get("amount") as? Number)?.toInt() ?: 0
                val overpaymentCustomerId = overpaymentData?.get("customerId") as? String ?: ""

                // ── 2. READ: customer docs before any writes ──
                val creditCustomerIds = payments
                    .filter { it.isCredit }
                    .map { it.customerId }
                    .filter { it.isNotEmpty() }
                    .toMutableSet()
                if (overpaymentCustomerId.isNotEmpty()) {
                    creditCustomerIds.add(overpaymentCustomerId)
                }

                creditCustomerIds.forEach { cid ->
                    txn.get(customersCol.document(cid))
                }

                // ── 3. WRITE: reverse money rows ──
                payments.filter { !it.isCredit }.forEach { p ->
                    txn.update(
                        accountsCol.document(p.accountId),
                        "currentBalance", FieldValue.increment(-p.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type" to MoneyTransactionType.ADJUSTMENT.name,
                        "fromAccountId" to p.accountId,
                        "toAccountId" to "",
                        "amount" to p.amount,
                        "fee" to 0,
                        "feeType" to "NONE",
                        "netAmount" to p.amount,
                        "description" to "Sale delete — reversal",
                        "referenceId" to saleId,
                        "referenceType" to "SALE_DELETE_REVERSAL",
                        "externalAccountName" to "",
                        "externalAccountNumber" to "",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                // ── 4. WRITE: reverse credit rows (customer balance goes DOWN) ──
                payments.filter { it.isCredit }.forEach { p ->
                    txn.update(
                        customersCol.document(p.customerId),
                        "creditBalance", FieldValue.increment(-p.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(creditTxnsCol.document(), mapOf(
                        "customerId" to p.customerId,
                        "type" to "PAYMENT",
                        "amount" to -p.amount,
                        "saleId" to saleId,
                        "paymentAccountId" to "",
                        "description" to "Sale delete — credit reversal",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                // ── 5. WRITE: reverse overpayment credit (customer balance goes UP) ──
                if (overpaymentAmount > 0 && overpaymentCustomerId.isNotEmpty()) {
                    txn.update(
                        customersCol.document(overpaymentCustomerId),
                        "creditBalance", FieldValue.increment(overpaymentAmount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(creditTxnsCol.document(), mapOf(
                        "customerId" to overpaymentCustomerId,
                        "type" to "SALE_ON_CREDIT",
                        "amount" to overpaymentAmount,
                        "saleId" to saleId,
                        "paymentAccountId" to "",
                        "description" to "Sale delete — overpayment reversal",
                        "date" to now,
                        "createdAt" to now
                    ))
                }

                // ── 6. WRITE: delete sale doc ──
                txn.delete(saleRef)
            }.await()

            Log.d(TAG, "Sale deleted atomically: $saleId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteSale failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
