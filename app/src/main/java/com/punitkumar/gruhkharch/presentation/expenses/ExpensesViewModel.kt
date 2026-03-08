package com.punitkumar.gruhkharch.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.*
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import com.punitkumar.gruhkharch.domain.usecase.GetExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpensesState(
    val expenses: List<Expense> = emptyList(),
    val groupedExpenses: Map<String, List<Expense>> = emptyMap(),
    val groupTotals: Map<String, Double> = emptyMap(),
    val filter: ExpenseFilter = ExpenseFilter(),
    val totalAmount: Double = 0.0,
    val totalCount: Int = 0,
    val members: List<Member> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val showFilters: Boolean = false,
    val currentUserId: String = "",
    val hasProject: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val currentProjectHolder: CurrentProjectHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ExpensesState())
    val state: StateFlow<ExpensesState> = _state.asStateFlow()

    private var projectId: String = ""
    private var dataJob: Job? = null

    init {
        observeProjectChanges()
    }

    private fun observeProjectChanges() {
        viewModelScope.launch {
            currentProjectHolder.projectId.collect { id ->
                dataJob?.cancel()
                if (id == null) {
                    _state.value = ExpensesState(hasProject = false, isLoading = false)
                } else {
                    _state.value = ExpensesState()
                    projectId = id
                    dataJob = launch { loadData() }
                }
            }
        }
    }

    private suspend fun loadData() {
        val userId = authRepository.currentUserId
        if (userId == null) {
            _state.update { it.copy(error = "Not signed in", isLoading = false) }
            return
        }
        val project = projectRepository.getProject(projectId)
        if (project == null) {
            _state.update { it.copy(error = "Project not found", hasProject = false, isLoading = false) }
            return
        }
        _state.update { it.copy(members = project.members, currentUserId = userId, error = null) }
        observeExpenses()
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            getExpensesUseCase(projectId, _state.value.filter).collect { expenses ->
                val grouped = groupExpenses(expenses, _state.value.filter.groupBy)
                val groupTotals = grouped.mapValues { (_, list) -> list.sumOf { it.amount } }
                _state.update {
                    it.copy(
                        expenses = expenses,
                        groupedExpenses = grouped,
                        groupTotals = groupTotals,
                        totalAmount = expenses.sumOf { e -> e.amount },
                        totalCount = expenses.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateSearch(query: String) {
        _state.update { it.copy(searchQuery = query) }
        val newFilter = _state.value.filter.copy(searchQuery = query.ifBlank { null })
        updateFilter(newFilter)
    }

    fun updateFilter(filter: ExpenseFilter) {
        _state.update { it.copy(filter = filter, isLoading = true) }
        observeExpenses()
    }

    fun toggleFilters() {
        _state.update { it.copy(showFilters = !it.showFilters) }
    }

    fun clearFilters() {
        updateFilter(ExpenseFilter())
        _state.update { it.copy(searchQuery = "") }
    }

    fun canDeleteExpense(expense: Expense): Boolean {
        return expense.createdBy == _state.value.currentUserId
    }

    fun deleteExpense(expense: Expense) {
        if (!canDeleteExpense(expense)) return
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
        }
    }

    private fun groupExpenses(expenses: List<Expense>, groupBy: GroupBy): Map<String, List<Expense>> {
        return when (groupBy) {
            GroupBy.NONE -> if (expenses.isEmpty()) emptyMap() else mapOf("All Expenses" to expenses)
            GroupBy.DATE -> expenses.groupBy { com.punitkumar.gruhkharch.util.DateUtils.getRelativeDateLabel(it.date) }
            GroupBy.CATEGORY -> expenses.groupBy { it.category }
            GroupBy.STAGE -> expenses.groupBy { it.stage }
            GroupBy.PAID_BY -> expenses.groupBy { it.paidBy.name }
            GroupBy.PAYMENT_MODE -> expenses.groupBy { it.paymentMode.displayName }
            GroupBy.MONTH -> expenses.groupBy { com.punitkumar.gruhkharch.util.DateUtils.formatMonthYear(it.date) }
        }
    }
}
