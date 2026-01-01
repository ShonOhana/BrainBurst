package com.brainburst.presentation.auth

import com.brainburst.domain.auth.GoogleSignInProvider
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSignUpMode: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleSignInProvider: GoogleSignInProvider,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }
    
    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }
    
    fun onFirstNameChanged(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName, errorMessage = null)
    }
    
    fun onLastNameChanged(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName, errorMessage = null)
    }
    
    fun toggleSignUpMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode,
            errorMessage = null
        )
    }
    
    fun onSignInClick() {
        val currentState = _uiState.value
        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "Please fill in all fields")
            return
        }
        
        // For sign-up, require firstName and lastName
        if (currentState.isSignUpMode) {
            if (currentState.firstName.isBlank() || currentState.lastName.isBlank()) {
                _uiState.value = currentState.copy(errorMessage = "Please enter your first and last name")
                return
            }
        }
        
        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = if (currentState.isSignUpMode) {
                authRepository.signUpWithEmail(
                    currentState.email, 
                    currentState.password,
                    currentState.firstName,
                    currentState.lastName
                )
            } else {
                authRepository.signInWithEmail(currentState.email, currentState.password)
            }
            
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState() // Reset state
                    navigator.navigateTo(Screen.Home)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Authentication failed"
                    )
                }
            )
        }
    }
    
    fun onGoogleSignInClick() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                // Get the Google ID token from the platform-specific provider
                val idToken = googleSignInProvider.signIn()
                
                // Use the ID token to sign in with Firebase
                val result = authRepository.signInWithGoogleToken(idToken)
                
                result.fold(
                    onSuccess = {
                        _uiState.value = AuthUiState() // Reset state
                        navigator.navigateTo(Screen.Home)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Google Sign-In failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Google Sign-In failed"
                )
            }
        }
    }
}

