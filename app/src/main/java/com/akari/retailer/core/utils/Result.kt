package com.akari.retailer.core.utils

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading
    
    fun getOrNull(): T? = (this as? Success)?.data
    
    fun exceptionOrNull(): Throwable? = (this as? Error)?.throwable
}
