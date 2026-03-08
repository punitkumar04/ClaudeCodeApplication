package com.punitkumar.gruhkharch.domain.usecase

import com.punitkumar.gruhkharch.domain.model.Expense
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class DashboardData(
    val totalSpent: Double = 0.0,
    val thisMonthSpent: Double = 0.0,
    val lastMonthSpent: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val stageBreakdown: Map<String, Double> = emptyMap(),
    val memberBreakdown: Map<String, Double> = emptyMap(),
    val paymentModeBreakdown: Map<String, Double> = emptyMap(),
    val monthlyTrend: Map<String, Double> = emptyMap(),
    val recentExpenses: List<Expense> = emptyList(),
    val topVendors: List<Pair<String, Double>> = emptyList()
)

class GetDashboardDataUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(projectId: String): Flow<DashboardData> {
        return expenseRepository.getExpenses(projectId).map { expenses ->
            val now = System.currentTimeMillis()
            val thisMonthStart = DateUtils.getStartOfMonth(now)
            val thisMonthEnd = DateUtils.getEndOfMonth(now)

            val lastMonthCal = java.util.Calendar.getInstance()
            lastMonthCal.add(java.util.Calendar.MONTH, -1)
            val lastMonthStart = DateUtils.getStartOfMonth(lastMonthCal.timeInMillis)
            val lastMonthEnd = DateUtils.getEndOfMonth(lastMonthCal.timeInMillis)

            val thisMonthExpenses = expenses.filter { it.date in thisMonthStart..thisMonthEnd }
            val lastMonthExpenses = expenses.filter { it.date in lastMonthStart..lastMonthEnd }

            val categoryBreakdown = expenses.groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
                .associate { it.key to it.value }

            val stageBreakdown = expenses.groupBy { it.stage }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val memberBreakdown = expenses.groupBy { it.paidBy.name }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val paymentModeBreakdown = expenses.groupBy { it.paymentMode.displayName }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val monthlyTrend = expenses.groupBy { DateUtils.formatMonthYear(it.date) }
                .mapValues { (_, list) -> list.sumOf { it.amount } }

            val topVendors = expenses.filter { !it.vendor.isNullOrBlank() }
                .groupBy { it.vendor.orEmpty() }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
                .take(10)
                .map { it.key to it.value }

            DashboardData(
                totalSpent = expenses.sumOf { it.amount },
                thisMonthSpent = thisMonthExpenses.sumOf { it.amount },
                lastMonthSpent = lastMonthExpenses.sumOf { it.amount },
                categoryBreakdown = categoryBreakdown,
                stageBreakdown = stageBreakdown,
                memberBreakdown = memberBreakdown,
                paymentModeBreakdown = paymentModeBreakdown,
                monthlyTrend = monthlyTrend,
                recentExpenses = expenses.sortedByDescending { it.date }.take(10),
                topVendors = topVendors
            )
        }
    }
}
