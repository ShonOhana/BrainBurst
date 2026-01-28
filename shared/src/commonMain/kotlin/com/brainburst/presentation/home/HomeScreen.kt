package com.brainburst.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import brainburst.shared.generated.resources.Res
import brainburst.shared.generated.resources.leaderboard
import brainburst.shared.generated.resources.sudoku_icon
import brainburst.shared.generated.resources.tango_icon
import brainburst.shared.generated.resources.zip_icon
import com.brainburst.domain.model.GameType
import com.brainburst.platform.isAndroid
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    uiState.user?.let { user ->
                        Text(
                            text = ("Hey " + user.displayName?.split(" ")?.first() + "!" ?: user.email ?: "User"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onSettingsClick() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today's Puzzles",
                style = TextStyle(
                    fontSize = MaterialTheme.typography.displaySmall.fontSize,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF9810FA),
                            Color(0xFF155DFC)
                        )
                    )
                ),
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "One shot per game, every day",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Gradient colors for Available state (only for SUDOKU)
            val gradientPurple = Color(0xFF9810FA)
            val gradientBlue = Color(0xFF155DFC)
            // Use large rounded corners for SUDOKU (both Available and Completed states)
            val cardShape = MaterialTheme.shapes.extraLarge

            val m = Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(gradientPurple, gradientBlue)
            ),
            shape = cardShape
            )
            // Game cards
            uiState.games.forEach { gameState ->
                GameCard(
                    modifier = m,
                    gameState = gameState,
                    isRefreshing = uiState.isRefreshing,
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
    modifier: Modifier,
    gameState: GameStateUI,
    isRefreshing: Boolean = false,
    onClick: () -> Unit
) {
    val isClickable = (gameState is GameStateUI.Available || gameState is GameStateUI.Completed) && !isRefreshing

    // Custom colors for completed state
    val completedCardBackground = Color(0xFF2D2A34)
    val completedCardBorder = Color(0xFF884CE9)
    val leaderboardButtonColor = Color.White

    // Determine if this is a SUDOKU or ZIP game that should have gradient
    val isGradientGame = gameState.gameType == GameType.MINI_SUDOKU_6X6 || gameState.gameType == GameType.ZIP
    val shouldUseGradient = isGradientGame && (gameState is GameStateUI.Available || gameState is GameStateUI.Completed)

    val cardColor = when (gameState) {
        is GameStateUI.Available -> {
            if (isGradientGame) Color.Transparent // Will use gradient instead
            else Color.White // White background for TANGO
        }
        is GameStateUI.Completed -> {
            if (isGradientGame) Color.Transparent // Will use gradient instead
            else Color.White // White background for TANGO
        }
        is GameStateUI.ComingSoon -> Color.White
        is GameStateUI.Loading -> Color.White
    }
    val textColor = when (gameState) {
        is GameStateUI.Available -> {
            if (isGradientGame) Color.White // White text for gradient background
            else Color.Black // Black text for white background
        }
        is GameStateUI.Completed -> {
            if (isGradientGame) Color.White // White text for gradient background
            else Color.Black // Black text for white background
        }
        is GameStateUI.ComingSoon -> MaterialTheme.colorScheme.onSurfaceVariant
        is GameStateUI.Loading -> MaterialTheme.colorScheme.onSurface
    }

    // Add border for completed state (only for non-gradient games)
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (gameState is GameStateUI.Completed && !isGradientGame) {
                Modifier.border(1.dp, completedCardBorder, MaterialTheme.shapes.medium)
            } else {
                Modifier
            }
        )
        .then(
            if (isClickable) Modifier.clickable(onClick = onClick)
            else Modifier
        )

    // Use large rounded corners for gradient games (both Available and Completed states)
    val cardShape = if (shouldUseGradient) {
        MaterialTheme.shapes.extraLarge
    } else {
        MaterialTheme.shapes.medium
    }

    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = if (shouldUseGradient) {
            CardDefaults.cardColors(containerColor = Color.Transparent)
        } else {
            CardDefaults.cardColors(containerColor = cardColor)
        }
    ) {
        Box(
            modifier = if (shouldUseGradient) modifier.fillMaxWidth().padding(vertical = 12.dp) else Modifier.fillMaxWidth()
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
                    // Add GameIcon on Android only
                    if (isAndroid) {
                        GameIcon(
                            gameType = gameState.gameType,
                            tintColor = if (shouldUseGradient) Color.White else null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    
                    // Text content
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (gameState is GameStateUI.Available && isGradientGame) {
                            // For Available gradient games (SUDOKU and ZIP)
                            // Show title as single line, centered vertically
                            Text(
                                text = if (gameState.gameType == GameType.MINI_SUDOKU_6X6) "Mini Sudoku" else gameState.title,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = MaterialTheme.typography.headlineMedium.fontSize * 0.85f
                                ),
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            // Title for other states or non-gradient games
                            Text(
                                modifier = if (shouldUseGradient) Modifier else Modifier.alpha(0.5f),
                                text = if (gameState.gameType == GameType.MINI_SUDOKU_6X6) "Mini Sudoku" else gameState.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = MaterialTheme.typography.headlineSmall.fontSize * 0.85f
                                ),
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Date (for Available and Completed states)
                        if (gameState is GameStateUI.Available && gameState.formattedDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = gameState.formattedDate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9f
                                ),
                                color = textColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right side: Buttons - take up right half of card
                when (gameState) {
                    is GameStateUI.Available -> {
                        // Two stacked buttons for available state - right half of card
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Top button: Play now
                            Button(
                                onClick = onClick,
                                enabled = !isRefreshing,
                                modifier = Modifier
                                    .height(51.dp)
                                    .fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8E8E8), // Light gray/off-white
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0xFFE8E8E8).copy(alpha = 0.5f),
                                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                                ),
                                shape = MaterialTheme.shapes.extraLarge // Pill shape
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.Black.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        Text(
                                            text = "Play now",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            maxLines = 1,
                                            modifier = Modifier.align(Alignment.CenterStart)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .align(Alignment.CenterEnd),
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }

                            // Bottom button: Leaderboard (disabled)
                            Button(
                                onClick = {},
                                modifier = Modifier
                                    .height(51.dp)
                                    .fillMaxWidth(),
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = leaderboardButtonColor,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.White.copy(alpha = 0.6f),
                                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Results",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                    Image(
                                        modifier = Modifier.size(20.dp).align(Alignment.CenterEnd),
                                        painter = painterResource(Res.drawable.leaderboard),
                                        contentDescription = "leaderboard",
                                        colorFilter = ColorFilter.tint(Color.Black)
                                    )
                                }
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
                                    .height(51.dp)
                                    .fillMaxWidth(),
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5C5C5C), // Dark gray
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color.White.copy(alpha = 0.6f),
                                    disabledContentColor = Color.Black
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = timeUntil8UTC,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .alpha(0.3f)
                                            .align(Alignment.CenterStart)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .alpha(0.3f)
                                            .align(Alignment.CenterEnd),
                                        tint = Color.Unspecified
                                    )
                                }
                            }

                            // Bottom button: Leaderboard (enabled)
                            Button(
                                onClick = onClick,
                                modifier = Modifier
                                    .height(51.dp)
                                    .fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = leaderboardButtonColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Results",
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.align(Alignment.CenterStart)
                                    )
                                    Image(
                                        modifier = Modifier.size(20.dp).align(Alignment.CenterEnd),
                                        painter = painterResource(Res.drawable.leaderboard),
                                        contentDescription = "leaderboard",
                                        colorFilter = ColorFilter.tint(Color.Black)
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
}

@Composable
fun GameIcon(
    gameType: GameType,
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (gameType) {
            GameType.MINI_SUDOKU_6X6 -> {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.sudoku_icon),
                    contentDescription = "Description",
                    colorFilter = tintColor?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) }
                )
            }

            GameType.ZIP -> {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.zip_icon),
                    contentDescription = "Description",
                    colorFilter = tintColor?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) }
                )
            }

            GameType.TANGO -> {
                Image(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.tango_icon),
                    contentDescription = "Description",
                    colorFilter = tintColor?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) }
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

