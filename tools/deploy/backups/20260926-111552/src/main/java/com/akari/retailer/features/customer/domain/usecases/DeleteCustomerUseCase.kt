package com.akari.retailer.features.customer.domain.usecases

import com.akari.retailer.core.usecases.UseCase
import com.akari.retailer.features.customer.data.repository.CustomerRepository

class DeleteCustomerUseCase(
    private val repository: CustomerRepository
) : UseCase<String, Result<Unit>> {
    
    override suspend fun invoke(params: String): Result<Unit> {
        return try {
            if (params.isEmpty()) {
                Result.failure(Exception("Customer ID cannot be empty"))
            } else {
                repository.deleteCustomer(params)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
