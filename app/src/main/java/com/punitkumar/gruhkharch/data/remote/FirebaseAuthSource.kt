package com.punitkumar.gruhkharch.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.punitkumar.gruhkharch.R
import com.punitkumar.gruhkharch.domain.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthSource @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    val currentUser: User?
        get() = auth.currentUser?.let {
            User(
                id = it.uid,
                name = it.displayName ?: "",
                email = it.email ?: "",
                photoUrl = it.photoUrl?.toString(),
                phoneNumber = it.phoneNumber
            )
        }

    val isSignedIn: Boolean get() = auth.currentUser != null
    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun signInWithGoogle(idToken: String): User {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: throw Exception("Sign in failed")
        return User(
            id = firebaseUser.uid,
            name = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            photoUrl = firebaseUser.photoUrl?.toString(),
            phoneNumber = firebaseUser.phoneNumber
        )
    }

    suspend fun deleteAccount() {
        val user = auth.currentUser ?: throw Exception("No user signed in")
        try {
            user.delete().await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
            val idToken = googleAccount?.idToken
                ?: throw Exception("Re-authentication required. Please sign out and sign in again, then retry.")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(credential).await()
            user.delete().await()
        }
        val gso = buildGso()
        GoogleSignIn.getClient(context, gso).revokeAccess().await()
    }

    fun signOut() {
        auth.signOut()
        val gso = buildGso()
        GoogleSignIn.getClient(context, gso).signOut()
    }

    private fun buildGso(): GoogleSignInOptions {
        val webClientId = context.getString(R.string.default_web_client_id)
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
}
