package com.punitkumar.gruhkharch.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentProjectHolder @Inject constructor() {
    private val _projectId = MutableStateFlow<String?>(null)
    val projectId: StateFlow<String?> = _projectId.asStateFlow()

    fun setProject(projectId: String) {
        _projectId.value = projectId
    }

    fun clear() {
        _projectId.value = null
    }
}
