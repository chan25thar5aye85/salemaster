package com.akari.retailer.features.money.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.akari.retailer.features.money.domain.models.FeeType
import com.akari.retailer.features.money.domain.models.MoneyTransaction
import com.akari.retailer.features.money.domain.models.MoneyTransactionType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreMoneyTransactionService {
    
    private val TAG = "FirestoreMoneyTx"
    private val db: FirebaseFirestore?
    private val COLLECTION = "money_transactions"
    
    init {
        db = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance: ${e.message}")
            null
        }
    }
    
    private fun getCollection() = db?.collection(COLLECTION)
    
    suspend fun addTransaction(transaction: MoneyTransaction): Result<String> {
        return try {
            val collection = getCollection()
            if (collection == null) {
                return Result.failure(Exception("Firestore not available"))
            }
            
            val docRef = collection.document()
            val data = mapOf(
                "type" to transaction.type.name,
                "fromAccountId" to transaction.fromAccountId,
                "toAccountId" to transaction.toAccountId,
                "amount" to transaction.amount,
                "fee" to transaction.fee,
                "feeType" to transaction.feeType.name,
                "netAmount" to transaction.netAmount,
                "description" to transaction.description,
                "referenceId" to transaction.referenceId,
                "referenceType" to transaction.referenceType,
                "externalAccountName" to transaction.externalAccountName,
                "externalAccountNumber" to transaction.externalAccountNumber,
                "date" to transaction.date,
                "createdAt" to System.currentTimeMillis()
            )
            
            docRef.set(data).await()
            Log.d(TAG, "✅ Transaction added: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Add transaction error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun getTransactions(): Flow<List<MoneyTransaction>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val transactions = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    MoneyTransaction(
                        id = doc.id,
                        type = try {
                            MoneyTransactionType.valueOf(data["type"] as? String ?: "ADJUSTMENT")
                        } catch (e: Exception) {
                            MoneyTransactionType.ADJUSTMENT
                        },
                        fromAccountId = data["fromAccountId"] as? String ?: "",
                        toAccountId = data["toAccountId"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toInt() ?: 0,
                        fee = (data["fee"] as? Number)?.toInt() ?: 0,
                        feeType = try {
                            FeeType.valueOf(data["feeType"] as? String ?: "NONE")
                        } catch (e: Exception) {
                            FeeType.NONE
                        },
                        netAmount = (data["netAmount"] as? Number)?.toInt() ?: 0,
                        description = data["description"] as? String ?: "",
                        referenceId = data["referenceId"] as? String ?: "",
                        referenceType = data["referenceType"] as? String ?: "",
                        externalAccountName = data["externalAccountName"] as? String ?: "",
                        externalAccountNumber = data["externalAccountNumber"] as? String ?: "",
                        date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(transactions)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * External transfers only. Filters client-side to avoid a composite
     * index on (type, date). Fine for small-to-medium datasets.
     */
    fun getExternalTransfers(): Flow<List<MoneyTransaction>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = collection
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                if (snapshot == null) { trySend(emptyList()); return@addSnapshotListener }

                val external = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val typeStr = data["type"] as? String ?: return@mapNotNull null
                    if (typeStr != "EXTERNAL_OUT" && typeStr != "EXTERNAL_IN") {
                        return@mapNotNull null
                    }
                    MoneyTransaction(
                        id = doc.id,
                        type = MoneyTransactionType.valueOf(typeStr),
                        fromAccountId = data["fromAccountId"] as? String ?: "",
                        toAccountId = data["toAccountId"] as? String ?: "",
                        amount = (data["amount"] as? Number)?.toInt() ?: 0,
                        fee = (data["fee"] as? Number)?.toInt() ?: 0,
                        feeType = try {
                            FeeType.valueOf(data["feeType"] as? String ?: "NONE")
                        } catch (e: Exception) { FeeType.NONE },
                        netAmount = (data["netAmount"] as? Number)?.toInt() ?: 0,
                        description = data["description"] as? String ?: "",
                        referenceId = data["referenceId"] as? String ?: "",
                        referenceType = data["referenceType"] as? String ?: "",
                        externalAccountName = data["externalAccountName"] as? String ?: "",
                        externalAccountNumber = data["externalAccountNumber"] as? String ?: "",
                        date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
                trySend(external)
            }

        awaitClose { listener.remove() }
    }


    fun getTransactionsForAccount(accountId: String): Flow<List<MoneyTransaction>> = callbackFlow {
        val collection = getCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val transactions = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val fromId = data["fromAccountId"] as? String ?: ""
                    val toId = data["toAccountId"] as? String ?: ""
                    
                    if (fromId == accountId || toId == accountId) {
                        MoneyTransaction(
                            id = doc.id,
                            type = try {
                                MoneyTransactionType.valueOf(data["type"] as? String ?: "ADJUSTMENT")
                            } catch (e: Exception) {
                                MoneyTransactionType.ADJUSTMENT
                            },
                            fromAccountId = fromId,
                            toAccountId = toId,
                            amount = (data["amount"] as? Number)?.toInt() ?: 0,
                            fee = (data["fee"] as? Number)?.toInt() ?: 0,
                            feeType = try {
                                FeeType.valueOf(data["feeType"] as? String ?: "NONE")
                            } catch (e: Exception) {
                                FeeType.NONE
                            },
                            netAmount = (data["netAmount"] as? Number)?.toInt() ?: 0,
                            description = data["description"] as? String ?: "",
                            referenceId = data["referenceId"] as? String ?: "",
                            referenceType = data["referenceType"] as? String ?: "",
                            externalAccountName = data["externalAccountName"] as? String ?: "",
                            externalAccountNumber = data["externalAccountNumber"] as? String ?: "",
                            date = (data["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    } else null
                }.sortedByDescending { it.date }
                trySend(transactions)
            }
        
        awaitClose { listener.remove() }
    }
}
