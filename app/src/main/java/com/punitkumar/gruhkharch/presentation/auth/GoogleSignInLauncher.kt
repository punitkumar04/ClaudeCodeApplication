package com.punitkumar.gruhkharch.presentation.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.punitkumar.gruhkharch.R

/**
 * Returns a function that opens the Google account picker. [onIdToken] receives the ID token on
 * success; [onError] receives a user-facing message. A user backing out triggers neither.
 */
@Composable
fun rememberGoogleSignInLauncher(
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Configuration errors also come back as RESULT_CANCELED, so inspect any returned data.
        // No data at all means the user simply backed out.
        if (result.data == null) return@rememberLauncherForActivityResult
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val idToken = task.getResult(ApiException::class.java)?.idToken
            if (idToken != null) {
                onIdToken(idToken)
            } else {
                onError(context.getString(R.string.sign_in_no_token))
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> Unit
                CommonStatusCodes.DEVELOPER_ERROR -> onError(context.getString(R.string.sign_in_config_error))
                else -> onError(context.getString(R.string.sign_in_failed_format, e.statusCode))
            }
        }
    }

    return {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
    }
}
