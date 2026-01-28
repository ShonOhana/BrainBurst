package com.brainburst.domain.repository

import com.brainburst.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    /**
     * Get the current authenticated user as a StateFlow
     * Can be accessed synchronously via .value
     */
    val currentUser: StateFlow<User?>
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    
    /**
     * Create a new account with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String, firstName: String, lastName: String): Result<User>
    
    /**
     * Sign in with Google ID token
     */
    suspend fun signInWithGoogleToken(idToken: String): Result<User>
    
    /**
     * Save user data to Firestore users collection
     */
    suspend fun saveUserToFirestore(user: User): Result<Unit>
    
    /**
     * Sign out the current user
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Delete the current user's account and all associated data
     * This will:
     * 1. Delete user profile from Firestore
     * 2. Delete all user's game results
     * 3. Delete the Firebase Auth account
     */
    suspend fun deleteAccount(): Result<Unit>
    
    /**
     * Update the current user's password
     * Requires the user to be recently authenticated
     * @param currentPassword The user's current password for verification
     * @param newPassword The new password to set
     */
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>
    
    /**
     * Check if a user is registered in Firestore
     * Waits for Firebase Auth to initialize, then checks Firestore
     * @return true if user is authenticated and exists in Firestore, false otherwise
     */
    suspend fun checkUserRegistration(): Boolean
    
    /**
     * Check if the current user has a password provider (email/password authentication)
     * @return true if the user signed in with email/password, false if using Google or other providers
     */
    fun hasPasswordProvider(): Boolean
}


