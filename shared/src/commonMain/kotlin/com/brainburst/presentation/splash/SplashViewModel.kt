package com.brainburst.presentation.splash

import com.brainburst.domain.repository.AuthRepository
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

            val isRegistered = authRepository.checkUserRegistration()
            
            navigator.navigateTo(if (isRegistered) Screen.Home else Screen.Auth)
            _isLoading.value = false
        }
    }
}




