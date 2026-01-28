package com.brainburst.data.repository

import com.brainburst.domain.model.User
import com.brainburst.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val coroutineScope: CoroutineScope
) : AuthRepository {
    
    private val usersCollection = firestore.collection("users")
    
    override val currentUser: StateFlow<User?> = firebaseAuth.authStateChanged
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) {
                flowOf<User?>(null)
            } else {
                flow<User?> {
                    // Check if user has password provider
                    val isPasswordProvider = firebaseUser.providerData.any { it.providerId == "password" }
                    
                    // First create User from Firebase Auth
                    val baseUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        isPasswordProvider = isPasswordProvider
                    )
                    
                    // Try to fetch from Firestore to get firstName/lastName
                    try {
                        val userDoc = usersCollection.document(firebaseUser.uid).get()
                        if (userDoc.exists) {
                            val firstName = userDoc.get<String?>("firstName") ?: ""
                            val lastName = userDoc.get<String?>("lastName") ?: ""
                            val firestoreDisplayName = userDoc.get<String?>("displayName")
                            
                            // Prefer firstName + lastName, then displayName from Firestore, then Firebase Auth displayName
                            val fullDisplayName = when {
                                firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
                                firstName.isNotBlank() -> firstName
                                lastName.isNotBlank() -> lastName
                                !firestoreDisplayName.isNullOrBlank() -> firestoreDisplayName
                                else -> baseUser.displayName
                            }
                            
                            emit(baseUser.copy(displayName = fullDisplayName))
                        } else {
                            emit(baseUser)
                        }
                    } catch (e: Exception) {
                        // If Firestore fetch fails, use Firebase Auth data
                        emit(baseUser)
                    }
                }
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    
    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password)
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName
                )
                
                // Try to load existing user data from Firestore, or save if doesn't exist
                loadOrSaveUserToFirestore(user)
                
                Result.success(user)
            } else {
                Result.failure(Exception("Sign in failed: No user returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signUpWithEmail(email: String, password: String, firstName: String, lastName: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password)
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val displayName = "$firstName $lastName".trim()
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = displayName
                )
                
                // Save to Firestore with firstName and lastName
                saveUserToFirestore(user, firstName, lastName).getOrElse {
                    // Log error but don't fail sign-up
                    println("Failed to save user to Firestore: ${it.message}")
                }
                
                Result.success(user)
            } else {
                Result.failure(Exception("Sign up failed: No user returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signInWithGoogleToken(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.credential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential)
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val displayName = firebaseUser.displayName ?: ""
                // Try to parse Google displayName into firstName and lastName
                val nameParts = displayName.split(" ", limit = 2)
                val firstName = nameParts.getOrElse(0) { "" }
                val lastName = nameParts.getOrElse(1) { "" }
                
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = displayName
                )
                
                // Save/update user in Firestore with firstName and lastName
                saveUserToFirestore(user, firstName, lastName).getOrElse {
                    println("Failed to save user to Firestore: ${it.message}")
                }
                
                Result.success(user)
            } else {
                Result.failure(Exception("Google sign in failed: No user returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveUserToFirestore(user: User): Result<Unit> {
        // Default implementation - use empty strings for firstName/lastName if not provided
        return saveUserToFirestore(user, "", "")
    }
    
    private suspend fun saveUserToFirestore(user: User, firstName: String, lastName: String): Result<Unit> {
        return try {
            val displayName = user.displayName ?: if (firstName.isNotBlank() || lastName.isNotBlank()) {
                "$firstName $lastName".trim()
            } else {
                user.email ?: "Player ${user.uid.take(6)}"
            }
            
            usersCollection.document(user.uid).set(
                mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "displayName" to displayName
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun loadOrSaveUserToFirestore(user: User) {
        try {
            val userDoc = usersCollection.document(user.uid).get()
            if (!userDoc.exists) {
                // User doesn't exist in Firestore, save it
                // Try to parse displayName if available
                val displayName = user.displayName ?: ""
                val nameParts = displayName.split(" ", limit = 2)
                val firstName = nameParts.getOrElse(0) { "" }
                val lastName = nameParts.getOrElse(1) { "" }
                saveUserToFirestore(user, firstName, lastName).getOrElse {
                    println("Failed to save user to Firestore: ${it.message}")
                }
            }
        } catch (e: Exception) {
            // If fetch fails, try to save anyway
            val displayName = user.displayName ?: ""
            val nameParts = displayName.split(" ", limit = 2)
            val firstName = nameParts.getOrElse(0) { "" }
            val lastName = nameParts.getOrElse(1) { "" }
            saveUserToFirestore(user, firstName, lastName).getOrElse {
                println("Failed to save user to Firestore: ${it.message}")
            }
        }
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val currentFirebaseUser = firebaseAuth.currentUser
            if (currentFirebaseUser == null) {
                return Result.failure(Exception("No user is currently logged in"))
            }
            
            val userId = currentFirebaseUser.uid
            
            // Step 1: Delete all user's game results from Firestore
            try {
                val resultsCollection = firestore.collection("results")
                val userResults = resultsCollection
                    .where { "userId" equalTo userId }
                    .get()
                
                // Delete each result document
                userResults.documents.forEach { document ->
                    try {
                        document.reference.delete()
                    } catch (e: Exception) {
                        println("Failed to delete result document ${document.id}: ${e.message}")
                        // Continue with other deletions even if one fails
                    }
                }
            } catch (e: Exception) {
                println("Failed to query/delete user results: ${e.message}")
                // Continue with account deletion even if results deletion fails
            }
            
            // Step 2: Delete user profile from Firestore
            try {
                usersCollection.document(userId).delete()
            } catch (e: Exception) {
                println("Failed to delete user profile: ${e.message}")
                // Continue with account deletion even if profile deletion fails
            }
            
            // Step 3: Delete the Firebase Auth account
            // Note: This might fail if the user's authentication token is too old
            // In that case, the user needs to re-authenticate first
            try {
                currentFirebaseUser.delete()
                Result.success(Unit)
            } catch (e: Exception) {
                // Check if error is due to requiring recent authentication
                val errorMessage = e.message?.lowercase() ?: ""
                if (errorMessage.contains("requires-recent-login") || 
                    errorMessage.contains("recent authentication") ||
                    errorMessage.contains("credential")) {
                    Result.failure(Exception("For security reasons, please log out and log back in before deleting your account."))
                } else {
                    Result.failure(Exception("Failed to delete account: ${e.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred: ${e.message}"))
        }
    }
    
    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val currentFirebaseUser = firebaseAuth.currentUser
            if (currentFirebaseUser == null) {
                return Result.failure(Exception("No user is currently logged in"))
            }
            
            val email = currentFirebaseUser.email
            if (email.isNullOrBlank()) {
                return Result.failure(Exception("Cannot update password for users without email"))
            }
            
            // Step 1: Re-authenticate with current password to verify identity
            try {
                firebaseAuth.signInWithEmailAndPassword(email, currentPassword)
            } catch (e: Exception) {
                return Result.failure(Exception("Current password is incorrect"))
            }
            
            // Step 2: Update password
            try {
                currentFirebaseUser.updatePassword(newPassword)
                Result.success(Unit)
            } catch (e: Exception) {
                val errorMessage = e.message?.lowercase() ?: ""
                when {
                    errorMessage.contains("weak-password") || errorMessage.contains("password") -> {
                        Result.failure(Exception("Password should be at least 6 characters"))
                    }
                    else -> {
                        Result.failure(Exception("Failed to update password: ${e.message}"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred: ${e.message}"))
        }
    }
    
    override suspend fun checkUserRegistration(): Boolean {
        return try {
            // Wait for Firebase Auth to initialize and get first auth state
            val firebaseUser = firebaseAuth.authStateChanged.first()
            
            if (firebaseUser == null) {
                return false
            }
            
            // Check if user exists in Firestore
            val userDoc = usersCollection.document(firebaseUser.uid).get()
            userDoc.exists
        } catch (e: Exception) {
            // On any error, return false (treat as not registered)
            false
        }
    }
    
    override fun hasPasswordProvider(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        return firebaseUser.providerData.any { it.providerId == "password" }
    }
}


