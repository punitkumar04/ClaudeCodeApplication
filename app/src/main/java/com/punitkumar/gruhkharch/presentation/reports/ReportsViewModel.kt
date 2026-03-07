package com.punitkumar.gruhkharch.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.Expense
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import com.punitkumar.gruhkharch.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportsState(
    val totalSpent: Double = 0.0,
    val budget: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val stageBreakdown: Map<String, Double> = emptyMap(),
    val memberBreakdown: Map<String, Double> = emptyMap(),
    val paymentModeBreakdown: Map<String, Double> = emptyMap(),
    val monthlyTrend: Map<String, Double> = emptyMap(),
    val topVendors: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val allExpenses: List<Expense> = emptyList(),
    val hasProject: Boolean = true
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val currentProjectHolder: CurrentProjectHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state.asStateFlow()

    private var reportsJob: Job? = null

    init {
        observeProjectChanges()
    }

    private fun observeProjectChanges() {
        viewModelScope.launch {
            currentProjectHolder.projectId.collect { projectId ->
                reportsJob?.cancel()
                if (projectId == null) {
                    _state.value = ReportsState(hasProject = false, isLoading = false)
                } else {
                    _state.value = ReportsState()
                    reportsJob = launch { loadReports(projectId) }
                }
            }
        }
    }

    private suspend fun loadReports(projectId: String) {
        val project = projectRepository.getProject(projectId)
        if (project == null) {
            _state.update { it.copy(hasProject = false, isLoading = false) }
            return
        }

        _state.update { it.copy(budget = project.budget) }

        expenseRepository.getExpenses(project.id).collect { expenses ->
            val total = expenses.sumOf { it.amount }

            val categoryMap = expenses.groupBy { it.category }
                .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }.toMap()

            val stageMap = expenses.groupBy { it.stage }
                .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }.toMap()

            val memberMap = expenses.groupBy { it.paidBy.name }
                .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }.toMap()

            val paymentMap = expenses.groupBy { it.paymentMode.displayName }
                .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }.toMap()

            val monthlyMap = expenses.groupBy { DateUtils.formatMonthYear(it.date) }
                .mapValues { (_, exps) -> exps.sumOf { it.amount } }

            val vendorMap = expenses.filter { !it.vendor.isNullOrBlank() }
                .groupBy { it.vendor!! }
                .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                .toList().sortedByDescending { it.second }.take(10).toMap()

            _state.update {
                it.copy(
                    totalSpent = total,
                    categoryBreakdown = categoryMap,
                    stageBreakdown = stageMap,
                    memberBreakdown = memberMap,
                    paymentModeBreakdown = paymentMap,
                    monthlyTrend = monthlyMap,
                    topVendors = vendorMap,
                    isLoading = false,
                    allExpenses = expenses
                )
            }
        }
    }

    fun generateCsvContent(): String {
        val expenses = _state.value.allExpenses
        val sb = StringBuilder()
        sb.appendLine("Title,Amount,Date,Paid By,Payment Mode,Category,Sub-Category,Stage,Vendor,Notes,Tags")
        expenses.forEach { e ->
            sb.appendLine(
                "\"${e.title}\",${e.amount},\"${DateUtils.formatDate(e.date)}\",\"${e.paidBy.name}\"," +
                "\"${e.paymentMode.displayName}\",\"${e.category}\",\"${e.subCategory ?: ""}\",\"${e.stage}\"," +
                "\"${e.vendor ?: ""}\",\"${e.notes ?: ""}\",\"${e.tags.joinToString(";")}\""
            )
        }
        return sb.toString()
    }
}
