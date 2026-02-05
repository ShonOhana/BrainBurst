package com.brainburst.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brainburst.shared.generated.resources.Res
import brainburst.shared.generated.resources.google
import brainburst.shared.generated.resources.sudoku_icon
import com.brainburst.platform.isAndroid
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // FocusRequesters for each field
    val lastNameFocusRequester = remember { FocusRequester() }
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 60.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // App Icon (3x3 grid)
            AppIcon()

            Spacer(modifier = Modifier.height(16.dp))

            // BrainBurst Title with Gradient
            GradientText(
                text = "BrainBurst",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle text
            if (uiState.isSignUpMode) {
                Text(
                    text = "Create account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666),
                    fontSize = 16.sp
                )
            } else {
                Text(
                    text = "Welcome Back!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF666666),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Fields in sign-up mode: First Name, Last Name, Email Address, Password
            if (uiState.isSignUpMode) {
                // First Name Field
                TextField(
                    value = uiState.firstName,
                    onValueChange = { viewModel.onFirstNameChanged(it) },
                    placeholder = {
                        Text(
                            "First Name",
                            color = Color(0xFF999999)
                        )
                    },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { lastNameFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Last Name Field
                TextField(
                    value = uiState.lastName,
                    onValueChange = { viewModel.onLastNameChanged(it) },
                    placeholder = {
                        Text(
                            "Last Name",
                            color = Color(0xFF999999)
                        )
                    },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { emailFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .focusRequester(lastNameFocusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Address Field (sign-up mode)
                TextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    placeholder = {
                        Text(
                            "Email Address",
                            color = Color(0xFF999999)
                        )
                    },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .focusRequester(emailFocusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Username/Email Field (sign-in mode)
                TextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    placeholder = {
                        Text(
                            "Email address",
                            color = Color(0xFF999999)
                        )
                    },
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Password Field
            TextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                placeholder = {
                    Text(
                        "Password",
                        color = Color(0xFF999999)
                    )
                },
                enabled = !uiState.isLoading,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        viewModel.onSignInClick()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .focusRequester(passwordFocusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error Message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start Playing Button with Gradient
            GradientButton(
                text = "Start Playing",
                onClick = { viewModel.onSignInClick() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                isLoading = uiState.isLoading
            )

            // Create Account Button, Or separator, and Google button (only in sign-in mode)
            Spacer(modifier = Modifier.height(16.dp))

            // Create Account Button
            Button(
                onClick = { viewModel.toggleSignUpMode() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (uiState.isSignUpMode) "Sign in" else "Create Account",
                    color = Color(0xFF333333),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Or separator
            Text(
                text = "Or",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Continue With Google Button
            Button(
                onClick = { viewModel.onGoogleSignInClick() },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Logo (simplified - using colored circles)
                    GoogleLogo()
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue With Google",
                        color = Color(0xFF333333),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(80.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // 3x3 grid of purple squares
        Column(
            modifier = Modifier.size(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF6750A4))
                )
            }
        }
    }
}

@Composable
fun GradientText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    // Split text and apply gradient colors
    Row(modifier = modifier) {
        Text(
            text = "Brain",
            style = style,
            color = Color(0xFF6750A4) // Purple
        )
        Text(
            text = "Burst",
            style = style,
            color = Color(0xFF4285F4) // Blue
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val gradientColors = listOf(
        Color(0xFF6750A4), // Purple
        Color(0xFF4285F4)  // Blue
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = gradientColors
                )
            )
            .then(
                if (!enabled) {
                    Modifier.alpha(0.6f)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    if (!isAndroid) return
    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(30.dp),
            painter = painterResource(Res.drawable.google),
            contentDescription = "Description"
        )
    }
}


