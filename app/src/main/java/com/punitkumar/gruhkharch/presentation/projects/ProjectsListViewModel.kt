package com.punitkumar.gruhkharch.presentation.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.Project
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ExpenseRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectWithTotal(
    val project: Project,
    val totalSpent: Double = 0.0
)

data class ProjectsListState(
    val projects: List<ProjectWithTotal> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProjectsListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val expenseRepository: ExpenseRepository,
    private val authRepository: AuthRepository,
    private val currentProjectHolder: CurrentProjectHolder
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsListState())
    val state: StateFlow<ProjectsListState> = _state.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: return@launch
            val projects = projectRepository.getProjectsForUser(userId)

            if (projects.isEmpty()) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            // Initialize with zero totals
            _state.update {
                it.copy(
                    projects = projects.map { p -> ProjectWithTotal(p) },
                    isLoading = false
                )
            }

            // Observe total spent for each project
            projects.forEach { project ->
                launch {
                    expenseRepository.getTotalSpent(project.id).collect { total ->
                        _state.update { state ->
                            val updated = state.projects.map { pwt ->
                                if (pwt.project.id == project.id) pwt.copy(totalSpent = total)
                                else pwt
                            }
                            state.copy(projects = updated)
                        }
                    }
                }
            }
        }
    }

    fun selectProject(projectId: String) {
        currentProjectHolder.setProject(projectId)
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        loadProjects()
    }
}
