package com.punitkumar.gruhkharch.data.remote

import com.google.firebase.firestore.FieldValue
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
        val filtered = data.filterValues { it != null }
        projectsCollection.document(projectId).update(filtered).await()
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

    suspend fun addMemberAtomically(projectId: String, memberMap: Map<String, Any?>, userId: String) {
        val docRef = projectsCollection.document(projectId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val memberIds = (snapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            if (userId !in memberIds) {
                transaction.update(docRef, "members", FieldValue.arrayUnion(memberMap))
                transaction.update(docRef, "memberIds", FieldValue.arrayUnion(userId))
            }
        }.await()
    }

    suspend fun deleteProject(projectId: String) {
        val expensesRef = firestore.collection(Constants.FIRESTORE_PROJECTS)
            .document(projectId)
            .collection(Constants.FIRESTORE_EXPENSES)
        val expenses = expensesRef.get().await()

        val chunks = expenses.documents.chunked(499)
        for (chunk in chunks) {
            val batch = firestore.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.delete(projectsCollection.document(projectId))
            batch.commit().await()
        }
        if (expenses.documents.isEmpty()) {
            projectsCollection.document(projectId).delete().await()
        }
    }

    fun observeProject(projectId: String): Flow<Map<String, Any?>?> = callbackFlow {
        val listener = projectsCollection.document(projectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FirestoreProject", "Snapshot listener error", error)
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
