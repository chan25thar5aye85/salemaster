package com.akari.retailer.features.inventory.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.core.ui.components.PaymentRow
import com.akari.retailer.core.utils.PaymentPreferences
import com.akari.retailer.features.expense.data.repository.FirestoreExpenseRepository
import com.akari.retailer.features.expense.data.remote.FirestoreExpenseService
import com.akari.retailer.features.expense.domain.models.Expense
import com.akari.retailer.features.inventory.data.repository.FirestoreInventoryRepository
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseOrderRepository
import com.akari.retailer.features.inventory.data.repository.PurchaseFinalizer
import com.akari.retailer.features.inventory.data.repository.FirestorePurchaseRepository
import com.akari.retailer.features.inventory.data.remote.FirestoreInventoryService
import com.akari.retailer.features.inventory.domain.models.Purchase
import com.akari.retailer.features.inventory.domain.models.PurchaseItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrder
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderItem
import com.akari.retailer.features.inventory.domain.models.PurchaseOrderStatus
import com.akari.retailer.features.money.data.remote.FirestoreMoneyService
import com.akari.retailer.features.money.data.remote.FirestoreMoneyTransactionService
import com.akari.retailer.features.money.data.repository.FirestoreMoneyAccountRepository
import com.akari.retailer.features.money.data.repository.FirestoreMoneyTransactionRepository
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.PaymentEntry
import com.akari.retailer.features.supplier.data.repository.FirestoreSupplierRepository
import com.akari.retailer.features.supplier.data.remote.FirestoreSupplierService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PurchaseOrderDetailState(
    val order: PurchaseOrder? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isUpdating: Boolean = false,
    val editableOrderItems: List<PurchaseOrderItem> = emptyList(),
    val accounts: List<MoneyAccount> = emptyList(),
    val paymentRows: List<PaymentRow> = listOf(
        PaymentRow(id = 1L, accountId = "default_cash", amount = "")
    ),
    val userTouchedAmounts: Boolean = false
) {
    fun getTotalCost(): Int = order?.receivedItems?.sumOf { it.total } ?: 0
    fun getTotalPaid(): Int = paymentRows.sumOf { it.amount.toIntOrNull() ?: 0 }
    fun getRemaining(): Int = getTotalCost() - getTotalPaid()
    fun isFullyPaid(): Boolean = getTotalPaid() == getTotalCost() && getTotalCost() > 0

    fun getPayments(): List<PaymentEntry> {
        return paymentRows
            .filter { it.amount.toIntOrNull()?.let { it > 0 } == true }
            .map { PaymentEntry(it.accountId, it.amount.toIntOrNull() ?: 0) }
    }
}

class PurchaseOrderDetailViewModel(
    private val repository: PurchaseOrderRepository,
    private val purchaseFinalizer: PurchaseFinalizer,
    private val paymentPreferences: PaymentPreferences
) : ViewModel() {

    private val TAG = "PurchaseOrderDetailVM"
    private val _state = MutableStateFlow(PurchaseOrderDetailState())
    val state: StateFlow<PurchaseOrderDetailState> = _state.asStateFlow()

    private var nextPaymentRowId = 2L

    private var loadOrderJob: Job? = null
    private var loadAccountsJob: Job? = null

    private val moneyAccountRepository by lazy {
        FirestoreMoneyAccountRepository(FirestoreMoneyService())
    }
    private val transactionRepo by lazy {
        FirestoreMoneyTransactionRepository(FirestoreMoneyTransactionService())
    }

    init {
        val lastAccountId = paymentPreferences.getLastUsedAccountId()
        _state.value = _state.value.copy(
            paymentRows = listOf(PaymentRow(id = 1L, accountId = lastAccountId, amount = ""))
        )
    }

    fun loadOrder(orderId: String) {
        loadOrderJob?.cancel()
        loadOrderJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                repository.getOrder(orderId).collect { loadedOrder ->
                    val totalCost = loadedOrder?.receivedItems?.sumOf { it.total } ?: 0

                    val updatedPayments = if (_state.value.paymentRows.size == 1 &&
                        !_state.value.userTouchedAmounts && totalCost > 0) {
                        listOf(_state.value.paymentRows[0].copy(amount = totalCost.toString()))
                    } else {
                        _state.value.paymentRows
                    }

                    _state.value = _state.value.copy(
                        order = loadedOrder,
                        isLoading = false,
                        editableOrderItems = loadedOrder?.orderItems ?: emptyList(),
                        paymentRows = updatedPayments,
                        error = if (loadedOrder == null) "Order not found" else null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load order"
                )
            }
        }
    }

    fun loadAccounts() {
        loadAccountsJob?.cancel()
        loadAccountsJob = viewModelScope.launch {
            try {
                moneyAccountRepository.getAccounts().collect { accounts ->
                    _state.value = _state.value.copy(accounts = accounts.filter { it.isActive })
                }
            } catch (e: Exception) { }
        }
    }

    fun updatePaymentAccount(rowId: Long, account: MoneyAccount) {
        val updated = _state.value.paymentRows.map { row ->
            if (row.id == rowId) row.copy(accountId = account.id) else row
        }
        _state.value = _state.value.copy(paymentRows = updated)
        paymentPreferences.setLastUsedAccountId(account.id)
    }

    fun updatePaymentAmount(rowId: Long, amount: String) {
        val digitsOnly = amount.filter { it.isDigit() }
        val updated = _state.value.paymentRows.map { row ->
            if (row.id == rowId) row.copy(amount = digitsOnly) else row
        }
        _state.value = _state.value.copy(
            paymentRows = updated,
            userTouchedAmounts = true
        )
    }

    fun addPaymentRow() {
        val remaining = _state.value.getRemaining()
        val lastAccountId = _state.value.paymentRows.lastOrNull()?.accountId ?: "default_cash"
        val newRow = PaymentRow(
            id = nextPaymentRowId++,
            accountId = lastAccountId,
            amount = if (remaining > 0) remaining.toString() else ""
        )
        _state.value = _state.value.copy(paymentRows = _state.value.paymentRows + newRow)
    }

    fun removePaymentRow(rowId: Long) {
        if (_state.value.paymentRows.size <= 1) return
        val previousTotal = _state.value.getTotalCost()
        _state.value = _state.value.copy(
            paymentRows = _state.value.paymentRows.filter { it.id != rowId }
        )
        syncSinglePaymentRow(previousTotal)
    }

    /**
     * If there's exactly one payment row, keep it in sync with the total —
     * but ONLY if the row still contains the previously auto-filled value.
     */
    private fun syncSinglePaymentRow(previousTotal: Int) {
        val rows = _state.value.paymentRows
        if (rows.size != 1) return

        val currentRowAmount = rows[0].amount.toIntOrNull() ?: 0
        val wasAutoFilled = rows[0].amount.isEmpty() ||
                            currentRowAmount == previousTotal

        if (wasAutoFilled) {
            val newTotal = _state.value.getTotalCost()
            if (newTotal > 0) {
                _state.value = _state.value.copy(
                    paymentRows = listOf(rows[0].copy(amount = newTotal.toString())),
                    userTouchedAmounts = false
                )
            }
        }
    }

    fun updateOrderItems(items: List<PurchaseOrderItem>, orderId: String) {
        viewModelScope.launch {
            val currentOrder = _state.value.order ?: return@launch
            val updatedOrder = currentOrder.copy(
                orderItems = items,
                orderTotal = items.sumOf { it.total },
                updatedAt = System.currentTimeMillis()
            )
            repository.updateOrder(updatedOrder)
            _state.value = _state.value.copy(editableOrderItems = items)
        }
    }

    fun receiveSelectedItems(orderId: String, selectedItems: List<PurchaseOrderItem>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)
            val currentOrder = _state.value.order ?: return@launch

            val currentReceivedItems = currentOrder.receivedItems.toMutableList()
            selectedItems.forEach { item ->
                if (!currentReceivedItems.any { it.productId == item.productId }) {
                    currentReceivedItems.add(item)
                }
            }

            val updatedOrder = currentOrder.copy(
                receivedItems = currentReceivedItems,
                receivedTotal = currentReceivedItems.sumOf { it.total },
                receivedDate = if (currentReceivedItems.isNotEmpty()) System.currentTimeMillis() else currentOrder.receivedDate,
                updatedAt = System.currentTimeMillis()
            )

            repository.updateOrder(updatedOrder)
            loadOrder(orderId)
            _state.value = _state.value.copy(isUpdating = false)
        }
    }

    suspend fun createPurchase(orderId: String): Result<Unit> {
        val currentOrder = _state.value.order
            ?: return Result.failure(Exception("Order not found"))

        if (currentOrder.status == PurchaseOrderStatus.COMPLETED) {
            return Result.failure(Exception("Purchase already created"))
        }
        if (currentOrder.receivedItems.isEmpty()) {
            return Result.failure(Exception("No items to purchase"))
        }

        val payments = _state.value.getPayments()
        val totalCost = currentOrder.receivedItems.sumOf { it.total }
        val totalPaid = payments.sumOf { it.amount }

        if (totalPaid > totalCost) {
            return Result.failure(
                Exception("Payments ($totalPaid) cannot exceed total ($totalCost)")
            )
        }

        val receiptNumber = generateReceiptNumber()

        // Atomic cascade via PurchaseFinalizer (purchase + stock + expense +
        // money + supplier + payable + order status). Idempotent on order id.
        val result = purchaseFinalizer.finalizePurchase(
            order = currentOrder,
            payments = payments,
            receiptNumber = receiptNumber
        )

        if (result.isSuccess) {
            loadOrder(orderId)
        }
        return result.map { }
    }


    fun updateStatus(orderId: String, newStatus: PurchaseOrderStatus) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)
            val result = repository.updateStatus(orderId, newStatus)
            if (result.isSuccess) loadOrder(orderId)
            _state.value = _state.value.copy(isUpdating = false)
        }
    }

    suspend fun deleteOrder(orderId: String): Result<Unit> {
        return repository.deleteOrder(orderId)
    }

    private fun generateReceiptNumber(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "RCP-$date-$random"
    }
}

class PurchaseOrderDetailViewModelFactory(
    private val repository: PurchaseOrderRepository,
    private val purchaseFinalizer: PurchaseFinalizer,
    private val paymentPreferences: PaymentPreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseOrderDetailViewModel::class.java)) {
            return PurchaseOrderDetailViewModel(repository, purchaseFinalizer, paymentPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
