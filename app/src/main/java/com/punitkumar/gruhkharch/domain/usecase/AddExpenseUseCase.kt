package com.punitkumar.gruhkharch.domain.usecase

import com.punitkumar.gruhkharch.domain.model.Expense
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<String> {
        if (expense.title.isBlank()) return Result.failure(Exception("Title is required"))
        if (expense.amount <= 0) return Result.failure(Exception("Amount must be greater than 0"))
        if (expense.category.isBlank()) return Result.failure(Exception("Category is required"))
        if (expense.stage.isBlank()) return Result.failure(Exception("Construction stage is required"))
        return expenseRepository.addExpense(expense)
    }
}
