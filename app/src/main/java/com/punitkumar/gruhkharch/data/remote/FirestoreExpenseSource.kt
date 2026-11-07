package com.punitkumar.gruhkharch.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.punitkumar.gruhkharch.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteExpenses(
    val expenses: List<Map<String, Any?>>,
    val isFromCache: Boolean
)

@Singleton
class FirestoreExpenseSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun expensesCollection(projectId: String) =
        firestore.collection(Constants.FIRESTORE_PROJECTS)
            .document(projectId)
            .collection(Constants.FIRESTORE_EXPENSES)

    /** Writes the full expense document, creating it if needed. Returns true once the server has it. */
    suspend fun setExpense(projectId: String, expenseId: String, expenseData: Map<String, Any?>): Boolean {
        return expensesCollection(projectId).document(expenseId).set(expenseData).awaitWrite()
    }

    suspend fun deleteExpense(projectId: String, expenseId: String): Boolean {
        return expensesCollection(projectId).document(expenseId).delete().awaitWrite()
    }

    suspend fun getExpense(projectId: String, expenseId: String): Map<String, Any?>? {
        val doc = expensesCollection(projectId).document(expenseId).get().await()
        return if (doc.exists()) doc.data?.plus("id" to doc.id) else null
    }

    suspend fun getAllExpenses(projectId: String): RemoteExpenses {
        val snapshot = expensesCollection(projectId).get().await()
        return RemoteExpenses(
            expenses = snapshot.documents.map { doc -> (doc.data ?: emptyMap()).plus("id" to doc.id) },
            isFromCache = snapshot.metadata.isFromCache
        )
    }

    fun observeExpenses(projectId: String): Flow<List<Map<String, Any?>>> = callbackFlow {
        val listener = expensesCollection(projectId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FirestoreExpense", "Snapshot listener error", error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.map { doc ->
                    (doc.data ?: emptyMap()).plus("id" to doc.id)
                } ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }
}
