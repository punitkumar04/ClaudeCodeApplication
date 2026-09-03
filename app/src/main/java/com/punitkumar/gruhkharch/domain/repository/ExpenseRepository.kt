package com.punitkumar.gruhkharch.domain.repository

import com.punitkumar.gruhkharch.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpenses(projectId: String): Flow<List<Expense>>
    fun getRecentExpenses(projectId: String, limit: Int = 10): Flow<List<Expense>>
    suspend fun getExpenseById(id: String): Expense?
    fun getTotalSpent(projectId: String): Flow<Double>
    fun getTotalByCategory(projectId: String, category: String): Flow<Double>
    fun getTotalByStage(projectId: String, stage: String): Flow<Double>
    fun getTotalByMember(projectId: String, userId: String): Flow<Double>
    fun getTotalInDateRange(projectId: String, startDate: Long, endDate: Long): Flow<Double>
    suspend fun addExpense(expense: Expense): Result<String>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expense: Expense): Result<Unit>
    fun searchExpenses(projectId: String, query: String): Flow<List<Expense>>
    suspend fun syncExpenses(projectId: String)
    suspend fun pullRemoteExpenses(projectId: String)
    fun observeRemoteExpenses(projectId: String): Flow<List<Expense>>
}
