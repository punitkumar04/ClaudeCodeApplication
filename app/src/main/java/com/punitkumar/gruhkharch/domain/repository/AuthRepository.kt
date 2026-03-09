package com.punitkumar.gruhkharch.domain.repository

import com.punitkumar.gruhkharch.domain.model.User

interface AuthRepository {
    val currentUser: User?
    val isSignedIn: Boolean
    val currentUserId: String?
    suspend fun signInWithGoogle(idToken: String): Result<User>
    fun signOut()
    suspend fun deleteAccount(): Result<Unit>
}
