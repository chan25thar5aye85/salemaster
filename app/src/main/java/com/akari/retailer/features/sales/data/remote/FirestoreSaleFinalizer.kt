package com.akari.retailer.features.sales.data.remote

import android.util.Log
import com.akari.retailer.features.money.domain.models.CreditAccount
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
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
                    "cashierId" to sale.cashierId
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
                    val custRef = customersCol.document(overpaymentCredit.customerId)
                    val custSnap = txn.get(custRef)
                    if (!custSnap.exists()) {
                        throw IllegalStateException(
                            "Customer not found: ${overpaymentCredit.customerId}"
                        )
                    }

                    // Customer credit balance goes DOWN (we owe them)
                    txn.update(
                        custRef,
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
}
