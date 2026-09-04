package com.akari.retailer.core.usecases

interface UseCase<in P, R> {
    suspend operator fun invoke(params: P): R
}

interface NoParamUseCase<R> {
    suspend operator fun invoke(): R
}
