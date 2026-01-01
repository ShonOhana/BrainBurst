package com.brainburst.presentation.splash

import com.brainburst.domain.repository.AuthRepository
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            viewModelScope.launch {
                // Skip the initial null value from StateFlow, wait for first real emission from Firebase
                val user = authRepository.currentUser
                    .drop(1)  // Skip the initial null value
                    .first() // Wait for the first real emission

                // Navigate based on actual auth state
                if (user != null) {
                    navigator.navigateTo(Screen.Home)
                } else {
                    navigator.navigateTo(Screen.Auth)
                }

                // Hide loading after navigation
                _isLoading.value = false
            }
        }
    }
}




