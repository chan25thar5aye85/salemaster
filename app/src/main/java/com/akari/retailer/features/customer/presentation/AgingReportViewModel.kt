package com.akari.retailer.features.customer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.features.customer.data.repository.CreditRepository
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.usecases.CalculateAgingReportUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AgingReportViewModel(
    private val customerRepository: CustomerRepository,
    private val creditRepository: CreditRepository,
    private val calculateAgingReport: CalculateAgingReportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AgingReportState())
    val state: StateFlow<AgingReportState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
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
                combine(
                    customerRepository.getCustomers(),
                    creditRepository.getTransactions()
                ) { customers, transactions ->
                    calculateAgingReport.invoke(customers, transactions)
                }.collect { report ->
                    _state.value = _state.value.copy(
                        report = report,
                        isLoading = false,
                        error = null
                    )
                    applyFilter()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load aging report"
                )
            }
        }
    }

    private fun applyFilter() {
        val report = _state.value.report ?: return
        val filter = _state.value.filter

        val filtered = if (filter == AgingFilter.ALL) {
            report.customers
        } else {
            val bucket = filter.toBucket()
            report.customers.filter { customer ->
                (customer.bucketTotals[bucket] ?: 0) > 0
            }
        }

        _state.value = _state.value.copy(filteredCustomers = filtered)
    }
}

class AgingReportViewModelFactory(
    private val customerRepository: CustomerRepository,
    private val creditRepository: CreditRepository,
    private val calculateAgingReport: CalculateAgingReportUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AgingReportViewModel::class.java)) {
            return AgingReportViewModel(customerRepository, creditRepository, calculateAgingReport) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
