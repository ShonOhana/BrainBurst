package com.brainburst.data.repository

import com.brainburst.domain.model.User
import com.brainburst.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
                    // First create User from Firebase Auth
                    val baseUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName
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
}


