package com.akari.retailer.features.money.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.money.domain.models.DefaultMoneyAccounts
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.features.money.domain.models.MoneyAccountType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreMoneyService {
    
    private val TAG = "FirestoreMoney"
    private val db: FirebaseFirestore?
    private val COLLECTION = "money_accounts"
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection(COLLECTION)
    
    suspend fun addAccount(account: MoneyAccount): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = collection.document()
            val data = mapOf(
                "name" to account.name,
                "type" to account.type.name,
                "icon" to account.icon,
                "color" to account.color,
                "openingBalance" to account.openingBalance,
                "currentBalance" to account.currentBalance,
                "accountNumber" to account.accountNumber,
                "notes" to account.notes,
                "isDefault" to account.isDefault,
                "isActive" to account.isActive,
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data).await()
            Log.d(TAG, "✅ Account added: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Add account error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateAccount(account: MoneyAccount): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null || account.id.isEmpty()) {
                return Result.failure(Exception("Invalid account or Firestore not available"))
            }
            
            val data = mapOf(
                "name" to account.name,
                "type" to account.type.name,
                "icon" to account.icon,
                "color" to account.color,
                "openingBalance" to account.openingBalance,
                "currentBalance" to account.currentBalance,
                "accountNumber" to account.accountNumber,
                "notes" to account.notes,
                "isActive" to account.isActive,
                "updatedAt" to System.currentTimeMillis()
            )
            collection.document(account.id).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update account error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteAccount(accountId: String): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val account = getAccountByIdSync(accountId)
            if (account?.isDefault == true) {
                return Result.failure(Exception("Cannot delete default account"))
            }
            
            collection.document(accountId).delete().await()
            Log.d(TAG, "✅ Account deleted: $accountId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete account error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getAccounts(): Flow<List<MoneyAccount>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val accounts = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    MoneyAccount(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        type = try {
                            MoneyAccountType.valueOf(data["type"] as? String ?: "CASH")
                        } catch (e: Exception) {
                            MoneyAccountType.CASH
                        },
                        icon = data["icon"] as? String ?: "💵",
                        color = data["color"] as? String ?: "#4CAF50",
                        openingBalance = (data["openingBalance"] as? Number)?.toInt() ?: 0,
                        currentBalance = (data["currentBalance"] as? Number)?.toInt() ?: 0,
                        accountNumber = data["accountNumber"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        isDefault = data["isDefault"] as? Boolean ?: false,
                        isActive = data["isActive"] as? Boolean ?: true,
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(accounts)
            }
        
        awaitClose { listener.remove() }
    }
    
    fun getAccountById(accountId: String): Flow<MoneyAccount?> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val listener = collection.document(accountId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val data = snapshot.data ?: run {
                    trySend(null)
                    return@addSnapshotListener
                }
                
                val account = MoneyAccount(
                    id = snapshot.id,
                    name = data["name"] as? String ?: "",
                    type = try {
                        MoneyAccountType.valueOf(data["type"] as? String ?: "CASH")
                    } catch (e: Exception) {
                        MoneyAccountType.CASH
                    },
                    icon = data["icon"] as? String ?: "💵",
                    color = data["color"] as? String ?: "#4CAF50",
                    openingBalance = (data["openingBalance"] as? Number)?.toInt() ?: 0,
                    currentBalance = (data["currentBalance"] as? Number)?.toInt() ?: 0,
                    accountNumber = data["accountNumber"] as? String ?: "",
                    notes = data["notes"] as? String ?: "",
                    isDefault = data["isDefault"] as? Boolean ?: false,
                    isActive = data["isActive"] as? Boolean ?: true,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                trySend(account)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getAccountByIdSync(accountId: String): MoneyAccount? {
        return try {
            val collection = getCollection() ?: return null
            val snapshot = collection.document(accountId).get().await()
            if (!snapshot.exists()) return null
            val data = snapshot.data ?: return null
            MoneyAccount(
                id = snapshot.id,
                name = data["name"] as? String ?: "",
                type = try {
                    MoneyAccountType.valueOf(data["type"] as? String ?: "CASH")
                } catch (e: Exception) {
                    MoneyAccountType.CASH
                },
                icon = data["icon"] as? String ?: "💵",
                color = data["color"] as? String ?: "#4CAF50",
                openingBalance = (data["openingBalance"] as? Number)?.toInt() ?: 0,
                currentBalance = (data["currentBalance"] as? Number)?.toInt() ?: 0,
                accountNumber = data["accountNumber"] as? String ?: "",
                notes = data["notes"] as? String ?: "",
                isDefault = data["isDefault"] as? Boolean ?: false,
                isActive = data["isActive"] as? Boolean ?: true,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get account sync error: ${e.message}")
            null
        }
    }
    
    suspend fun updateBalance(accountId: String, newBalance: Int): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            collection.document(accountId).update(
                mapOf(
                    "currentBalance" to newBalance,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Update balance error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun adjustBalance(accountId: String, amount: Int): Result<Unit> {
        return try {
            val account = getAccountByIdSync(accountId)
                ?: return Result.failure(Exception("Account not found"))
            val newBalance = account.currentBalance + amount
            updateBalance(accountId, newBalance)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Adjust balance error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun seedDefaultAccounts(): Result<Unit> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val existing = collection.limit(1).get().await()
            if (!existing.isEmpty) {
                Log.d(TAG, "Money accounts already exist, skipping seed")
                return Result.success(Unit)
            }
            
            DefaultMoneyAccounts.list.forEach { account ->
                val docRef = collection.document(account.id)
                val data = mapOf(
                    "name" to account.name,
                    "type" to account.type.name,
                    "icon" to account.icon,
                    "color" to account.color,
                    "openingBalance" to 0,
                    "currentBalance" to 0,
                    "accountNumber" to "",
                    "notes" to "",
                    "isDefault" to true,
                    "isActive" to true,
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
                docRef.set(data).await()
                Log.d(TAG, "✅ Seeded account: ${account.name}")
            }
            
            Log.d(TAG, "✅ All default money accounts seeded")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Seed accounts error: ${e.message}")
            Result.failure(e)
        }
    }
}
