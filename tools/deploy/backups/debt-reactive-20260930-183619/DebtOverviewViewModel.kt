package com.akari.retailer.features.debt.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CreditRepository
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.AgingBucket
import com.akari.retailer.features.customer.domain.models.CreditTransactionType
import com.akari.retailer.features.customer.domain.usecases.AgingInput
import com.akari.retailer.features.customer.domain.usecases.CalculateAgingReportUseCase
import com.akari.retailer.features.customer.domain.usecases.CreditLine
import com.akari.retailer.features.customer.domain.usecases.PaymentLine
import com.akari.retailer.features.supplier.data.repository.SupplierCreditRepository
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.SupplierTransactionType
import com.akari.retailer.features.supplier.domain.usecases.RecordSupplierPaymentUseCase
import com.akari.retailer.features.customer.domain.usecases.ExtendCreditUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DebtOverviewViewModel(
    private val customerRepository: CustomerRepository,
    private val creditRepository: CreditRepository,
    private val supplierRepository: SupplierRepository,
    private val supplierCreditRepository: SupplierCreditRepository,
    private val calculateAgingReport: CalculateAgingReportUseCase,
    private val extendCreditUseCase: ExtendCreditUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DebtOverviewState())
    val state: StateFlow<DebtOverviewState> = _state.asStateFlow()

    init {
        load()
    }

    fun setTab(tab: DebtTab) {
        _state.value = _state.value.copy(tab = tab)
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val recv = loadReceivables()
                val pay = loadPayables()
                _state.value = _state.value.copy(
                    isLoading = false,
                    receivableTotal = recv.totalOwed,
                    receivablePartyCount = recv.totalPartiesOwing,
                    receivableBuckets = recv.bucketTotals,
                    receivableParties = recv.parties.map {
                        DebtParty(
                            partyId = it.partyId,
                            partyName = it.partyName,
                            amount = it.totalOwed,
                            oldestDays = it.oldestDays
                        )
                    },
                    payableTotal = pay.totalOwed,
                    payablePartyCount = pay.totalPartiesOwing,
                    payableBuckets = pay.bucketTotals,
                    payableParties = pay.parties.map {
                        DebtParty(
                            partyId = it.partyId,
                            partyName = it.partyName,
                            amount = it.totalOwed,
                            oldestDays = it.oldestDays
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load debt"
                )
            }
        }
    }

    private suspend fun loadReceivables(): com.akari.retailer.features.customer.domain.models.AgingReport {
        val customers = customerRepository.getCustomers().firstOrNull() ?: emptyList()
        val transactions = creditRepository.getTransactions().firstOrNull() ?: emptyList()
        val inputs = customers.mapNotNull { customer ->
            val txns = transactions.filter { it.customerId == customer.id }
            if (txns.isEmpty()) return@mapNotNull null
            val credits = txns.filter { it.type == CreditTransactionType.SALE_ON_CREDIT }
                .map { CreditLine(it.id, it.amount, it.date) }
            val payments = txns.filter {
                it.type == CreditTransactionType.PAYMENT || it.type == CreditTransactionType.REFUND
            }.map { PaymentLine(-it.amount, it.date) }
            AgingInput(customer.id, customer.name, credits, payments)
        }
        return calculateAgingReport.invoke(inputs)
    }

    private suspend fun loadPayables(): com.akari.retailer.features.customer.domain.models.AgingReport {
        val suppliers = supplierRepository.getSuppliers().firstOrNull() ?: emptyList()
        val transactions = supplierCreditRepository.getTransactions().firstOrNull() ?: emptyList()
        val inputs = suppliers.mapNotNull { supplier ->
            val txns = transactions.filter { it.supplierId == supplier.id }
            if (txns.isEmpty()) return@mapNotNull null
            val credits = txns.filter { it.type == SupplierTransactionType.PURCHASE_ON_CREDIT }
                .map { CreditLine(it.id, it.amount, it.date) }
            val payments = txns.filter {
                it.type == SupplierTransactionType.PAYMENT ||
                it.type == SupplierTransactionType.REFUND_RECEIVED
            }.map {
                val abs = if (it.amount < 0) -it.amount else it.amount
                PaymentLine(abs, it.date)
            }
            AgingInput(supplier.id, supplier.name, credits, payments)
        }
        return calculateAgingReport.invoke(inputs)
    }

    // ── Add-debt dialog ──
    fun openAddDialog(customerId: String = "", customerName: String = "",
                      supplierId: String = "", supplierName: String = "") {
        _state.value = _state.value.copy(
            showAddDialog = true,
            addAmount = "",
            addNote = "",
            selectedCustomerId = customerId,
            selectedCustomerName = customerName,
            selectedSupplierId = supplierId,
            selectedSupplierName = supplierName,
            addError = null,
            addSuccess = false
        )
    }

    fun closeAddDialog() {
        _state.value = _state.value.copy(
            showAddDialog = false,
            addAmount = "",
            addNote = "",
            addError = null
        )
    }

    fun updateAmount(v: String) { _state.value = _state.value.copy(addAmount = v.filter { it.isDigit() }) }
    fun updateNote(v: String)   { _state.value = _state.value.copy(addNote = v) }
    fun selectCustomer(id: String, name: String) {
        _state.value = _state.value.copy(selectedCustomerId = id, selectedCustomerName = name)
    }
    fun selectSupplier(id: String, name: String) {
        _state.value = _state.value.copy(selectedSupplierId = id, selectedSupplierName = name)
    }

    fun save() {
        val s = _state.value
        val amount = s.addAmount.toIntOrNull() ?: 0
        if (amount <= 0) {
            _state.value = s.copy(addError = "Enter a valid amount"); return
        }
        val isReceivable = s.tab == DebtTab.RECEIVABLES
        if (isReceivable && s.selectedCustomerId.isEmpty()) {
            _state.value = s.copy(addError = "Select a customer"); return
        }
        if (!isReceivable && s.selectedSupplierId.isEmpty()) {
            _state.value = s.copy(addError = "Select a supplier"); return
        }
        _state.value = s.copy(isSaving = true, addError = null)
        viewModelScope.launch {
            val result = if (isReceivable) {
                extendCreditUseCase.invoke(
                    customerId = s.selectedCustomerId,
                    amount = amount,
                    saleId = "",
                    description = s.addNote.trim().ifEmpty { "Manual debt" }
                )
            } else {
                supplierCreditRepository.recordPurchaseOnCredit(
                    supplierId = s.selectedSupplierId,
                    amount = amount,
                    purchaseId = "",
                    description = s.addNote.trim().ifEmpty { "Manual debt" }
                )
            }
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    addSuccess = true,
                    showAddDialog = false
                )
                load()
            } else {
                _state.value = _state.value.copy(
                    isSaving = false,
                    addError = result.exceptionOrNull()?.message ?: "Failed to save"
                )
            }
        }
    }

    fun clearAddSuccess() { _state.value = _state.value.copy(addSuccess = false) }
}

class DebtOverviewViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val creditRepository: CreditRepository,
    private val supplierRepository: SupplierRepository,
    private val supplierCreditRepository: SupplierCreditRepository,
    private val calculateAgingReport: CalculateAgingReportUseCase,
    private val extendCreditUseCase: ExtendCreditUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DebtOverviewViewModel::class.java)) {
            return DebtOverviewViewModel(
                customerRepository, creditRepository,
                supplierRepository, supplierCreditRepository,
                calculateAgingReport, extendCreditUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
