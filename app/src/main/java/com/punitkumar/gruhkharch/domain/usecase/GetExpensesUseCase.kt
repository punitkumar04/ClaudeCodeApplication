package com.punitkumar.gruhkharch.domain.usecase

import com.punitkumar.gruhkharch.domain.model.Expense
import com.punitkumar.gruhkharch.domain.model.ExpenseFilter
import com.punitkumar.gruhkharch.domain.model.SortBy
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(projectId: String, filter: ExpenseFilter = ExpenseFilter()): Flow<List<Expense>> {
        val baseFlow = if (!filter.searchQuery.isNullOrBlank()) {
            expenseRepository.searchExpenses(projectId, filter.searchQuery)
        } else {
            expenseRepository.getExpenses(projectId)
        }

        return baseFlow.map { expenses ->
            var filtered = expenses

            // Apply filters
            filter.dateRange?.let { range ->
                filtered = filtered.filter { it.date in range.startDate..range.endDate }
            }
            if (filter.paidByUserIds.isNotEmpty()) {
                filtered = filtered.filter { it.paidBy.userId in filter.paidByUserIds }
            }
            if (filter.categories.isNotEmpty()) {
                filtered = filtered.filter { it.category in filter.categories }
            }
            if (filter.subCategories.isNotEmpty()) {
                filtered = filtered.filter { it.subCategory in filter.subCategories }
            }
            if (filter.stages.isNotEmpty()) {
                filtered = filtered.filter { it.stage in filter.stages }
            }
            if (filter.paymentModes.isNotEmpty()) {
                filtered = filtered.filter { it.paymentMode in filter.paymentModes }
            }
            filter.amountMin?.let { min ->
                filtered = filtered.filter { it.amount >= min }
            }
            filter.amountMax?.let { max ->
                filtered = filtered.filter { it.amount <= max }
            }
            filter.vendor?.let { vendor ->
                filtered = filtered.filter { it.vendor?.contains(vendor, ignoreCase = true) == true }
            }
            if (filter.tags.isNotEmpty()) {
                filtered = filtered.filter { expense -> expense.tags.any { it in filter.tags } }
            }

            // Apply sorting
            when (filter.sortBy) {
                SortBy.DATE_DESC -> filtered.sortedByDescending { it.date }
                SortBy.DATE_ASC -> filtered.sortedBy { it.date }
                SortBy.AMOUNT_DESC -> filtered.sortedByDescending { it.amount }
                SortBy.AMOUNT_ASC -> filtered.sortedBy { it.amount }
                SortBy.CATEGORY_ASC -> filtered.sortedBy { it.category }
            }
        }
    }
}
