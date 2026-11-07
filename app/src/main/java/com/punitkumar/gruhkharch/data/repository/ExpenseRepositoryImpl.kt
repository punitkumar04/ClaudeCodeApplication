package com.punitkumar.gruhkharch.data.repository

import com.punitkumar.gruhkharch.data.local.dao.ExpenseDao
import com.punitkumar.gruhkharch.data.remote.FirebaseStorageSource
import com.punitkumar.gruhkharch.data.remote.FirestoreExpenseSource
import com.punitkumar.gruhkharch.domain.model.Expense
import com.punitkumar.gruhkharch.domain.model.Member
import com.punitkumar.gruhkharch.domain.model.PaymentMode
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.util.toEntity
import com.punitkumar.gruhkharch.util.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.util.Log
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ExpenseRepository"

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val firestoreExpenseSource: FirestoreExpenseSource,
    private val storageSource: FirebaseStorageSource
) : ExpenseRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getExpenses(projectId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentExpenses(projectId: String, limit: Int): Flow<List<Expense>> {
        return expenseDao.getRecentExpenses(projectId, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseById(id: String): Expense? {
        return expenseDao.getExpenseById(id)?.toDomain()
    }

    override fun getTotalSpent(projectId: String): Flow<Double> {
        return expenseDao.getTotalSpent(projectId).map { it ?: 0.0 }
    }

    override fun getTotalByCategory(projectId: String, category: String): Flow<Double> {
        return expenseDao.getTotalByCategory(projectId, category).map { it ?: 0.0 }
    }

    override fun getTotalByStage(projectId: String, stage: String): Flow<Double> {
        return expenseDao.getTotalByStage(projectId, stage).map { it ?: 0.0 }
    }

    override fun getTotalByMember(projectId: String, userId: String): Flow<Double> {
        return expenseDao.getTotalByMember(projectId, userId).map { it ?: 0.0 }
    }

    override fun getTotalInDateRange(projectId: String, startDate: Long, endDate: Long): Flow<Double> {
        return expenseDao.getTotalInDateRange(projectId, startDate, endDate).map { it ?: 0.0 }
    }

    override suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val id = if (expense.id.isBlank()) UUID.randomUUID().toString() else expense.id
            val newExpense = expense.copy(id = id, updatedAt = System.currentTimeMillis(), isSynced = false)
            expenseDao.insertExpense(newExpense.toEntity())
            pushToRemote(newExpense)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            val updated = expense.copy(updatedAt = System.currentTimeMillis(), isSynced = false)
            expenseDao.updateExpense(updated.toEntity())
            pushToRemote(updated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(expense: Expense): Result<Unit> {
        return try {
            expenseDao.deleteExpenseById(expense.id)
            syncScope.launch {
                try {
                    firestoreExpenseSource.deleteExpense(expense.projectId, expense.id)
                    expense.receiptUrl?.let { storageSource.deleteReceipt(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete expense from Firestore", e)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Runs outside the caller's scope so saving never waits on the network.
    private fun pushToRemote(expense: Expense) {
        syncScope.launch {
            try {
                val acknowledged = firestoreExpenseSource.setExpense(
                    expense.projectId, expense.id, expenseToMap(expense)
                )
                if (acknowledged) expenseDao.markAsSynced(expense.id, expense.updatedAt)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync expense ${expense.id}, will retry later", e)
            }
        }
    }

    override fun searchExpenses(projectId: String, query: String): Flow<List<Expense>> {
        return expenseDao.searchExpenses(projectId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncExpenses(projectId: String) {
        try {
            expenseDao.getUnsyncedExpenses(projectId).forEach { pushToRemote(it.toDomain()) }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for project $projectId", e)
        }
    }

    override suspend fun pullRemoteExpenses(projectId: String) {
        try {
            syncExpenses(projectId)
            val remote = firestoreExpenseSource.getAllExpenses(projectId)

            // Local edits not yet on the server win over the remote copy.
            val unsyncedIds = expenseDao.getUnsyncedExpenses(projectId).map { it.id }.toSet()
            val remoteExpenses = remote.expenses
                .mapNotNull { mapToExpense(it, projectId) }
                .filter { it.id !in unsyncedIds }
            expenseDao.insertExpenses(remoteExpenses.map { it.toEntity() })

            // Remove expenses other members deleted. A cache-only result may be incomplete, so skip then.
            if (!remote.isFromCache) {
                val remoteIds = remote.expenses.mapNotNull { it["id"] as? String }.toSet()
                val deletedRemotely = expenseDao.getSyncedExpenseIds(projectId).filter { it !in remoteIds }
                deletedRemotely.chunked(500).forEach { expenseDao.deleteExpensesByIds(it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pull remote expenses for $projectId", e)
        }
    }

    override fun observeRemoteExpenses(projectId: String): Flow<List<Expense>> {
        return firestoreExpenseSource.observeExpenses(projectId).map { list ->
            list.mapNotNull { map -> mapToExpense(map, projectId) }
        }
    }

    private fun expenseToMap(expense: Expense): Map<String, Any?> = mapOf(
        "title" to expense.title,
        "amount" to expense.amount,
        "date" to expense.date,
        "paidBy" to mapOf("userId" to expense.paidBy.userId, "name" to expense.paidBy.name),
        "paymentMode" to expense.paymentMode.name,
        "transactionRef" to expense.transactionRef,
        "category" to expense.category,
        "subCategory" to expense.subCategory,
        "stage" to expense.stage,
        "vendor" to expense.vendor,
        "notes" to expense.notes,
        "receiptUrl" to expense.receiptUrl,
        "tags" to expense.tags,
        "createdBy" to expense.createdBy,
        "createdAt" to expense.createdAt,
        "updatedAt" to expense.updatedAt
    )

    private fun mapToExpense(map: Map<String, Any?>, projectId: String): Expense? {
        return try {
            val paidByMap = map["paidBy"] as? Map<*, *>
            Expense(
                id = map["id"] as? String ?: return null,
                title = map["title"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                paidBy = Member(
                    userId = paidByMap?.get("userId") as? String ?: "",
                    name = paidByMap?.get("name") as? String ?: ""
                ),
                paymentMode = try { PaymentMode.valueOf(map["paymentMode"] as? String ?: "") } catch (e: Exception) { PaymentMode.OTHER },
                transactionRef = map["transactionRef"] as? String,
                category = map["category"] as? String ?: "",
                subCategory = map["subCategory"] as? String,
                stage = map["stage"] as? String ?: "",
                vendor = map["vendor"] as? String,
                notes = map["notes"] as? String,
                receiptUrl = map["receiptUrl"] as? String,
                tags = (map["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdBy = map["createdBy"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                projectId = projectId,
                isSynced = true
            )
        } catch (e: Exception) {
            null
        }
    }
}
