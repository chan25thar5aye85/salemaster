package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.customer.data.repository.CustomerRepository
import com.akari.retailer.features.customer.domain.models.Customer

class AddCustomerUseCase(
    private val repository: CustomerRepository
) : UseCase<Customer, Result<String>> {
    
    override suspend fun invoke(params: Customer): Result<String> {
        return try {
            if (params.name.isBlank()) {
                Result.failure(Exception("Customer name is required"))
            } else {
                repository.addCustomer(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
