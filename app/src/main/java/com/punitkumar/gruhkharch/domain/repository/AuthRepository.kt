package com.punitkumar.gruhkharch.domain.repository

import com.punitkumar.gruhkharch.domain.model.User

interface AuthRepository {
    val currentUser: User?
    val isSignedIn: Boolean
    val currentUserId: String?
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInAsGuest(name: String): Result<User>
    suspend fun linkGoogle(idToken: String): Result<Unit>
    fun signOut()
    suspend fun deleteAccount(): Result<Unit>
}
