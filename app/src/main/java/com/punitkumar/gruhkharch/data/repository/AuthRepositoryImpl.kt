package com.punitkumar.gruhkharch.data.repository

import com.punitkumar.gruhkharch.data.remote.FirebaseAuthSource
import com.punitkumar.gruhkharch.domain.model.User
import com.punitkumar.gruhkharch.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authSource: FirebaseAuthSource
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

    override fun signOut() {
        authSource.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            authSource.deleteAccount()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
