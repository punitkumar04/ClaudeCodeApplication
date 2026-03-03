package com.punitkumar.gruhkharch.data.local.dao

import androidx.room.*
import com.punitkumar.gruhkharch.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY date DESC")
    fun getExpensesByProject(projectId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId ORDER BY date DESC LIMIT :limit")
    fun getRecentExpenses(projectId: String, limit: Int = 10): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: String): ExpenseEntity?

    @Query("SELECT SUM(amount) FROM expenses WHERE projectId = :projectId")
    fun getTotalSpent(projectId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE projectId = :projectId AND category = :category")
    fun getTotalByCategory(projectId: String, category: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE projectId = :projectId AND stage = :stage")
    fun getTotalByStage(projectId: String, stage: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE projectId = :projectId AND paidByUserId = :userId")
    fun getTotalByMember(projectId: String, userId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE projectId = :projectId AND date >= :startDate AND date <= :endDate")
    fun getTotalInDateRange(projectId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT DISTINCT category FROM expenses WHERE projectId = :projectId")
    fun getUsedCategories(projectId: String): Flow<List<String>>

    @Query("SELECT DISTINCT vendor FROM expenses WHERE projectId = :projectId AND vendor IS NOT NULL AND vendor != ''")
    fun getUsedVendors(projectId: String): Flow<List<String>>

    @Query("SELECT * FROM expenses WHERE projectId = :projectId AND isSynced = 0")
    suspend fun getUnsyncedExpenses(projectId: String): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("DELETE FROM expenses WHERE projectId = :projectId")
    suspend fun deleteAllExpensesForProject(projectId: String)

    @Query("UPDATE expenses SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("SELECT * FROM expenses WHERE projectId = :projectId AND title LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchExpenses(projectId: String, query: String): Flow<List<ExpenseEntity>>
}
