package com.akari.retailer.features.sales.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akari.retailer.data.repository.SaleRepository
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.Customer
import com.akari.retailer.features.money.data.repository.MoneyAccountRepository
import com.akari.retailer.features.money.domain.models.CreditAccount
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.sales.domain.models.Sale
import com.akari.retailer.features.sales.data.repository.SaleFinalizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SaleDetailState(
    val sale: Sale? = null,
    val accounts: List<MoneyAccount> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val deleteSuccess: Boolean = false
)

class SaleDetailViewModel(
    private val saleRepository: SaleRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val customerRepository: CustomerRepository,
    private val saleFinalizer: SaleFinalizer,
    private val saleId: String
) : ViewModel() {

    private val _state = MutableStateFlow(SaleDetailState())
    val state: StateFlow<SaleDetailState> = _state.asStateFlow()

    private var loadSaleJob: Job? = null
    private var loadAccountsJob: Job? = null
    private var loadCustomersJob: Job? = null

    init {
        loadSale()
        loadAccounts()
        loadCustomers()
    }

    fun deleteSale() {
        viewModelScope.launch {
            val result = saleFinalizer.deleteSale(saleId)
            _state.value = _state.value.copy(deleteSuccess = result.isSuccess)
        }
    }

    private fun loadSale() {
        loadSaleJob?.cancel()
        loadSaleJob = viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                saleRepository.getSaleById(saleId).collect { sale ->
                    _state.value = _state.value.copy(
                        sale = sale,
                        isLoading = false,
                        error = if (sale == null) "Sale not found" else null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load sale"
                )
            }
        }
    }

    private fun loadAccounts() {
        loadAccountsJob?.cancel()
        loadAccountsJob = viewModelScope.launch {
            try {
                moneyAccountRepository.getAccounts().collect { accounts ->
                    _state.value = _state.value.copy(accounts = accounts)
                }
            } catch (e: Exception) { }
        }
    }

    private fun loadCustomers() {
        loadCustomersJob?.cancel()
        loadCustomersJob = viewModelScope.launch {
            try {
                customerRepository.getCustomers().collect { customers ->
                    _state.value = _state.value.copy(customers = customers)
                }
            } catch (e: Exception) { }
        }
    }

    /**
     * Human-readable label for a payment row.
     * For credit: "💳 Credit (Aung)"
     * For money: "💵 Cash"
     */
    fun paymentLabel(accountId: String, customerId: String): String {
        if (accountId == CreditAccount.ID) {
            val customer = _state.value.customers.find { it.id == customerId }
            return "💳 ${CreditAccount.DISPLAY_NAME}" + (customer?.let { " (${it.name})" } ?: "")
        }
        val account = _state.value.accounts.find { it.id == accountId }
        return account?.getDisplayName() ?: accountId
    }
}

class SaleDetailViewModelFactory(
    private val saleRepository: SaleRepository,
    private val moneyAccountRepository: MoneyAccountRepository,
    private val customerRepository: CustomerRepository,
    private val saleFinalizer: SaleFinalizer,
    private val saleId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleDetailViewModel::class.java)) {
            return SaleDetailViewModel(saleRepository, moneyAccountRepository, customerRepository, saleFinalizer, saleId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
