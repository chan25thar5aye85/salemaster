package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.core.usecases.NoParamUseCase
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.Customer
import kotlinx.coroutines.flow.Flow

class GetCustomersUseCase(
    private val repository: CustomerRepository
) : NoParamUseCase<Flow<List<Customer>>> {
    
    override suspend fun invoke(): Flow<List<Customer>> {
        return repository.getCustomers()
    }
}
