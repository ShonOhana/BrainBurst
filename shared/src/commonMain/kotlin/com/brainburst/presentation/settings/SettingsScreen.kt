package com.brainburst.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    val gradientPurple = Color(0xFF6C3CE3)
    val gradientBlue = Color(0xFF5B42F3)
    val backgroundColor = Color(0xFFF5F3FF)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Settings title
            Text(
                text = "Settings",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF6C3CE3),
                            Color(0xFF5B42F3)
                        )
                    )
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            )
            
            // User profile card with gradient background
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(gradientPurple, gradientBlue)
                            ),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User initials circle
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getInitials(uiState.user?.displayName, uiState.user?.email),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // User name and email
                            Column {
                                Text(
                                    text = uiState.user?.displayName ?: "User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = uiState.user?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        
                        // Grid icon
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Account Settings section
            Text(
                text = "Account Settings",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Change Password
            SettingsMenuItem(
                icon = Icons.Default.Lock,
                iconColor = Color(0xFF9810FA),
                title = "Change Password",
                subtitle = "Update your password",
                onClick = { viewModel.onChangePasswordClick() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Preferences section
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Notifications
            SettingsMenuItemWithSwitch(
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFF9810FA),
                title = "Notifications",
                subtitle = "Enable push notifications",
                checked = uiState.notificationsEnabled,
                onCheckedChange = { viewModel.onNotificationsToggle(it) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Delete Account
            SettingsMenuItem(
                icon = Icons.Default.Delete,
                iconColor = Color.Red,
                title = "Delete Account",
                subtitle = "Permanently delete your account",
                onClick = { viewModel.onDeleteAccountClick() },
                showLoading = uiState.isDeleting
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Log Out
            SettingsMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                iconColor = Color(0xFFFF3B30),
                title = "Log Out",
                subtitle = null,
                onClick = { viewModel.onLogoutClick() },
                showLoading = uiState.isLoading
            )
            
            // Error message if logout fails
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Delete Account Confirmation Dialog
    if (uiState.showDeleteAccountDialog) {
        DeleteAccountConfirmationDialog(
            onConfirm = { viewModel.onDeleteAccountConfirm() },
            onDismiss = { viewModel.onDeleteAccountCancel() }
        )
    }
    
    // Change Password Bottom Sheet
    if (uiState.showChangePasswordSheet) {
        ChangePasswordBottomSheet(
            isLoading = uiState.isUpdatingPassword,
            errorMessage = uiState.passwordUpdateError,
            isSuccess = uiState.passwordUpdateSuccess,
            onUpdatePassword = { current, new, confirm ->
                viewModel.onUpdatePassword(current, new, confirm)
            },
            onDismiss = { viewModel.onChangePasswordDismiss() }
        )
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    showLoading: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(enabled = !showLoading, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Title and subtitle
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (iconColor == Color(0xFFFF3B30)) iconColor else Color.Black
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Arrow or loading indicator
            if (showLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsMenuItemWithSwitch(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon with background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Title and subtitle
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Switch
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF9810FA),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.LightGray
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordBottomSheet(
    isLoading: Boolean,
    errorMessage: String?,
    isSuccess: Boolean,
    onUpdatePassword: (currentPassword: String, newPassword: String, confirmPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = "Enter your current password and choose a new one",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Current Password
            Text(
                text = "Current Password",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading && !isSuccess,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF9810FA),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // New Password
            Text(
                text = "New Password",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading && !isSuccess,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF9810FA),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm New Password
            Text(
                text = "Confirm New Password",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading && !isSuccess,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF9810FA),
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF3B30),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Success message
            if (isSuccess) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "✓ Password updated successfully!",
                    color = Color(0xFF34C759),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && !isSuccess
                ) {
                    Text("Cancel")
                }
                
                // Update Password button
                Button(
                    onClick = {
                        onUpdatePassword(currentPassword, newPassword, confirmPassword)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && !isSuccess,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9810FA),
                        disabledContainerColor = Color(0xFF9810FA).copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Update Password")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAccountConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color(0xFFFF3B30),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Account?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "This action cannot be undone. All your data will be permanently deleted, including:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "• Your profile information",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• All your game results and scores",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Your leaderboard rankings",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Are you sure you want to continue?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B30)
                )
            ) {
                Text("Delete Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getInitials(displayName: String?, email: String?): String {
    return when {
        !displayName.isNullOrBlank() -> {
            val parts = displayName.trim().split(" ")
            if (parts.size >= 2) {
                "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            } else {
                displayName.take(2).uppercase()
            }
        }
        !email.isNullOrBlank() -> {
            email.take(2).uppercase()
        }
        else -> "U"
    }
}
