package com.akari.retailer.features.money.data.repository

import com.akari.retailer.features.money.domain.models.MoneyAccount
import kotlinx.coroutines.flow.Flow

interface MoneyAccountRepository {
    suspend fun addAccount(account: MoneyAccount): Result<String>
    suspend fun updateAccount(account: MoneyAccount): Result<Unit>
    suspend fun deleteAccount(accountId: String): Result<Unit>
    fun getAccounts(): Flow<List<MoneyAccount>>
    fun getAccountById(accountId: String): Flow<MoneyAccount?>
    suspend fun getAccountByIdSync(accountId: String): MoneyAccount?
    suspend fun updateBalance(accountId: String, newBalance: Int): Result<Unit>
    suspend fun adjustBalance(accountId: String, amount: Int): Result<Unit>
    suspend fun seedDefaultAccounts(): Result<Unit>
}
