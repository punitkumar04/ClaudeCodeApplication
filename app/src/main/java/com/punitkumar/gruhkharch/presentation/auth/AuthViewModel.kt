package com.punitkumar.gruhkharch.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punitkumar.gruhkharch.domain.CurrentProjectHolder
import com.punitkumar.gruhkharch.domain.model.Project
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val currentProjectHolder: CurrentProjectHolder
) : ViewModel() {

    private val _isSignedIn = MutableStateFlow(authRepository.isSignedIn)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _hasProject = MutableStateFlow(false)
    val hasProject: StateFlow<Boolean> = _hasProject.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess {
                    _isSignedIn.value = true
                    _authState.value = AuthState.Success
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Sign in failed")
                }
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val user = authRepository.currentUser ?: run {
                _authState.value = AuthState.Error("Not signed in")
                return@launch
            }
            projectRepository.createProject(name, user.id, user.name)
                .onSuccess { project ->
                    _currentProject.value = project
                    _hasProject.value = true
                    currentProjectHolder.setProject(project.id)
                    _authState.value = AuthState.Success
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Failed to create project")
                }
        }
    }

    fun joinProject(inviteCode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val user = authRepository.currentUser ?: run {
                _authState.value = AuthState.Error("Not signed in")
                return@launch
            }
            projectRepository.joinProject(inviteCode, user.id, user.name)
                .onSuccess { project ->
                    _currentProject.value = project
                    _hasProject.value = true
                    currentProjectHolder.setProject(project.id)
                    _authState.value = AuthState.Success
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "Failed to join project")
                }
        }
    }

    fun onSignInFailed(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun signOut() {
        authRepository.signOut()
        _isSignedIn.value = false
        _hasProject.value = false
        _currentProject.value = null
        currentProjectHolder.clear()
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
