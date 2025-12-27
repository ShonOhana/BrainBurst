package com.brainburst.domain.auth

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class GoogleSignInProvider(private val activity: ComponentActivity) {
    
    // Default web client ID - this should be from google-services.json
    // You'll need to add your Web Client ID from Firebase Console
    private val webClientId = "720231725608-782cugcmp087vvk3tn77h40spukpk886.apps.googleusercontent.com"
    
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(activity, gso)
    }
    
    actual suspend fun signIn(): String = suspendCancellableCoroutine { continuation ->
        val signInIntent = googleSignInClient.signInIntent
        
        val launcher = activity.activityResultRegistry.register(
            "google_sign_in",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (idToken != null) {
                        if (continuation.isActive) {
                            continuation.resume(idToken)
                        }
                    } else {
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception("Failed to get ID token"))
                        }
                    }
                } catch (e: ApiException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            } else {
                if (continuation.isActive) {
                    continuation.resumeWithException(Exception("Sign-In cancelled"))
                }
            }
        }
        
        continuation.invokeOnCancellation {
            launcher.unregister()
        }
        
        launcher.launch(signInIntent)
    }
}



