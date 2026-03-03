package com.punitkumar.gruhkharch.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.punitkumar.gruhkharch.util.Constants
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadReceipt(projectId: String, imageUri: Uri): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference
            .child(Constants.STORAGE_RECEIPTS)
            .child(projectId)
            .child(fileName)
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteReceipt(receiptUrl: String) {
        try {
            storage.getReferenceFromUrl(receiptUrl).delete().await()
        } catch (e: Exception) {
            // Ignore if file doesn't exist
        }
    }
}
