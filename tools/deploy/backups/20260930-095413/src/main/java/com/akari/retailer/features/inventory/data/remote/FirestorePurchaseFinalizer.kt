package com.akari.retailer.features.inventory.data.remote

import android.util.Log
import com.akari.retailer.features.inventory.data.repository.PurchaseFinalizer
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.supplier.domain.models.SupplierTransactionType
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestorePurchaseFinalizer(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PurchaseFinalizer {

    private val TAG = "PurchaseFinalizer"

    private val purchasesCol        get() = db.collection("purchases")
    private val ordersCol           get() = db.collection("purchase_orders")
    private val productsCol         get() = db.collection("products")
    private val stockMovementsCol   get() = db.collection("stock_movements")
    private val expensesCol         get() = db.collection("expenses")
    private val accountsCol         get() = db.collection("money_accounts")
    private val moneyTxnsCol        get() = db.collection("money_transactions")
    private val suppliersCol        get() = db.collection("suppliers")
    private val supplierTxnsCol     get() = db.collection("supplier_transactions")

    override suspend fun finalizePurchase(
        order: PurchaseOrder,
        payments: List<PaymentEntry>,
        receiptNumber: String
    ): Result<String> {
        return try {
            require(order.id.isNotEmpty()) { "Order id is required" }
            require(order.receivedItems.isNotEmpty()) { "No received items to purchase" }

            val totalCost = order.receivedItems.sumOf { it.total }
            val totalPaid = payments.sumOf { it.amount }
            require(totalPaid <= totalCost) {
                "Payments ($totalPaid) cannot exceed total ($totalCost)"
            }
            val creditAmount = totalCost - totalPaid

            val now = System.currentTimeMillis()
            val purchaseRef = purchasesCol.document(order.id)   // idempotent
            val orderRef = ordersCol.document(order.id)

            db.runTransaction { txn ->
                // ── 1. READ ──
                val orderSnap = txn.get(orderRef)
                if (orderSnap.exists() &&
                    orderSnap.getString("status") == PurchaseOrderStatus.COMPLETED.name
                ) {
                    throw IllegalStateException("Purchase already created for this order")
                }

                data class ProductSnap(val ref: DocumentReference, val stock: Int)
                val productSnaps = mutableMapOf<String, ProductSnap>()
                for (item in order.receivedItems) {
                    val pRef = productsCol.document(item.productId)
                    val pSnap = txn.get(pRef)
                    val stock = (pSnap.getLong("stockQuantity") ?: 0L).toInt()
                    productSnaps[item.productId] = ProductSnap(pRef, stock)
                }

                // ── 2. WRITE purchase doc ──
                txn.set(purchaseRef, mapOf(
                    "orderId"       to order.id,
                    "orderName"     to order.orderName,
                    "orderNumber"   to order.orderNumber,
                    "supplierId"    to order.supplierId,
                    "supplierName"  to order.supplierName,
                    "items"         to order.receivedItems.map { it.toMap() },
                    "totalCost"     to totalCost,
                    "paidAmount"    to totalPaid,
                    "creditAmount"  to creditAmount,
                    "payments"      to payments.map { it.toMap() },
                    "purchaseDate"  to now,
                    "notes"         to order.notes,
                    "receiptNumber" to receiptNumber,
                    "createdAt"     to now
                ))

                // ── 3. WRITE stock + movements ──
                for (item in order.receivedItems) {
                    val snap = productSnaps.getValue(item.productId)
                    txn.update(
                        snap.ref,
                        "stockQuantity", FieldValue.increment(item.quantity.toLong()),
                        "updatedAt", now
                    )
                    txn.set(stockMovementsCol.document(), mapOf(
                        "productId"       to item.productId,
                        "type"            to "PURCHASE",
                        "quantity"        to item.quantity,
                        "previousStock"   to snap.stock,
                        "newStock"        to (snap.stock + item.quantity),
                        "reason"          to "Purchase: ${order.orderName}",
                        "saleId"          to "",
                        "purchaseOrderId" to order.id,
                        "createdAt"       to now,
                        "userId"          to "default"
                    ))
                }

                // ── 4. WRITE expense ──
                txn.set(expensesCol.document(), mapOf(
                    "title"              to "Purchase Order: ${order.orderName}",
                    "amount"             to totalCost,
                    "categoryId"         to "default_inventory",
                    "type"               to "BUSINESS",
                    "businessPercentage" to 100,
                    "payments"           to payments.map { it.toMap() },
                    "description"        to "PO #${order.orderNumber} from ${order.supplierName}",
                    "date"               to now,
                    "createdAt"          to now,
                    "updatedAt"          to now
                ))

                // ── 5. WRITE money accounts + transactions ──
                for (p in payments) {
                    txn.update(
                        accountsCol.document(p.accountId),
                        "currentBalance", FieldValue.increment(-p.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type"                 to MoneyTransactionType.PURCHASE_OUT.name,
                        "fromAccountId"        to p.accountId,
                        "toAccountId"          to "",
                        "amount"               to p.amount,
                        "fee"                  to 0,
                        "feeType"              to "NONE",
                        "netAmount"            to p.amount,
                        "description"          to "PO #${order.orderNumber}",
                        "referenceId"          to order.id,
                        "referenceType"        to "PURCHASE",
                        "externalAccountName"  to "",
                        "externalAccountNumber" to "",
                        "date"                 to now,
                        "createdAt"            to now
                    ))
                }

                // ── 6. WRITE supplier totals + payable ──
                if (order.supplierId.isNotEmpty()) {
                    val supUpdates = mutableMapOf<String, Any>(
                        "totalPurchased" to FieldValue.increment(totalCost.toLong()),
                        "lastOrderDate"  to now,
                        "updatedAt"      to now
                    )
                    if (creditAmount > 0) {
                        supUpdates["payableBalance"] =
                            FieldValue.increment(creditAmount.toLong())
                    }
                    txn.update(suppliersCol.document(order.supplierId), supUpdates)

                    if (creditAmount > 0) {
                        txn.set(supplierTxnsCol.document(), mapOf(
                            "supplierId"       to order.supplierId,
                            "type"             to SupplierTransactionType.PURCHASE_ON_CREDIT.name,
                            "amount"           to creditAmount,
                            "purchaseId"       to order.id,
                            "paymentAccountId" to "",
                            "description"      to "Purchase: ${order.orderName}",
                            "date"             to now,
                            "createdAt"        to now
                        ))
                    }
                }

                // ── 7. WRITE order status → COMPLETED ──
                txn.update(
                    orderRef,
                    "status",        PurchaseOrderStatus.COMPLETED.name,
                    "completedDate", now,
                    "updatedAt",     now
                )
            }.await()

            Log.d(TAG, "Purchase finalized atomically: ${order.id}")
            Result.success(order.id)
        } catch (e: Exception) {
            Log.e(TAG, "finalizePurchase failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePurchase(
        purchaseId: String,
        orderId: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val purchaseRef = purchasesCol.document(purchaseId)
            val orderRef    = ordersCol.document(orderId)

            db.runTransaction { txn ->
                // ── 1. READ purchase ──
                val purchaseSnap = txn.get(purchaseRef)
                if (!purchaseSnap.exists()) return@runTransaction
                val purchase = purchaseSnap.data ?: return@runTransaction

                val items = (purchase["items"] as? List<*>)?.mapNotNull { row ->
                    (row as? Map<*, *>)?.let {
                        PurchaseItemDto(
                            productId = it["productId"] as? String ?: "",
                            quantity  = (it["quantity"] as? Number)?.toInt() ?: 0
                        )
                    }
                } ?: emptyList()

                val payments = (purchase["payments"] as? List<*>)?.mapNotNull { row ->
                    (row as? Map<*, *>)?.let {
                        PaymentEntry(
                            accountId = it["accountId"] as? String ?: "",
                            amount    = (it["amount"] as? Number)?.toInt() ?: 0
                        )
                    }
                } ?: emptyList()

                val supplierId   = purchase["supplierId"]   as? String ?: ""
                val totalCost    = (purchase["totalCost"]    as? Number)?.toInt() ?: 0
                val creditAmount = (purchase["creditAmount"] as? Number)?.toInt() ?: 0

                // ── 2. READ products ──
                val productStocks = mutableMapOf<String, Pair<DocumentReference, Int>>()
                for (item in items) {
                    val pRef = productsCol.document(item.productId)
                    val pSnap = txn.get(pRef)
                    val stock = (pSnap.getLong("stockQuantity") ?: 0L).toInt()
                    productStocks[item.productId] = pRef to stock
                }

                // ── 3. WRITE reverse stock + movements ──
                for (item in items) {
                    val (pRef, stock) = productStocks.getValue(item.productId)
                    txn.update(
                        pRef,
                        "stockQuantity", FieldValue.increment(-item.quantity.toLong()),
                        "updatedAt", now
                    )
                    txn.set(stockMovementsCol.document(), mapOf(
                        "productId"       to item.productId,
                        "type"            to "CANCEL",
                        "quantity"        to -item.quantity,
                        "previousStock"   to stock,
                        "newStock"        to (stock - item.quantity),
                        "reason"          to "Purchase deleted",
                        "saleId"          to "",
                        "purchaseOrderId" to orderId,
                        "createdAt"       to now,
                        "userId"          to "default"
                    ))
                }

                // ── 4. WRITE reverse money ──
                for (p in payments) {
                    txn.update(
                        accountsCol.document(p.accountId),
                        "currentBalance", FieldValue.increment(p.amount.toLong()),
                        "updatedAt", now
                    )
                    txn.set(moneyTxnsCol.document(), mapOf(
                        "type"                 to MoneyTransactionType.ADJUSTMENT.name,
                        "fromAccountId"        to "",
                        "toAccountId"          to p.accountId,
                        "amount"               to p.amount,
                        "fee"                  to 0,
                        "feeType"              to "NONE",
                        "netAmount"            to p.amount,
                        "description"          to "Purchase delete — reversal",
                        "referenceId"          to orderId,
                        "referenceType"        to "PURCHASE_DELETE_REVERSAL",
                        "externalAccountName"  to "",
                        "externalAccountNumber" to "",
                        "date"                 to now,
                        "createdAt"            to now
                    ))
                }

                // ── 5. WRITE reverse supplier ──
                if (supplierId.isNotEmpty()) {
                    val supUpdates = mutableMapOf<String, Any>(
                        "totalPurchased" to FieldValue.increment(-totalCost.toLong()),
                        "updatedAt"      to now
                    )
                    if (creditAmount > 0) {
                        supUpdates["payableBalance"] =
                            FieldValue.increment(-creditAmount.toLong())
                    }
                    txn.update(suppliersCol.document(supplierId), supUpdates)

                    if (creditAmount > 0) {
                        txn.set(supplierTxnsCol.document(), mapOf(
                            "supplierId"       to supplierId,
                            "type"             to SupplierTransactionType.PAYMENT.name,
                            "amount"           to -creditAmount,
                            "purchaseId"       to orderId,
                            "paymentAccountId" to "",
                            "description"      to "Purchase delete — payable reversal",
                            "date"             to now,
                            "createdAt"        to now
                        ))
                    }
                }

                // ── 6. WRITE order status → RECEIVED ──
                txn.update(
                    orderRef,
                    "status",        PurchaseOrderStatus.RECEIVED.name,
                    "completedDate", 0L,
                    "updatedAt",     now
                )

                // ── 7. WRITE delete purchase doc ──
                txn.delete(purchaseRef)
            }.await()

            Log.d(TAG, "Purchase deleted atomically: $purchaseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deletePurchase failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── helpers ──
    private data class PurchaseItemDto(val productId: String, val quantity: Int)

    private fun PurchaseOrderItem.toMap() = mapOf(
        "productId"   to productId,
        "productName" to productName,
        "quantity"    to quantity,
        "costPrice"   to costPrice,
        "total"       to total
    )

    private fun PaymentEntry.toMap() = mapOf(
        "accountId" to accountId,
        "amount"    to amount
    )
}
