package com.punitkumar.gruhkharch.domain

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.projectDataStore by preferencesDataStore(name = "current_project")
private val KEY_PROJECT_ID = stringPreferencesKey("project_id")

@Singleton
class CurrentProjectHolder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _projectId = MutableStateFlow<String?>(null)
    val projectId: StateFlow<String?> = _projectId.asStateFlow()

    init {
        scope.launch {
            val saved = context.projectDataStore.data
                .map { it[KEY_PROJECT_ID] }
                .first()
            _projectId.value = saved
        }
    }

    fun setProject(projectId: String) {
        _projectId.value = projectId
        scope.launch {
            context.projectDataStore.edit { it[KEY_PROJECT_ID] = projectId }
        }
    }

    fun clear() {
        _projectId.value = null
        scope.launch {
            context.projectDataStore.edit { it.remove(KEY_PROJECT_ID) }
        }
    }
}
