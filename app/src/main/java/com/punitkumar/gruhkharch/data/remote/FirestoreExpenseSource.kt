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

@Singleton
class FirestoreExpenseSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun expensesCollection(projectId: String) =
        firestore.collection(Constants.FIRESTORE_PROJECTS)
            .document(projectId)
            .collection(Constants.FIRESTORE_EXPENSES)

    suspend fun addExpense(projectId: String, expenseId: String, expenseData: Map<String, Any?>): String {
        expensesCollection(projectId).document(expenseId).set(expenseData).await()
        return expenseId
    }

    suspend fun updateExpense(projectId: String, expenseId: String, data: Map<String, Any?>) {
        expensesCollection(projectId).document(expenseId).set(data).await()
    }

    suspend fun deleteExpense(projectId: String, expenseId: String) {
        expensesCollection(projectId).document(expenseId).delete().await()
    }

    suspend fun getExpense(projectId: String, expenseId: String): Map<String, Any?>? {
        val doc = expensesCollection(projectId).document(expenseId).get().await()
        return if (doc.exists()) doc.data?.plus("id" to doc.id) else null
    }

    fun observeExpenses(projectId: String): Flow<List<Map<String, Any?>>> = callbackFlow {
        val listener = expensesCollection(projectId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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
