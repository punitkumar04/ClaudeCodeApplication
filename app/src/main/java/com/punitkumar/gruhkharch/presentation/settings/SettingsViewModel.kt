package com.punitkumar.gruhkharch.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.Member
import com.punitkumar.gruhkharch.domain.model.Project
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    val isProjectOwner: Boolean = false,
    val isLoading: Boolean = false,
    val isProjectDeleted: Boolean = false,
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

    private var observeJob: Job? = null

    init {
        loadUserProfile()
        observeProjectChanges()
    }

    private fun loadUserProfile() {
        val user = authRepository.currentUser
        _state.update {
            it.copy(
                userName = user?.name ?: "",
                userEmail = user?.email ?: ""
            )
        }
    }

    private fun observeProjectChanges() {
        viewModelScope.launch {
            currentProjectHolder.projectId.collect { projectId ->
                observeJob?.cancel()
                if (projectId == null) {
                    _state.update { it.copy(project = null) }
                } else {
                    observeJob = launch { observeProject(projectId) }
                }
            }
        }
    }

    private suspend fun observeProject(projectId: String) {
        val currentUserId = authRepository.currentUserId ?: ""
        projectRepository.observeProject(projectId).collect { project ->
            if (project != null) {
                _state.update {
                    it.copy(
                        project = project,
                        inviteCode = project.inviteCode,
                        members = project.members,
                        budget = if (project.budget > 0) project.budget.toLong().toString() else "",
                        currentStage = project.currentStage,
                        isProjectOwner = project.createdBy == currentUserId
                    )
                }
            }
        }
    }

    fun updateBudget(budget: String) {
        _state.update { it.copy(budget = budget) }
    }

    fun saveBudget() {
        val project = _state.value.project ?: return
        val budgetAmount = _state.value.budget.toDoubleOrNull()
        if (budgetAmount == null || budgetAmount < 0 || !budgetAmount.isFinite()) {
            _state.update { it.copy(message = "Enter a valid budget amount") }
            return
        }
        viewModelScope.launch {
            projectRepository.updateBudget(project.id, budgetAmount)
                .onSuccess {
                    _state.update { it.copy(message = "Budget updated") }
                }
                .onFailure { e ->
                    _state.update { it.copy(message = e.message ?: "Failed to update budget") }
                }
        }
    }

    fun updateCurrentStage(stage: String) {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.updateCurrentStage(project.id, stage)
                .onSuccess {
                    _state.update { it.copy(message = "Stage updated") }
                }
                .onFailure { e ->
                    _state.update { it.copy(message = e.message ?: "Failed to update stage") }
                }
        }
    }

    fun regenerateInviteCode() {
        val project = _state.value.project ?: return
        viewModelScope.launch {
            projectRepository.regenerateInviteCode(project.id)
                .onSuccess { code ->
                    _state.update { it.copy(message = "New invite code generated") }
                }
                .onFailure { e ->
                    _state.update { it.copy(message = e.message ?: "Failed to regenerate code") }
                }
        }
    }

    fun deleteProject() {
        val project = _state.value.project ?: return
        if (!_state.value.isProjectOwner) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            projectRepository.deleteProject(project.id)
                .onSuccess {
                    currentProjectHolder.clear()
                    _state.update { it.copy(isLoading = false, isProjectDeleted = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, message = e.message ?: "Failed to delete project") }
                }
        }
    }

    fun deleteAccount(onAccountDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            authRepository.deleteAccount()
                .onSuccess {
                    currentProjectHolder.clear()
                    onAccountDeleted()
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, message = e.message ?: "Failed to delete account") }
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
