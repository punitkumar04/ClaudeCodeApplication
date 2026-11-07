package com.punitkumar.gruhkharch.data.remote

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val REMOTE_WRITE_TIMEOUT_MS = 10_000L

/**
 * Firestore completes a write task only once the server acknowledges it, so awaiting it while
 * offline suspends until connectivity returns. The write itself is already queued in Firestore's
 * local cache and will be sent automatically, so callers stop waiting after a timeout.
 *
 * @return true if the server acknowledged the write, false if it is still pending.
 */
internal suspend fun <T> Task<T>.awaitWrite(): Boolean =
    withTimeoutOrNull(REMOTE_WRITE_TIMEOUT_MS) {
        await()
        true
    } ?: false
