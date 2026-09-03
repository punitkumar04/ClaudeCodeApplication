package com.punitkumar.gruhkharch.data.repository

import android.net.Uri
import com.punitkumar.gruhkharch.data.local.dao.ExpenseDao
import com.punitkumar.gruhkharch.data.remote.FirebaseStorageSource
import com.punitkumar.gruhkharch.data.remote.FirestoreExpenseSource
import com.punitkumar.gruhkharch.domain.model.Expense
import com.punitkumar.gruhkharch.domain.model.Member
import com.punitkumar.gruhkharch.domain.model.PaymentMode
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.util.toEntity
import com.punitkumar.gruhkharch.util.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
            val newExpense = expense.copy(id = id, updatedAt = System.currentTimeMillis())

            // Save locally first
            expenseDao.insertExpense(newExpense.toEntity())

            // Sync to Firestore
            try {
                val data = expenseToMap(newExpense)
                firestoreExpenseSource.addExpense(newExpense.projectId, id, data)
                expenseDao.markAsSynced(id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync new expense to Firestore, will retry later", e)
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            val updated = expense.copy(updatedAt = System.currentTimeMillis())
            expenseDao.updateExpense(updated.toEntity())

            try {
                firestoreExpenseSource.updateExpense(
                    expense.projectId, expense.id, expenseToMap(updated)
                )
                expenseDao.markAsSynced(expense.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync expense update to Firestore, will retry later", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(expense: Expense): Result<Unit> {
        return try {
            expenseDao.deleteExpenseById(expense.id)

            try {
                firestoreExpenseSource.deleteExpense(expense.projectId, expense.id)
                expense.receiptUrl?.let { storageSource.deleteReceipt(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete expense from Firestore", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun searchExpenses(projectId: String, query: String): Flow<List<Expense>> {
        return expenseDao.searchExpenses(projectId, query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun syncExpenses(projectId: String) {
        try {
            val unsynced = expenseDao.getUnsyncedExpenses(projectId)
            unsynced.forEach { entity ->
                try {
                    val data = expenseToMap(entity.toDomain())
                    firestoreExpenseSource.updateExpense(projectId, entity.id, data)
                    expenseDao.markAsSynced(entity.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync expense ${entity.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed for project $projectId", e)
        }
    }

    override suspend fun pullRemoteExpenses(projectId: String) {
        try {
            val remoteMaps = firestoreExpenseSource.getAllExpenses(projectId)
            val remoteExpenses = remoteMaps.mapNotNull { mapToExpense(it, projectId) }
            remoteExpenses.forEach { expense ->
                expenseDao.insertExpense(expense.toEntity())
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
