package com.punitkumar.gruhkharch.data.repository

import com.google.firebase.firestore.Source
import com.punitkumar.gruhkharch.data.local.dao.ExpenseDao
import com.punitkumar.gruhkharch.data.local.dao.ProjectDao
import com.punitkumar.gruhkharch.data.remote.FirebaseAuthSource
import com.punitkumar.gruhkharch.data.remote.FirestoreProjectSource
import com.punitkumar.gruhkharch.domain.model.User
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authSource: FirebaseAuthSource,
    private val firestoreProjectSource: FirestoreProjectSource,
    private val expenseDao: ExpenseDao,
    private val projectDao: ProjectDao
) : AuthRepository {

    override val currentUser: User? get() = authSource.currentUser
    override val isSignedIn: Boolean get() = authSource.isSignedIn
    override val currentUserId: String? get() = authSource.currentUserId

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val user = authSource.signInWithGoogle(idToken)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAsGuest(name: String): Result<User> {
        return try {
            Result.success(authSource.signInAsGuest(name))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun linkGoogle(idToken: String): Result<Unit> {
        return try {
            authSource.linkGoogle(idToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        authSource.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = authSource.currentUserId ?: throw Exception("No user signed in")
            // Must run before the auth account is deleted, while Firestore still accepts this user.
            deleteRemoteUserData(userId)
            authSource.deleteAccount()
            expenseDao.deleteAll()
            projectDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Deletes projects the user owns (with their expenses) and removes them from shared projects.
    // Reads from the server so an offline cache can't make us skip projects.
    private suspend fun deleteRemoteUserData(userId: String) {
        firestoreProjectSource.getProjectsForUser(userId, Source.SERVER).forEach { project ->
            val projectId = project["id"] as? String ?: return@forEach
            if (project["createdBy"] == userId) {
                firestoreProjectSource.deleteProject(projectId)
            } else {
                firestoreProjectSource.removeMemberAtomically(projectId, userId)
            }
        }
    }
}
