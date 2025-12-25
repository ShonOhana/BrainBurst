package com.brainburst.data.repository

import com.brainburst.domain.model.User
import com.brainburst.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val coroutineScope: CoroutineScope
) : AuthRepository {
    
    override val currentUser: StateFlow<User?> = firebaseAuth.authStateChanged
        .map { firebaseUser ->
            firebaseUser?.let {
                User(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName
                )
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
                Result.success(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName
                    )
                )
            } else {
                Result.failure(Exception("Sign in failed: No user returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun signUpWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password)
            val firebaseUser = result.user
            if (firebaseUser != null) {
                Result.success(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName
                    )
                )
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
                Result.success(
                    User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName
                    )
                )
            } else {
                Result.failure(Exception("Google sign in failed: No user returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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


