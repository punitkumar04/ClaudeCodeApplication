package com.punitkumar.gruhkharch.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
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

    // Invite codes live in their own collection so joining is a single-document read by code;
    // security rules then never need to let strangers query the projects collection.
    private val inviteCodesCollection = firestore.collection(Constants.FIRESTORE_INVITE_CODES)

    private fun inviteCodeData(projectId: String, ownerId: String) =
        mapOf("projectId" to projectId, "createdBy" to ownerId)

    /**
     * Writes the project and its invite code together. The id is generated on the client, so the
     * project is usable even if the write is still queued offline. Throws if the code is taken.
     */
    suspend fun createProject(data: Map<String, Any?>, inviteCode: String, ownerId: String): String {
        val projectRef = projectsCollection.document()
        firestore.batch()
            .set(projectRef, data)
            .set(inviteCodesCollection.document(inviteCode), inviteCodeData(projectRef.id, ownerId))
            .commit()
            .awaitWrite()
        return projectRef.id
    }

    suspend fun getProjectIdForInviteCode(code: String): String? {
        val doc = inviteCodesCollection.document(code).get().await()
        return if (doc.exists()) doc.getString("projectId") else null
    }

    /** Creates the invite-code entry for projects made before invite codes had their own collection. */
    suspend fun ensureInviteCode(projectId: String, code: String, ownerId: String) {
        if (code.isBlank()) return
        val ref = inviteCodesCollection.document(code)
        if (!ref.get().await().exists()) {
            ref.set(inviteCodeData(projectId, ownerId)).awaitWrite()
        }
    }

    suspend fun regenerateInviteCode(projectId: String, oldCode: String, newCode: String, ownerId: String) {
        val batch = firestore.batch()
            .update(projectsCollection.document(projectId), "inviteCode", newCode)
            .set(inviteCodesCollection.document(newCode), inviteCodeData(projectId, ownerId))
        deleteInviteCodeIfOwned(batch, oldCode, ownerId)
        batch.commit().awaitWrite()
    }

    // Rules reject deleting a code doc that doesn't exist, which would fail the whole batch.
    private suspend fun deleteInviteCodeIfOwned(batch: WriteBatch, code: String?, ownerId: String) {
        if (code.isNullOrBlank()) return
        val ref = inviteCodesCollection.document(code)
        val doc = ref.get().await()
        if (doc.exists() && doc.getString("createdBy") == ownerId) batch.delete(ref)
    }

    suspend fun updateProject(projectId: String, data: Map<String, Any?>) {
        val filtered = data.filterValues { it != null }
        projectsCollection.document(projectId).update(filtered).awaitWrite()
    }

    suspend fun getProject(projectId: String): Map<String, Any?>? {
        val doc = projectsCollection.document(projectId).get().await()
        return if (doc.exists()) doc.data?.plus("id" to doc.id) else null
    }

    suspend fun getProjectsForUser(userId: String, source: Source = Source.DEFAULT): List<Map<String, Any?>> {
        val snapshot = projectsCollection
            .whereArrayContains("memberIds", userId)
            .get(source)
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

    // arrayUnion is applied atomically on the server and also works while offline.
    suspend fun addNameOnlyMember(projectId: String, memberMap: Map<String, Any?>) {
        projectsCollection.document(projectId)
            .update("members", FieldValue.arrayUnion(memberMap))
            .awaitWrite()
    }

    suspend fun removeMemberAtomically(projectId: String, userId: String) {
        val docRef = projectsCollection.document(projectId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val members = (snapshot.get("members") as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?.filter { it["userId"] != userId }
                ?: emptyList()
            val memberIds = (snapshot.get("memberIds") as? List<*>)
                ?.filterIsInstance<String>()
                ?.filter { it != userId }
                ?: emptyList()
            transaction.update(docRef, mapOf("members" to members, "memberIds" to memberIds))
        }.await()
    }

    suspend fun deleteProject(projectId: String) {
        val expensesRef = firestore.collection(Constants.FIRESTORE_PROJECTS)
            .document(projectId)
            .collection(Constants.FIRESTORE_EXPENSES)
        val projectDoc = projectsCollection.document(projectId).get().await()
        val expenses = expensesRef.get().await()

        // Expense rules look up the project to check membership, so the project doc must be
        // deleted in the last batch, after all its expenses.
        val chunks = expenses.documents.chunked(498)
        chunks.forEachIndexed { index, chunk ->
            if (index < chunks.lastIndex) {
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().awaitWrite()
            }
        }
        val lastBatch = firestore.batch()
        chunks.lastOrNull()?.forEach { lastBatch.delete(it.reference) }
        lastBatch.delete(projectsCollection.document(projectId))
        projectDoc.getString("createdBy")?.let { ownerId ->
            deleteInviteCodeIfOwned(lastBatch, projectDoc.getString("inviteCode"), ownerId)
        }
        lastBatch.commit().awaitWrite()
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
