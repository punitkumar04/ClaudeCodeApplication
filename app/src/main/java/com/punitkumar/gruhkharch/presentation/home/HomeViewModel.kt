package com.punitkumar.gruhkharch.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.*
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import com.punitkumar.gruhkharch.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val projectName: String = "",
    val totalSpent: Double = 0.0,
    val budget: Double = 0.0,
    val currentStage: String = "",
    val thisMonthSpent: Double = 0.0,
    val lastMonthSpent: Double = 0.0,
    val recentExpenses: List<Expense> = emptyList(),
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val stageBreakdown: Map<String, Double> = emptyMap(),
    val memberBreakdown: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasProject: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val currentProjectHolder: CurrentProjectHolder
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            val projectId = currentProjectHolder.projectId.value
            if (projectId == null) {
                _state.update { it.copy(hasProject = false, isLoading = false) }
                return@launch
            }
            val project = projectRepository.getProject(projectId)
            if (project == null) {
                _state.update { it.copy(hasProject = false, isLoading = false) }
                return@launch
            }

            _state.update {
                it.copy(
                    projectName = project.name,
                    budget = project.budget,
                    currentStage = project.currentStage
                )
            }

            // Observe total spent
            launch {
                expenseRepository.getTotalSpent(project.id).collect { total ->
                    _state.update { it.copy(totalSpent = total) }
                }
            }

            // Observe this month spending
            val now = System.currentTimeMillis()
            val monthStart = DateUtils.getStartOfMonth(now)
            val monthEnd = DateUtils.getEndOfMonth(now)
            launch {
                expenseRepository.getTotalInDateRange(project.id, monthStart, monthEnd).collect { total ->
                    _state.update { it.copy(thisMonthSpent = total) }
                }
            }

            // Last month
            val lastMonthEnd = monthStart - 1
            val lastMonthStart = DateUtils.getStartOfMonth(lastMonthEnd)
            launch {
                expenseRepository.getTotalInDateRange(project.id, lastMonthStart, lastMonthEnd).collect { total ->
                    _state.update { it.copy(lastMonthSpent = total) }
                }
            }

            // Recent expenses
            launch {
                expenseRepository.getRecentExpenses(project.id, 10).collect { expenses ->
                    _state.update { it.copy(recentExpenses = expenses, isLoading = false) }
                }
            }

            // Category breakdown
            launch {
                expenseRepository.getExpenses(project.id).collect { expenses ->
                    val categoryMap = expenses.groupBy { it.category }
                        .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                        .toMap()

                    val stageMap = expenses.groupBy { it.stage }
                        .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                        .toMap()

                    val memberMap = expenses.groupBy { it.paidBy.name }
                        .mapValues { (_, exps) -> exps.sumOf { it.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                        .toMap()

                    _state.update {
                        it.copy(
                            categoryBreakdown = categoryMap,
                            stageBreakdown = stageMap,
                            memberBreakdown = memberMap
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        loadDashboard()
    }
}
