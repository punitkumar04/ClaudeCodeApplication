package com.punitkumar.gruhkharch.domain.repository

import com.punitkumar.gruhkharch.domain.model.Member
import com.punitkumar.gruhkharch.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    suspend fun createProject(name: String, userId: String, userName: String): Result<Project>
    suspend fun joinProject(inviteCode: String, userId: String, userName: String): Result<Project>
    suspend fun getProject(projectId: String): Project?
    fun observeProject(projectId: String): Flow<Project?>
    suspend fun updateProject(project: Project): Result<Unit>
    suspend fun getProjectsForUser(userId: String): List<Project>
    fun getAllLocalProjects(): Flow<List<Project>>
    suspend fun updateBudget(projectId: String, budget: Double): Result<Unit>
    suspend fun updateCurrentStage(projectId: String, stage: String): Result<Unit>
    suspend fun addMember(projectId: String, member: Member): Result<Unit>
    suspend fun removeMember(projectId: String, userId: String): Result<Unit>
    suspend fun regenerateInviteCode(projectId: String): Result<String>
}
