package com.brainburst.domain.auth

/**
 * Platform-specific Google Sign-In provider
 * Each platform will provide its own implementation
 */
expect class GoogleSignInProvider {
    /**
     * Initiates the Google Sign-In flow and returns the ID token
     * @return The Google ID token to be used for Firebase authentication
     * @throws Exception if sign-in fails or is cancelled
     */
    suspend fun signIn(): String
}






