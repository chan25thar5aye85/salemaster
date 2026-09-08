package com.akari.retailer.features.expense.domain.usecases

import com.akari.retailer.core.usecases.NoParamUseCase
import com.akari.retailer.features.expense.data.repository.CategoryRepository

class SeedDefaultCategoriesUseCase(
    private val repository: CategoryRepository
) : NoParamUseCase<Result<Unit>> {
    
    override suspend fun invoke(): Result<Unit> {
        return repository.seedDefaultCategories()
    }
}
