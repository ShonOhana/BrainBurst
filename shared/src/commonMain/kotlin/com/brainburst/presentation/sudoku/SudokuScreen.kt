package com.brainburst.presentation.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainburst.domain.game.Position
import com.brainburst.platform.PlatformLifecycleHandler
import com.brainburst.presentation.ads.BannerAdView
import kotlinx.coroutines.delay

// Helper function to count number occurrences on the board
private fun countNumberOccurrences(board: List<List<Int>>): Map<Int, Int> {
    val counts = mutableMapOf<Int, Int>()
    for (row in board) {
        for (value in row) {
            if (value != 0) { // Don't count empty cells
                counts[value] = (counts[value] ?: 0) + 1
            }
        }
    }
    return counts
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(viewModel: SudokuViewModel, adManager: com.brainburst.domain.ads.AdManager) {
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    
    // Lifecycle: Handle app background/foreground
    PlatformLifecycleHandler(
        onPause = {
            viewModel.onAppPaused()
        },
        onResume = {
            viewModel.onAppResumed()
        }
    )
    
    // Handle initial screen visibility - start timer when screen first appears
    LaunchedEffect(Unit) {
        // Start timer when screen first appears if puzzle is loaded
        if (uiState.board.isNotEmpty()) {
            viewModel.onAppResumed()
        }
    }
    
    // Handle events
    LaunchedEffect(event) {
        when (val e = event) {
            is SudokuEvent.PuzzleCompleted -> {
                // Navigation to leaderboard is handled in ViewModel
                // Clear the event after handling
                viewModel.clearEvent()
            }
            is SudokuEvent.ValidationError -> {
                // Snackbar will show the error
            }
            null -> {}
        }
    }
    
    // Purple gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8D5FF),
                        Color(0xFFD8BBFF)
                    )
                )
            )
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.onBackPress() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            // Use BoxWithConstraints to get available space
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val maxHeight = maxHeight
                val maxWidth = maxWidth
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Back button - more compact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = { viewModel.onBackPress() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF6200EA),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Title - smaller with gradient
                    Text(
                        text = "Mini Sudoku",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF9810FA),
                                    Color(0xFF155DFC)
                                )
                            )
                        )
                    )
                    
                    // Subtitle - smaller
                    Text(
                        text = "6×6 Brain Puzzle",
                        fontSize = 12.sp,
                        color = Color(0xFF6200EA).copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Stats Card - more compact
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Time
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = uiState.elapsedTimeFormatted,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6200EA)
                                )
                                Text(
                                    text = "Time",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                            
                            Divider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp),
                                color = Color(0xFFE0E0E0)
                            )
                            
                            // Moves
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = uiState.movesCount.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6200EA)
                                )
                                Text(
                                    text = "Moves",
                                    fontSize = 10.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Sudoku board - responsive size with height constraint
                    val boardWidth = maxWidth * 0.92f
                    val maxBoardHeight = maxHeight * 0.45f
                    SudokuBoard(
                        board = uiState.board,
                        fixedCells = uiState.fixedCells,
                        selectedPosition = uiState.selectedPosition,
                        invalidPositions = uiState.invalidPositions,
                        onCellClick = { viewModel.onCellClick(it) },
                        boardWidth = boardWidth,
                        maxBoardHeight = maxBoardHeight
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Auto-submit when puzzle is complete
                    LaunchedEffect(uiState.isComplete) {
                        if (uiState.isComplete) {
                            // Wait 500ms before submitting
                            delay(500)
                            viewModel.onSubmit()
                        }
                    }
                    
                    // Calculate which numbers are fully placed (appear 6 times)
                    val numberCounts = remember(uiState.board) {
                        countNumberOccurrences(uiState.board)
                    }
                    val fullyPlacedNumbers = remember(numberCounts) {
                        (1..6).filter { (numberCounts[it] ?: 0) >= 6 }.toSet()
                    }
                    
                    // Number pad - more compact
                    NumberPad(
                        onNumberClick = { viewModel.onNumberPress(it) },
                        onEraseClick = { viewModel.onErasePress() },
                        onHintClick = { viewModel.onHintPress() },
                        onSubmit = { viewModel.onSubmit() },
                        isComplete = uiState.isComplete,
                        disabledNumbers = fullyPlacedNumbers,
                        isHintOnCooldown = uiState.isHintOnCooldown,
                        hintCooldownProgress = uiState.hintCooldownProgress
                    )
                    
                    // Banner ad at bottom
                    Spacer(modifier = Modifier.height(8.dp))
                    BannerAdView(adManager = adManager)
                }
            }
            
            // Full-screen loader overlay when submitting
            if (uiState.isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Submitting your result...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading leaderboard",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Snackbar for errors
            if (event is SudokuEvent.ValidationError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        action = {
                            TextButton(onClick = { viewModel.clearEvent() }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text((event as SudokuEvent.ValidationError).message)
                    }
                }
            }
        }
    }
}

@Composable
fun SudokuBoard(
    board: List<List<Int>>,
    fixedCells: Set<Position>,
    selectedPosition: Position?,
    invalidPositions: List<Position>,
    onCellClick: (Position) -> Unit,
    boardWidth: Dp,
    maxBoardHeight: Dp
) {
    if (board.isEmpty()) return
    
    val size = board.size
    // Calculate cell dimensions - width matches board, height constrained by available space
    val cellWidth = (boardWidth.value / size).dp - 2.dp
    val idealCellHeight = cellWidth * 1.08f // 8% taller for better visibility
    val maxCellHeight = (maxBoardHeight.value / size).dp - 2.dp
    val cellHeight = minOf(idealCellHeight.value, maxCellHeight.value).dp
    
    Card(
        modifier = Modifier.wrapContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            // Custom border with thicker top
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 2.dp,
                        color = Color(0xFF101828),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            
            // Board content
            Column {
                for (row in 0 until size) {
                    Row {
                        for (col in 0 until size) {
                            val position = Position(row, col)
                            val value = board[row][col]
                            val isFixed = position in fixedCells
                            val isSelected = position == selectedPosition
                            val isInvalid = position in invalidPositions
                            
                            // Determine border widths based on position
                            val rightBorderWidth = when {
                                col == size - 1 -> 0.dp // No border on last column
                                col % 3 == 2 -> 3.dp // Thicker border every 3 columns
                                else -> 1.dp // Thin border
                            }
                            
                            val bottomBorderWidth = when {
                                row == size - 1 -> 0.dp // No border on last row
                                row % 2 == 1 -> 3.dp // Thicker border every 2 rows
                                else -> 1.dp // Thin border
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(cellHeight)
                                    .background(
                                        when {
                                            isInvalid -> Color(0xFFFFCDD2)
                                            isSelected -> Color(0xFFBBDEFB)
                                            else -> Color.White
                                        }
                                    )
                                    .border(
                                        width = 0.dp,
                                        color = Color.Transparent
                                    )
                                    .clickable(enabled = !isFixed) {
                                        onCellClick(position)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Cell content - responsive font size
                                if (value != 0) {
                                    Text(
                                        text = value.toString(),
                                        fontSize = (cellWidth.value * 0.5f).sp,
                                        fontWeight = if (isFixed) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isInvalid -> Color(0xFFD32F2F)
                                            isFixed -> Color(0xFF1A1A1A)
                                            else -> Color(0xFF6200EA)
                                        }
                                    )
                                }
                                
                                // Right border
                                if (rightBorderWidth > 0.dp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(rightBorderWidth)
                                            .background(
                                                if (rightBorderWidth > 1.dp) Color(0xFF667085) 
                                                else Color(0xFFD1D5DC)
                                            )
                                            .align(Alignment.CenterEnd)
                                    )
                                }
                                
                                // Bottom border
                                if (bottomBorderWidth > 0.dp) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(bottomBorderWidth)
                                            .background(
                                                if (bottomBorderWidth > 1.dp) Color(0xFF667085) 
                                                else Color(0xFFD1D5DC)
                                            )
                                            .align(Alignment.BottomCenter)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPad(
    onNumberClick: (Int) -> Unit,
    onEraseClick: () -> Unit,
    onHintClick: () -> Unit,
    onSubmit: () -> Unit,
    isComplete: Boolean,
    disabledNumbers: Set<Int> = emptySet(),
    isHintOnCooldown: Boolean = false,
    hintCooldownProgress: Float = 0f
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // First row: 1-3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (number in 1..3) {
                Button(
                    onClick = { onNumberClick(number) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isComplete && number !in disabledNumbers
                ) {
                    Text(
                        text = number.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Second row: 4-6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (number in 4..6) {
                Button(
                    onClick = { onNumberClick(number) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isComplete && number !in disabledNumbers
                ) {
                    Text(
                        text = number.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Clear and Hint buttons in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Hint button with cooldown animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Button(
                    onClick = onHintClick,
                    modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9810FA),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF9810FA).copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isComplete && !isHintOnCooldown
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hint",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Cooldown progress overlay
                if (isHintOnCooldown) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hintCooldownProgress)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
            
            // Clear button
            Button(
                onClick = onEraseClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isComplete
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clear",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Success message when puzzle is complete (instead of submit button)
        if (isComplete) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎉",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Puzzle Complete!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

