package com.brainburst.presentation.settings

import com.brainburst.domain.model.User
import com.brainburst.domain.notifications.NotificationManager
import com.brainburst.domain.repository.AuthRepository
import com.brainburst.domain.repository.PreferencesRepository
import com.brainburst.domain.share.ShareManager
import com.brainburst.presentation.navigation.Navigator
import com.brainburst.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteAccountDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val showChangePasswordSheet: Boolean = false,
    val isUpdatingPassword: Boolean = false,
    val passwordUpdateError: String? = null,
    val passwordUpdateSuccess: Boolean = false,
    val permissionDenied: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val isAwaitingPermissionGrant: Boolean = false  // Track if we're waiting for user to grant permission
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationManager: NotificationManager,
    private val shareManager: ShareManager,
    private val navigator: Navigator,
    private val viewModelScope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        observeAuthState()
        observeNotificationPreference()
    }
    
    /**
     * Check permission status when user returns to settings screen
     * (e.g., after granting permission in phone settings)
     */
    fun onScreenResumed() {
        viewModelScope.launch {
            val hasPermission = notificationManager.hasNotificationPermission()
            val prefsEnabled = preferencesRepository.getNotificationsEnabled().first()
            val isAwaitingPermission = _uiState.value.isAwaitingPermissionGrant
            
            // If we sent user to settings and they now have permission, enable notifications
            if (isAwaitingPermission && hasPermission) {
                preferencesRepository.setNotificationsEnabled(true)
                notificationManager.scheduleDailyNotifications()
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = true,
                    permissionDenied = false,
                    isAwaitingPermissionGrant = false
                )
            }
            // If user revoked permission in phone settings, disable notifications in app
            else if (!hasPermission && prefsEnabled) {
                preferencesRepository.setNotificationsEnabled(false)
                notificationManager.cancelDailyNotifications()
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = false,
                    permissionDenied = false,
                    isAwaitingPermissionGrant = false
                )
            }
            // Clear the awaiting flag if still set
            else if (isAwaitingPermission) {
                _uiState.value = _uiState.value.copy(
                    isAwaitingPermissionGrant = false
                )
            }
        }
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(user = user)
            }
        }
    }
    
    private fun observeNotificationPreference() {
        viewModelScope.launch {
            preferencesRepository.getNotificationsEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
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
        viewModelScope.launch {
            try {
                if (enabled) {
                    // Check if we have notification permission
                    val hasPermission = notificationManager.hasNotificationPermission()
                    
                    if (hasPermission) {
                        // Have permission, proceed with enabling
                        preferencesRepository.setNotificationsEnabled(true)
                        notificationManager.scheduleDailyNotifications()
                        _uiState.value = _uiState.value.copy(
                            notificationsEnabled = true,
                            permissionDenied = false,
                            isAwaitingPermissionGrant = false
                        )
                    } else {
                        // Need to request permission - open settings and mark that we're awaiting grant
                        _uiState.value = _uiState.value.copy(
                            notificationsEnabled = false,
                            permissionDenied = false,
                            isAwaitingPermissionGrant = true
                        )
                        notificationManager.requestNotificationPermission()
                    }
                } else {
                    // Disabling notifications
                    preferencesRepository.setNotificationsEnabled(false)
                    notificationManager.cancelDailyNotifications()
                    _uiState.value = _uiState.value.copy(
                        notificationsEnabled = false,
                        permissionDenied = false,
                        isAwaitingPermissionGrant = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Revert the toggle if there's an error
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = !enabled,
                    permissionDenied = false,
                    isAwaitingPermissionGrant = false
                )
            }
        }
    }
    
    fun onPermissionDialogDismissed() {
        _uiState.value = _uiState.value.copy(
            permissionDenied = false,
            notificationsEnabled = false
        )
    }
    
    fun onOpenNotificationSettings() {
        // This will need to be handled by platform-specific code
        // For now, just dismiss the dialog
        _uiState.value = _uiState.value.copy(
            permissionDenied = false,
            notificationsEnabled = false
        )
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
        // Show confirmation dialog first
        _uiState.value = _uiState.value.copy(
            showLogoutDialog = true,
            errorMessage = null
        )
    }
    
    fun onLogoutConfirm() {
        _uiState.value = _uiState.value.copy(
            showLogoutDialog = false,
            isLoading = true,
            errorMessage = null
        )
        
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
    
    fun onLogoutCancel() {
        _uiState.value = _uiState.value.copy(
            showLogoutDialog = false,
            errorMessage = null
        )
    }
    
    fun onShareAppClick() {
        shareManager.shareApp()
    }
}
