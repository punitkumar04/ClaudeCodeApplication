package com.punitkumar.gruhkharch.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.Member
import com.punitkumar.gruhkharch.domain.model.Project
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val userName: String = "",
    val userEmail: String = "",
    val project: Project? = null,
    val inviteCode: String = "",
    val members: List<Member> = emptyList(),
    val budget: String = "",
    val currentStage: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val currentProjectHolder: CurrentProjectHolder
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            _state.update {
                it.copy(
                    userName = user?.name ?: "",
                    userEmail = user?.email ?: ""
                )
            }

            val projectId = currentProjectHolder.projectId.value ?: return@launch
            val project = projectRepository.getProject(projectId) ?: return@launch

            _state.update {
                it.copy(
                    project = project,
                    inviteCode = project.inviteCode,
                    members = project.members,
                    budget = if (project.budget > 0) project.budget.toLong().toString() else "",
                    currentStage = project.currentStage
                )
            }
        }
    }

    fun updateBudget(budget: String) {
        _state.update { it.copy(budget = budget) }
    }

    fun saveBudget() {
        val project = _state.value.project ?: return
        val budgetAmount = _state.value.budget.toDoubleOrNull() ?: return
        viewModelScope.launch {
            projectRepository.updateBudget(project.id, budgetAmount)
            _state.update { it.copy(message = "Budget updated") }
        }
    }

    fun updateCurrentStage(stage: String) {
        _state.update { it.copy(currentStage = stage) }
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.updateCurrentStage(project.id, stage)
            _state.update { it.copy(message = "Stage updated") }
        }
    }

    fun regenerateInviteCode() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.regenerateInviteCode(project.id)
                .onSuccess { code ->
                    _state.update { it.copy(inviteCode = code, message = "New invite code generated") }
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        currentProjectHolder.clear()
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
