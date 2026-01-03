package com.brainburst.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import brainburst.shared.generated.resources.Res
import brainburst.shared.generated.resources.sudoku_icon
import brainburst.shared.generated.resources.tango_icon
import brainburst.shared.generated.resources.zip_icon
import com.brainburst.domain.model.GameType
import org.jetbrains.compose.resources.painterResource

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

    // Custom colors for completed state
    val completedCardBackground = Color(0xFF2D2A34)
    val completedCardBorder = Color(0xFF884CE9)
    val leaderboardButtonColor = Color(0xFF884CE9)

    val cardColor = when (gameState) {
        is GameStateUI.Available -> MaterialTheme.colorScheme.primaryContainer
        is GameStateUI.Completed -> completedCardBackground
        is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.surfaceVariant
        is GameStateUI.Loading -> MaterialTheme.colorScheme.surface
    }
    val textColor = when (gameState) {
        is GameStateUI.Available -> MaterialTheme.colorScheme.onPrimaryContainer
        is GameStateUI.Completed -> Color(0xFFE0D8E8) // Light pink/lavender for completed
        is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.onSurfaceVariant
        is GameStateUI.Loading -> MaterialTheme.colorScheme.onSurface
    }

    // Add border for completed state
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (gameState is GameStateUI.Completed) {
                Modifier.border(1.dp, completedCardBorder, MaterialTheme.shapes.medium)
            } else {
                Modifier
            }
        )
        .then(
            if (isClickable) Modifier.clickable(onClick = onClick)
            else Modifier
        )

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        // Card content - vertically centered
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Icon and text
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show icon for completed state
//                GameIcon(
//                    gameType = gameState.gameType,
//                    modifier = Modifier.size(48.dp)
//                )
                Spacer(modifier = Modifier.width(12.dp))

                // Text content
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title
                    Text(
                        text = gameState.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    // Date (only for Available state)
                    if (gameState is GameStateUI.Available && gameState.formattedDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = gameState.formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Right side: Buttons - take up right half of card
            when (gameState) {
                is GameStateUI.Available -> {
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play Now")
                        }
                    }
                }

                is GameStateUI.Completed -> {
                    // Calculate and display time until next 8 UTC
                    var timeUntil8UTC by remember { mutableStateOf(calculateTimeUntil8UTC()) }

                    // Update every minute
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(60_000) // Update every minute
                            timeUntil8UTC = calculateTimeUntil8UTC()
                        }
                    }

                    // Two stacked buttons for completed state - right half of card
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Top button: Time until next puzzle (disabled)
                        Button(
                            onClick = { /* Disabled */ },
                            modifier = Modifier
                                .height(36.dp)
                                .fillMaxWidth(),
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5C5C5C), // Dark gray
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF5C5C5C),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timeUntil8UTC,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Bottom button: Leaderboard - same size as top button
                        Button(
                            onClick = onClick,
                            modifier = Modifier
                                .height(36.dp)
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = leaderboardButtonColor,
                                contentColor = Color.White
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(0xFF5E35B1) // Dark purple for icon
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LeaderBoard",
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                Image(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(Res.drawable.sudoku_icon),
                    contentDescription = "Description"
                )
            }

            GameType.ZIP -> {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.zip_icon),
                    contentDescription = "Description"
                )
            }

            GameType.TANGO -> {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.tango_icon),
                    contentDescription = "Description"
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

/**
 * Calculate time remaining until next 8:00 UTC
 * Returns formatted string like "2h 30m", "45m", "5h 15m", etc.
 */
fun calculateTimeUntil8UTC(): String {
    val now = Clock.System.now()
    val nowMillis = now.toEpochMilliseconds()

    // Get current time components in UTC
    // Calculate milliseconds since start of day (midnight UTC)
    val millisecondsInDay = 24 * 60 * 60 * 1000
    val millisecondsSinceMidnight = nowMillis % millisecondsInDay

    // Calculate milliseconds for 8:00 UTC today
    val eightHoursInMillis = 8 * 60 * 60 * 1000

    // Calculate target time: if current time is before 8:00 UTC, target is today 8:00
    // Otherwise, target is tomorrow 8:00 UTC
    val target8UTCMillis = if (millisecondsSinceMidnight < eightHoursInMillis) {
        // Target is today at 8:00 UTC
        (nowMillis / millisecondsInDay) * millisecondsInDay + eightHoursInMillis
    } else {
        // Target is tomorrow at 8:00 UTC
        ((nowMillis / millisecondsInDay) + 1) * millisecondsInDay + eightHoursInMillis
    }

    // Calculate difference
    val diffMillis = target8UTCMillis - nowMillis

    if (diffMillis <= 0) {
        return "Available now"
    }

    val totalMinutes = diffMillis / (60 * 1000)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return buildString {
        if (hours > 0) {
            append("${hours}h")
            if (minutes > 0) {
                append(" ${minutes}m")
            }
        } else {
            append("${minutes}m")
        }
    }
}

