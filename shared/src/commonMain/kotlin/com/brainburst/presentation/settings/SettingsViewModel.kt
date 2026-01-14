package com.brainburst.presentation.settings

import com.brainburst.domain.model.User
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteAccountDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val notificationsEnabled: Boolean = false,  // Default false until push notifications are implemented
    val showChangePasswordSheet: Boolean = false,
    val isUpdatingPassword: Boolean = false,
    val passwordUpdateError: String? = null,
    val passwordUpdateSuccess: Boolean = false
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
            }
        }
    }
    
    fun onBackClick() {
        navigator.navigateTo(Screen.Home)
    }
    
    fun onEditProfileClick() {
        // TODO: Navigate to Edit Profile screen
    }
    
    fun onChangePasswordClick() {
        _uiState.value = _uiState.value.copy(
            showChangePasswordSheet = true,
            passwordUpdateError = null,
            passwordUpdateSuccess = false
        )
    }
    
    fun onChangePasswordDismiss() {
        _uiState.value = _uiState.value.copy(
            showChangePasswordSheet = false,
            passwordUpdateError = null,
            passwordUpdateSuccess = false,
            isUpdatingPassword = false
        )
    }
    
    fun onUpdatePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        // Validation
        if (currentPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(
                passwordUpdateError = "Please enter your current password"
            )
            return
        }
        
        if (newPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(
                passwordUpdateError = "Please enter a new password"
            )
            return
        }
        
        if (newPassword.length < 6) {
            _uiState.value = _uiState.value.copy(
                passwordUpdateError = "Password must be at least 6 characters"
            )
            return
        }
        
        if (newPassword != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                passwordUpdateError = "Passwords do not match"
            )
            return
        }
        
        if (currentPassword == newPassword) {
            _uiState.value = _uiState.value.copy(
                passwordUpdateError = "New password must be different from current password"
            )
            return
        }
        
        // Start password update
        _uiState.value = _uiState.value.copy(
            isUpdatingPassword = true,
            passwordUpdateError = null
        )
        
        viewModelScope.launch {
            val result = authRepository.updatePassword(currentPassword, newPassword)
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isUpdatingPassword = false,
                        passwordUpdateSuccess = true,
                        passwordUpdateError = null
                    )
                    // Auto-dismiss after short delay to show success
                    kotlinx.coroutines.delay(1500)
                    onChangePasswordDismiss()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingPassword = false,
                        passwordUpdateError = error.message ?: "Failed to update password"
                    )
                }
            )
        }
    }
    
    fun onNotificationsToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        // TODO: Save notification preference to local storage
        // TODO: Register/unregister for push notifications when implemented
    }
    
    fun onDeleteAccountClick() {
        // Show confirmation dialog first
        _uiState.value = _uiState.value.copy(
            showDeleteAccountDialog = true,
            errorMessage = null
        )
    }
    
    fun onDeleteAccountConfirm() {
        _uiState.value = _uiState.value.copy(
            showDeleteAccountDialog = false,
            isDeleting = true,
            errorMessage = null
        )
        
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            
            result.fold(
                onSuccess = {
                    // User will be automatically navigated to auth screen
                    // when currentUser becomes null
                    _uiState.value = SettingsUiState()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        errorMessage = error.message ?: "Failed to delete account"
                    )
                }
            )
        }
    }
    
    fun onDeleteAccountCancel() {
        _uiState.value = _uiState.value.copy(
            showDeleteAccountDialog = false,
            errorMessage = null
        )
    }
    
    fun onLogoutClick() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = authRepository.signOut()
            
            result.fold(
                onSuccess = {
                    // Navigation will be handled automatically when user becomes null
                    _uiState.value = SettingsUiState()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Logout failed"
                    )
                }
            )
        }
    }
}
