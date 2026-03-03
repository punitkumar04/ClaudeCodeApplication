package com.punitkumar.gruhkharch.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.punitkumar.gruhkharch.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProjectSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val projectsCollection = firestore.collection(Constants.FIRESTORE_PROJECTS)

    suspend fun createProject(data: Map<String, Any?>): String {
        val docRef = projectsCollection.add(data).await()
        return docRef.id
    }

    suspend fun updateProject(projectId: String, data: Map<String, Any?>) {
        projectsCollection.document(projectId).set(data).await()
    }

    suspend fun getProject(projectId: String): Map<String, Any?>? {
        val doc = projectsCollection.document(projectId).get().await()
        return if (doc.exists()) doc.data?.plus("id" to doc.id) else null
    }

    suspend fun getProjectByInviteCode(code: String): Map<String, Any?>? {
        val snapshot = projectsCollection
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.let {
            it.data?.plus("id" to it.id)
        }
    }

    suspend fun getProjectsForUser(userId: String): List<Map<String, Any?>> {
        val snapshot = projectsCollection
            .whereArrayContains("memberIds", userId)
            .get()
            .await()
        return snapshot.documents.map { doc ->
            (doc.data ?: emptyMap()).plus("id" to doc.id)
        }
    }

    fun observeProject(projectId: String): Flow<Map<String, Any?>?> = callbackFlow {
        val listener = projectsCollection.document(projectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val data = if (snapshot != null && snapshot.exists()) {
                    snapshot.data?.plus("id" to snapshot.id)
                } else null
                trySend(data)
            }
        awaitClose { listener.remove() }
    }
}
