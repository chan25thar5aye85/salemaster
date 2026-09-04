package com.akari.retailer.features.sales.domain.usecases

import com.akari.retailer.core.usecases.UseCase

class CalculateTotalUseCase : UseCase<List<Int>, Int> {
    
    override suspend fun invoke(params: List<Int>): Int {
        return params.sum()
    }
}
