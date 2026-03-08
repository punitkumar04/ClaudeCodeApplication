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
import kotlinx.coroutines.Job
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

    private var dashboardJob: Job? = null

    init {
        observeProjectChanges()
    }

    private fun observeProjectChanges() {
        viewModelScope.launch {
            currentProjectHolder.projectId.collect { projectId ->
                dashboardJob?.cancel()
                if (projectId == null) {
                    _state.value = HomeState(hasProject = false, isLoading = false)
                } else {
                    _state.value = HomeState()
                    dashboardJob = launch { loadDashboard(projectId) }
                }
            }
        }
    }

    private suspend fun loadDashboard(projectId: String) {
        val project = projectRepository.getProject(projectId)
        if (project == null) {
            _state.update { it.copy(hasProject = false, isLoading = false, error = "Project not found") }
            return
        }

        _state.update {
            it.copy(
                projectName = project.name,
                budget = project.budget,
                currentStage = project.currentStage,
                error = null
            )
        }

        // Observe total spent
        viewModelScope.launch {
            expenseRepository.getTotalSpent(project.id).collect { total ->
                _state.update { it.copy(totalSpent = total) }
            }
        }

        // Observe this month spending
        val now = System.currentTimeMillis()
        val monthStart = DateUtils.getStartOfMonth(now)
        val monthEnd = DateUtils.getEndOfMonth(now)
        viewModelScope.launch {
            expenseRepository.getTotalInDateRange(project.id, monthStart, monthEnd).collect { total ->
                _state.update { it.copy(thisMonthSpent = total) }
            }
        }

        // Last month
        val lastMonthEnd = monthStart - 1
        val lastMonthStart = DateUtils.getStartOfMonth(lastMonthEnd)
        viewModelScope.launch {
            expenseRepository.getTotalInDateRange(project.id, lastMonthStart, lastMonthEnd).collect { total ->
                _state.update { it.copy(lastMonthSpent = total) }
            }
        }

        // Recent expenses
        viewModelScope.launch {
            expenseRepository.getRecentExpenses(project.id, 10).collect { expenses ->
                _state.update { it.copy(recentExpenses = expenses, isLoading = false) }
            }
        }

        // Category breakdown
        viewModelScope.launch {
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

    fun refresh() {
        val projectId = currentProjectHolder.projectId.value ?: return
        dashboardJob?.cancel()
        _state.update { it.copy(isLoading = true) }
        dashboardJob = viewModelScope.launch { loadDashboard(projectId) }
    }
}
