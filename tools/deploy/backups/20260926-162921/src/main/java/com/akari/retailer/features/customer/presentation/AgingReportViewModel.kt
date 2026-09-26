package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CreditRepository
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.AgingReport
import com.akari.retailer.features.customer.domain.models.CreditTransaction
import com.akari.retailer.features.customer.domain.models.CreditTransactionType
import com.akari.retailer.features.customer.domain.usecases.AgingInput
import com.akari.retailer.features.customer.domain.usecases.CalculateAgingReportUseCase
import com.akari.retailer.features.customer.domain.usecases.CreditLine
import com.akari.retailer.features.customer.domain.usecases.PaymentLine
import com.akari.retailer.features.supplier.data.repository.SupplierCreditRepository
import com.akari.retailer.features.supplier.data.repository.SupplierRepository
import com.akari.retailer.features.supplier.domain.models.SupplierTransaction
import com.akari.retailer.features.supplier.domain.models.SupplierTransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

enum class AgingMode {
    RECEIVABLES,
    PAYABLES
}

enum class AgingFilter {
    ALL, CURRENT, DAYS_30, DAYS_60, DAYS_90;

    fun toBucket(): com.akari.retailer.features.customer.domain.models.AgingBucket? =
        when (this) {
            ALL -> null
            CURRENT -> com.akari.retailer.features.customer.domain.models.AgingBucket.CURRENT
            DAYS_30 -> com.akari.retailer.features.customer.domain.models.AgingBucket.DAYS_30
            DAYS_60 -> com.akari.retailer.features.customer.domain.models.AgingBucket.DAYS_60
            DAYS_90 -> com.akari.retailer.features.customer.domain.models.AgingBucket.DAYS_90
        }
}

class AgingReportViewModel(
    private val customerRepository: CustomerRepository,
    private val creditRepository: CreditRepository,
    private val supplierRepository: SupplierRepository,
    private val supplierCreditRepository: SupplierCreditRepository,
    private val calculateAgingReport: CalculateAgingReportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AgingReportState())
    val state: StateFlow<AgingReportState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadReport()
    }

    fun setMode(mode: AgingMode) {
        _state.value = _state.value.copy(mode = mode)
        loadReport()
    }

    fun setFilter(filter: AgingFilter) {
        _state.value = _state.value.copy(filter = filter)
        applyFilter()
    }

    fun refresh() {
        loadReport()
    }

    private fun loadReport() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val report = when (_state.value.mode) {
                    AgingMode.RECEIVABLES -> loadReceivables()
                    AgingMode.PAYABLES -> loadPayables()
                }
                _state.value = _state.value.copy(
                    report = report,
                    isLoading = false,
                    error = null
                )
                applyFilter()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load aging report"
                )
            }
        }
    }

    private suspend fun loadReceivables(): AgingReport {
        // one-shot reads — do NOT use collect() on a hot flow, it never completes
        val customers = customerRepository.getCustomers().firstOrNull() ?: emptyList()
        val transactions = creditRepository.getTransactions().firstOrNull() ?: emptyList()

        val inputs = customers.mapNotNull { customer ->
            val txns = transactions.filter { it.customerId == customer.id }
            if (txns.isEmpty()) return@mapNotNull null

            val credits = txns
                .filter { it.type == CreditTransactionType.SALE_ON_CREDIT }
                .map { CreditLine(it.id, it.amount, it.date) }
            // Both PAYMENT and REFUND reduce the customer's debt (FIFO).
            // Payments have negative amount in Firestore; refunds are also
            // stored as negative. So both map to a positive PaymentLine.
            val payments = txns
                .filter {
                    it.type == CreditTransactionType.PAYMENT ||
                    it.type == CreditTransactionType.REFUND
                }
                .map { PaymentLine(-it.amount, it.date) }

            AgingInput(
                partyId = customer.id,
                partyName = customer.name,
                credits = credits,
                payments = payments
            )
        }
        return calculateAgingReport.invoke(inputs)
    }

    private suspend fun loadPayables(): AgingReport {
        // one-shot reads — do NOT use collect() on a hot flow, it never completes
        val suppliers = supplierRepository.getSuppliers().firstOrNull() ?: emptyList()
        val transactions = supplierCreditRepository.getTransactions().firstOrNull() ?: emptyList()

        val inputs = suppliers.mapNotNull { supplier ->
            val txns = transactions.filter { it.supplierId == supplier.id }
            if (txns.isEmpty()) return@mapNotNull null

            val credits = txns
                .filter { it.type == SupplierTransactionType.PURCHASE_ON_CREDIT }
                .map { CreditLine(it.id, it.amount, it.date) }
            // PAYMENT and REFUND_RECEIVED both reduce what we owe the supplier.
            // PAYMENT amounts are negative; REFUND_RECEIVED amounts are positive.
            // We want a positive "applied to queue" value either way.
            val payments = txns
                .filter {
                    it.type == SupplierTransactionType.PAYMENT ||
                    it.type == SupplierTransactionType.REFUND_RECEIVED
                }
                .map {
                    val absAmount = if (it.amount < 0) -it.amount else it.amount
                    PaymentLine(absAmount, it.date)
                }

            AgingInput(
                partyId = supplier.id,
                partyName = supplier.name,
                credits = credits,
                payments = payments
            )
        }
        return calculateAgingReport.invoke(inputs)
    }

    private fun applyFilter() {
        val report = _state.value.report ?: return
        val filter = _state.value.filter

        val filtered = if (filter == AgingFilter.ALL) {
            report.parties
        } else {
            val bucket = filter.toBucket()
            report.parties.filter { party ->
                (party.bucketTotals[bucket] ?: 0) > 0
            }
        }

        _state.value = _state.value.copy(filteredParties = filtered)
    }
}

class AgingReportViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val creditRepository: CreditRepository,
    private val supplierRepository: SupplierRepository,
    private val supplierCreditRepository: SupplierCreditRepository,
    private val calculateAgingReport: CalculateAgingReportUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgingReportViewModel::class.java)) {
            return AgingReportViewModel(
                customerRepository,
                creditRepository,
                supplierRepository,
                supplierCreditRepository,
                calculateAgingReport
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
