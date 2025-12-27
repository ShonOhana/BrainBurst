package com.brainburst.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brainburst.domain.model.GameType

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
    val cardColor = when (gameState) {
        is GameStateUI.Available -> MaterialTheme.colorScheme.primaryContainer
        is GameStateUI.Completed -> MaterialTheme.colorScheme.tertiaryContainer
        is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.surfaceVariant
        is GameStateUI.Loading -> MaterialTheme.colorScheme.surface
    }
    val textColor = when (gameState) {
        is GameStateUI.Available -> MaterialTheme.colorScheme.onPrimaryContainer
        is GameStateUI.Completed -> MaterialTheme.colorScheme.onTertiaryContainer
        is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.onSurfaceVariant
        is GameStateUI.Loading -> MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        // Card content - vertically centered
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                // Text content on the left
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title
                    Text(
                        text = gameState.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    
                    // Date (for Available and Completed states)
                    if (gameState is GameStateUI.Available && gameState.formattedDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = gameState.formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    } else if (gameState is GameStateUI.Completed && gameState.formattedDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = gameState.formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Button on the right
                when (gameState) {
                    is GameStateUI.Available -> {
                        // If today's puzzle doesn't exist, show "Results" instead of "Play Now"
                        val buttonText = if (gameState.hasTodayPuzzle) "Play Now" else "Results"
                        val buttonIcon = if (gameState.hasTodayPuzzle) Icons.Default.PlayArrow else Icons.Default.CheckCircle
                        
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = buttonIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(buttonText)
                        }
                    }
                    is GameStateUI.Completed -> {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Results")
                        }
                    }
                    is GameStateUI.ComingSoon -> {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.height(36.dp),
                            enabled = false
                        ) {
                            Text("Coming Soon")
                        }
                    }
                    is GameStateUI.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
    }
}

@Composable
fun GameIcon(
    gameType: GameType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (gameType) {
            GameType.MINI_SUDOKU_6X6 -> {
                SudokuGridIcon(modifier = Modifier.fillMaxSize())
            }
            GameType.ZIP -> {
                // Placeholder for Zip icon - can be implemented later
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                )
            }
            GameType.TANGO -> {
                // Placeholder for Tango icon - can be implemented later
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
fun SudokuGridIcon(
    modifier: Modifier = Modifier
) {
    val gridColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        // Background with grid pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = size.width * 0.15f
            val cellSize = (size.width - padding * 2) / 3f
            val startX = padding
            val startY = padding
            
            val lineWidth = 1.5.dp.toPx()
            val thickLineWidth = 3.dp.toPx()
            
            // Draw grid lines - 3x3 grid
            // Vertical lines
            for (i in 0..3) {
                val x = startX + i * cellSize
                val lineWidthToUse = if (i == 0 || i == 3) thickLineWidth else lineWidth
                drawLine(
                    color = gridColor,
                    start = Offset(x, startY),
                    end = Offset(x, startY + cellSize * 3),
                    strokeWidth = lineWidthToUse
                )
            }
            
            // Horizontal lines
            for (i in 0..3) {
                val y = startY + i * cellSize
                val lineWidthToUse = if (i == 0 || i == 3) thickLineWidth else lineWidth
                drawLine(
                    color = gridColor,
                    start = Offset(startX, y),
                    end = Offset(startX + cellSize * 3, y),
                    strokeWidth = lineWidthToUse
                )
            }
            
            // Draw some dots to represent numbers in cells
            val dotPositions = listOf(
                Offset(startX + cellSize * 0.5f, startY + cellSize * 0.5f),
                Offset(startX + cellSize * 1.5f, startY + cellSize * 0.5f),
                Offset(startX + cellSize * 0.5f, startY + cellSize * 1.5f),
                Offset(startX + cellSize * 2.5f, startY + cellSize * 1.5f),
                Offset(startX + cellSize * 1.5f, startY + cellSize * 2.5f)
            )
            
            dotPositions.forEach { position ->
                drawCircle(
                    color = gridColor.copy(alpha = 1f),
                    radius = cellSize * 0.25f,
                    center = position
                )
            }
        }
        
        // Overlay a checkmark circle in the center (like LinkedIn style)
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    Color(0xFF4CAF50), // Green color for checkmark
                    MaterialTheme.shapes.extraLarge
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White
            )
        }
    }
}

