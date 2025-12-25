package com.brainburst.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BrainBurst 🧠",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.user?.let { user ->
                            Text(
                                text = user.displayName ?: user.email ?: "User",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onLogoutClick() },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error message if logout fails
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = "Today's Puzzles",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "One shot per game, every day",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Game cards
            uiState.games.forEach { gameState ->
                GameCard(
                    gameState = gameState,
                    onClick = { viewModel.onGameClick(gameState.gameType) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ============================================================
            // ADMIN TOOLS (Development Only)
            // ============================================================
            // Uncomment the section below to show the admin upload button
            // Use this to quickly add test puzzles during development
            // ============================================================
            
            /*
            // Admin message (if any)
            if (uiState.adminMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.adminMessage!!.startsWith("✅"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.adminMessage!!,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { viewModel.clearAdminMessage() }) {
                            Text("OK")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ADMIN: Upload test puzzle button (for development only)
            OutlinedButton(
                onClick = { viewModel.uploadTestPuzzle() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔧 Upload Today's Test Puzzle")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "⚠️ Admin button: Tap once to add today's puzzle to Firestore",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            */
        }
    }
}

@Composable
fun GameCard(
    gameState: GameStateUI,
    onClick: () -> Unit
) {
    val isClickable = gameState is GameStateUI.Available || gameState is GameStateUI.Completed
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .then(
                if (isClickable) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = when (gameState) {
                is GameStateUI.Available -> MaterialTheme.colorScheme.primaryContainer
                is GameStateUI.Completed -> MaterialTheme.colorScheme.tertiaryContainer
                is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.surfaceVariant
                is GameStateUI.Loading -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = gameState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (gameState) {
                        is GameStateUI.Available -> MaterialTheme.colorScheme.onPrimaryContainer
                        is GameStateUI.Completed -> MaterialTheme.colorScheme.onTertiaryContainer
                        is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.onSurfaceVariant
                        is GameStateUI.Loading -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = gameState.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (gameState) {
                        is GameStateUI.Available -> MaterialTheme.colorScheme.onPrimaryContainer
                        is GameStateUI.Completed -> MaterialTheme.colorScheme.onTertiaryContainer
                        is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.onSurfaceVariant
                        is GameStateUI.Loading -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Completion time for completed games
                if (gameState is GameStateUI.Completed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Solved in ${gameState.completionTimeFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            // Status chip
            when (gameState) {
                is GameStateUI.Available -> {
                    AssistChip(
                        onClick = onClick,
                        label = { Text("Play Now") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                is GameStateUI.Completed -> {
                    AssistChip(
                        onClick = onClick,
                        label = { Text("View Results") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
                is GameStateUI.ComingSoon -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Coming Soon") },
                        modifier = Modifier.align(Alignment.TopEnd),
                        enabled = false
                    )
                }
                is GameStateUI.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

