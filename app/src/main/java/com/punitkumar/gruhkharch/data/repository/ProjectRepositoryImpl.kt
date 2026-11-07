package com.punitkumar.gruhkharch.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import com.punitkumar.gruhkharch.data.local.dao.ExpenseDao
import com.punitkumar.gruhkharch.data.local.dao.ProjectDao
import com.punitkumar.gruhkharch.data.remote.FirestoreProjectSource
import com.punitkumar.gruhkharch.domain.model.Member
import com.punitkumar.gruhkharch.domain.model.MemberRole
import com.punitkumar.gruhkharch.domain.model.Project
import com.punitkumar.gruhkharch.domain.repository.ProjectRepository
import com.punitkumar.gruhkharch.util.Constants
import com.punitkumar.gruhkharch.util.toEntity
import com.punitkumar.gruhkharch.util.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProjectRepository"
private const val INVITE_CODE_ATTEMPTS = 3

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val expenseDao: ExpenseDao,
    private val firestoreProjectSource: FirestoreProjectSource
) : ProjectRepository {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inviteCodesChecked: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    override suspend fun createProject(name: String, userId: String, userName: String): Result<Project> {
        return try {
            val member = Member(
                userId = userId,
                name = userName,
                role = MemberRole.OWNER,
                color = Constants.MEMBER_COLORS.first()
            )
            val createdAt = System.currentTimeMillis()
            fun projectData(inviteCode: String) = mapOf(
                "name" to name,
                "createdBy" to userId,
                "inviteCode" to inviteCode,
                "budget" to 0.0,
                "currency" to "INR",
                "currentStage" to "Pre-Construction",
                "members" to listOf(memberToMap(member)),
                "memberIds" to listOf(userId),
                "stageBudgets" to emptyMap<String, Double>(),
                "categoryBudgets" to emptyMap<String, Double>(),
                "createdAt" to createdAt
            )

            val (projectId, inviteCode) = withFreshInviteCode { code ->
                firestoreProjectSource.createProject(projectData(code), code, userId) to code
            }

            val project = Project(
                id = projectId,
                name = name,
                createdBy = userId,
                inviteCode = inviteCode,
                members = listOf(member),
                createdAt = createdAt
            )

            projectDao.insertProject(project.toEntity())
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinProject(inviteCode: String, userId: String, userName: String): Result<Project> {
        return try {
            val projectId = firestoreProjectSource.getProjectIdForInviteCode(inviteCode)
                ?: return Result.failure(
                    Exception("Invite code not found. If it's correct, ask the project owner to open the app once, then try again.")
                )
            val projectData = firestoreProjectSource.getProject(projectId)
                ?: return Result.failure(Exception("Project not found"))

            val existingMembers = (projectData["members"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
            val alreadyMember = existingMembers.any { (it["userId"] as? String) == userId }

            if (!alreadyMember) {
                val colorIndex = existingMembers.size % Constants.MEMBER_COLORS.size
                val newMemberMap = memberToMap(
                    Member(userId = userId, name = userName, color = Constants.MEMBER_COLORS[colorIndex])
                )
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
        val local = projectDao.getProjectById(projectId)?.toDomain()
        if (local != null) return local
        return try {
            val remote = firestoreProjectSource.getProject(projectId) ?: return null
            val project = mapToProject(remote)
            projectDao.insertProject(project.toEntity())
            project
        } catch (e: Exception) {
            null
        }
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
            val remote = firestoreProjectSource.getProjectsForUser(userId)
            val projects = remote.projects.map { mapToProject(it) }
            projects.forEach { projectDao.insertProject(it.toEntity()) }
            // Drop local copies of projects the user was removed from (only trust a server answer).
            if (!remote.isFromCache) {
                val currentIds = projects.map { it.id }.toSet()
                projectDao.getAllProjectsSync()
                    .filter { it.id !in currentIds && it.toDomain().members.any { m -> m.userId == userId } }
                    .forEach { stale ->
                        expenseDao.deleteAllExpensesForProject(stale.id)
                        projectDao.deleteProject(stale)
                    }
            }
            projects.filter { it.createdBy == userId }.forEach { migrateInviteCode(it) }
            projects
        } catch (e: Exception) {
            // Offline: the local cache may also hold projects of a previous user on this device.
            projectDao.getAllProjectsSync()
                .map { it.toDomain() }
                .filter { project -> project.members.any { it.userId == userId } }
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
            val project = projectDao.getProjectById(projectId)?.toDomain()
                ?: return Result.failure(Exception("Project not found"))
            if (userId == project.createdBy) {
                return Result.failure(Exception("The project owner can't be removed"))
            }
            // Transaction, so members who joined in the meantime aren't overwritten.
            firestoreProjectSource.removeMemberAtomically(projectId, userId)
            projectDao.updateProject(project.copy(members = project.members.filter { it.userId != userId }).toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun regenerateInviteCode(projectId: String): Result<String> {
        return try {
            val project = projectDao.getProjectById(projectId)?.toDomain() ?: return Result.failure(Exception("Project not found"))
            val newCode = withFreshInviteCode { code ->
                firestoreProjectSource.regenerateInviteCode(projectId, project.inviteCode, code, project.createdBy)
                code
            }
            projectDao.updateProject(project.copy(inviteCode = newCode).toEntity())
            Result.success(newCode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            firestoreProjectSource.deleteProject(projectId)
            expenseDao.deleteAllExpensesForProject(projectId)
            projectDao.getProjectById(projectId)?.let { projectDao.deleteProject(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addNameOnlyMember(projectId: String, name: String): Result<Member> {
        return try {
            val project = projectDao.getProjectById(projectId)?.toDomain()
                ?: return Result.failure(Exception("Project not found"))
            val member = Member(
                userId = "local_${UUID.randomUUID()}",
                name = name,
                color = Constants.MEMBER_COLORS[project.members.size % Constants.MEMBER_COLORS.size],
                hasAccount = false
            )
            firestoreProjectSource.addNameOnlyMember(projectId, memberToMap(member))
            projectDao.updateProject(project.copy(members = project.members + member).toEntity())
            Result.success(member)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun memberToMap(member: Member): Map<String, Any?> = mapOf(
        "userId" to member.userId,
        "name" to member.name,
        "role" to member.role.name,
        "color" to member.color,
        "hasAccount" to member.hasAccount
    )

    // Projects created before invite codes had their own collection need an entry so others can
    // join. Checked once per app run, in the background so the project list isn't delayed.
    private fun migrateInviteCode(project: Project) {
        if (!inviteCodesChecked.add(project.id)) return
        backgroundScope.launch {
            try {
                firestoreProjectSource.ensureInviteCode(project.id, project.inviteCode, project.createdBy)
            } catch (e: Exception) {
                inviteCodesChecked.remove(project.id)
                Log.w(TAG, "Failed to migrate invite code for ${project.id}", e)
            }
        }
    }

    // A code already used by another project is rejected by the security rules; try a new one.
    private suspend fun <T> withFreshInviteCode(block: suspend (String) -> T): T {
        repeat(INVITE_CODE_ATTEMPTS - 1) {
            try {
                return block(generateInviteCode())
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        return block(generateInviteCode())
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
        "members" to project.members.map { memberToMap(it) },
        // memberIds drives security rules and queries, so it lists only people with an account.
        "memberIds" to project.members.filter { it.hasAccount }.map { it.userId },
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
                color = m["color"] as? String ?: Constants.MEMBER_COLORS.first(),
                hasAccount = m["hasAccount"] as? Boolean ?: true
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
