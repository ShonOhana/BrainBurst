package com.brainburst.domain.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
actual class GoogleSignInProvider {
    
    actual suspend fun signIn(): String = suspendCancellableCoroutine { continuation ->
        // Google Sign-In temporarily disabled on iOS due to framework conflicts
        // To enable: add GoogleSignIn pod and implement native sign-in flow
        continuation.resumeWithException(
            Exception(
                "iOS Google Sign-In is temporarily disabled.\n" +
                "Please use email/password authentication for now."
            )
        )
    }
}

