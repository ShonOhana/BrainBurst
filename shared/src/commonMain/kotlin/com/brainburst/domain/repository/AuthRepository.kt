package com.brainburst.domain.repository

import com.brainburst.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /**
     * Get the current authenticated user as a Flow
     */
    val currentUser: Flow<User?>
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    
    /**
     * Create a new account with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<User>
    
    /**
     * Sign in with Google ID token
     */
    suspend fun signInWithGoogleToken(idToken: String): Result<User>
    
    /**
     * Sign out the current user
     */
    suspend fun signOut(): Result<Unit>
}

