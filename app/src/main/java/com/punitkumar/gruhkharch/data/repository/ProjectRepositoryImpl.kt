package com.punitkumar.gruhkharch.data.repository

import com.punitkumar.gruhkharch.data.local.dao.ProjectDao
import com.punitkumar.gruhkharch.data.remote.FirestoreProjectSource
import com.punitkumar.gruhkharch.domain.model.Member
import com.punitkumar.gruhkharch.domain.model.MemberRole
import com.punitkumar.gruhkharch.domain.model.Project
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import com.punitkumar.gruhkharch.util.Constants
import com.punitkumar.gruhkharch.util.toEntity
import com.punitkumar.gruhkharch.util.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val firestoreProjectSource: FirestoreProjectSource
) : ProjectRepository {

    override suspend fun createProject(name: String, userId: String, userName: String): Result<Project> {
        return try {
            val inviteCode = generateInviteCode()
            val member = Member(
                userId = userId,
                name = userName,
                role = MemberRole.OWNER,
                color = Constants.MEMBER_COLORS.first()
            )
            val projectData = mapOf(
                "name" to name,
                "createdBy" to userId,
                "inviteCode" to inviteCode,
                "budget" to 0.0,
                "currency" to "INR",
                "currentStage" to "Pre-Construction",
                "members" to listOf(mapOf("userId" to userId, "name" to userName, "role" to "OWNER", "color" to member.color)),
                "memberIds" to listOf(userId),
                "stageBudgets" to emptyMap<String, Double>(),
                "categoryBudgets" to emptyMap<String, Double>(),
                "createdAt" to System.currentTimeMillis()
            )

            val projectId = firestoreProjectSource.createProject(projectData)

            val project = Project(
                id = projectId,
                name = name,
                createdBy = userId,
                inviteCode = inviteCode,
                members = listOf(member),
                createdAt = System.currentTimeMillis()
            )

            projectDao.insertProject(project.toEntity())
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinProject(inviteCode: String, userId: String, userName: String): Result<Project> {
        return try {
            val projectData = firestoreProjectSource.getProjectByInviteCode(inviteCode)
                ?: return Result.failure(Exception("Invalid invite code"))

            val projectId = projectData["id"] as? String ?: return Result.failure(Exception("Project not found"))

            val existingMembers = (projectData["members"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
            val alreadyMember = existingMembers.any { (it["userId"] as? String) == userId }

            if (!alreadyMember) {
                val colorIndex = existingMembers.size % Constants.MEMBER_COLORS.size
                val newMemberMap = mapOf<String, Any?>("userId" to userId, "name" to userName, "role" to "FAMILY_MEMBER", "color" to Constants.MEMBER_COLORS[colorIndex])
                firestoreProjectSource.addMemberAtomically(projectId, newMemberMap, userId)
            }

            val project = mapToProject(firestoreProjectSource.getProject(projectId) ?: projectData)
            projectDao.insertProject(project.toEntity())
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProject(projectId: String): Project? {
        return projectDao.getProjectById(projectId)?.toDomain()
    }

    override fun observeProject(projectId: String): Flow<Project?> {
        return projectDao.getProjectFlow(projectId).map { it?.toDomain() }
    }

    override suspend fun updateProject(project: Project): Result<Unit> {
        return try {
            projectDao.updateProject(project.toEntity())
            firestoreProjectSource.updateProject(project.id, projectToMap(project))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectsForUser(userId: String): List<Project> {
        return try {
            firestoreProjectSource.getProjectsForUser(userId).map { mapToProject(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getAllLocalProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun updateBudget(projectId: String, budget: Double): Result<Unit> {
        return try {
            val project = projectDao.getProjectById(projectId)?.toDomain() ?: return Result.failure(Exception("Project not found"))
            val updated = project.copy(budget = budget)
            projectDao.updateProject(updated.toEntity())
            firestoreProjectSource.updateProject(projectId, projectToMap(updated))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCurrentStage(projectId: String, stage: String): Result<Unit> {
        return try {
            val project = projectDao.getProjectById(projectId)?.toDomain() ?: return Result.failure(Exception("Project not found"))
            val updated = project.copy(currentStage = stage)
            projectDao.updateProject(updated.toEntity())
            firestoreProjectSource.updateProject(projectId, projectToMap(updated))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMember(projectId: String, member: Member): Result<Unit> {
        return try {
            val project = projectDao.getProjectById(projectId)?.toDomain() ?: return Result.failure(Exception("Project not found"))
            val updated = project.copy(members = project.members + member)
            projectDao.updateProject(updated.toEntity())
            firestoreProjectSource.updateProject(projectId, projectToMap(updated))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(projectId: String, userId: String): Result<Unit> {
        return try {
            val project = projectDao.getProjectById(projectId)?.toDomain() ?: return Result.failure(Exception("Project not found"))
            val updated = project.copy(members = project.members.filter { it.userId != userId })
            projectDao.updateProject(updated.toEntity())
            firestoreProjectSource.updateProject(projectId, projectToMap(updated))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun regenerateInviteCode(projectId: String): Result<String> {
        return try {
            val newCode = generateInviteCode()
            val project = projectDao.getProjectById(projectId)?.toDomain() ?: return Result.failure(Exception("Project not found"))
            val updated = project.copy(inviteCode = newCode)
            projectDao.updateProject(updated.toEntity())
            firestoreProjectSource.updateProject(projectId, projectToMap(updated))
            Result.success(newCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            firestoreProjectSource.deleteProject(projectId)
            projectDao.getProjectById(projectId)?.let { projectDao.deleteProject(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..Constants.INVITE_CODE_LENGTH).map { chars.random() }.joinToString("")
    }

    private fun projectToMap(project: Project): Map<String, Any?> = mapOf(
        "name" to project.name,
        "createdBy" to project.createdBy,
        "inviteCode" to project.inviteCode,
        "budget" to project.budget,
        "currency" to project.currency,
        "currentStage" to project.currentStage,
        "members" to project.members.map { mapOf("userId" to it.userId, "name" to it.name, "role" to it.role.name, "color" to it.color) },
        "memberIds" to project.members.map { it.userId },
        "stageBudgets" to project.stageBudgets,
        "categoryBudgets" to project.categoryBudgets,
        "createdAt" to project.createdAt
    )

    private fun mapToProject(map: Map<String, Any?>): Project {
        val membersRaw = (map["members"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
        val members = membersRaw.map { m ->
            Member(
                userId = m["userId"] as? String ?: "",
                name = m["name"] as? String ?: "",
                role = try { MemberRole.valueOf(m["role"] as? String ?: "FAMILY_MEMBER") } catch (e: Exception) { MemberRole.FAMILY_MEMBER },
                color = m["color"] as? String ?: Constants.MEMBER_COLORS.first()
            )
        }
        val stageBudgets = (map["stageBudgets"] as? Map<*, *>)
            ?.entries?.associate { (it.key as? String ?: "") to ((it.value as? Number)?.toDouble() ?: 0.0) }
            ?: emptyMap()
        val categoryBudgets = (map["categoryBudgets"] as? Map<*, *>)
            ?.entries?.associate { (it.key as? String ?: "") to ((it.value as? Number)?.toDouble() ?: 0.0) }
            ?: emptyMap()

        return Project(
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            createdBy = map["createdBy"] as? String ?: "",
            inviteCode = map["inviteCode"] as? String ?: "",
            budget = (map["budget"] as? Number)?.toDouble() ?: 0.0,
            currency = map["currency"] as? String ?: "INR",
            currentStage = map["currentStage"] as? String ?: "Pre-Construction",
            members = members,
            stageBudgets = stageBudgets,
            categoryBudgets = categoryBudgets,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
