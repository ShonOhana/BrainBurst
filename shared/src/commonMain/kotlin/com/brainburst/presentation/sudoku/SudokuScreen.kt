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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brainburst.domain.game.Position
import com.brainburst.platform.PlatformLifecycleHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(viewModel: SudokuViewModel) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = { viewModel.onBackPress() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF6200EA)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Title
                Text(
                    text = "Mini Sudoku",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6200EA)
                )
                
                // Subtitle
                Text(
                    text = "6×6 Brain Puzzle",
                    fontSize = 16.sp,
                    color = Color(0xFF6200EA).copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Clock icon using Unicode
                            Text(
                                text = "🕐",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Time",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                                Text(
                                    text = uiState.elapsedTimeFormatted,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                        }
                        
                        // Divider
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .width(1.dp)
                                .background(Color(0xFFE0E0E0))
                        )
                        
                        // Moves
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "#",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6200EA)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Moves",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                                Text(
                                    text = uiState.movesCount.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sudoku board
                SudokuBoard(
                    board = uiState.board,
                    fixedCells = uiState.fixedCells,
                    selectedPosition = uiState.selectedPosition,
                    invalidPositions = uiState.invalidPositions,
                    onCellClick = { viewModel.onCellClick(it) }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Number pad
                NumberPad(
                    onNumberClick = { viewModel.onNumberPress(it) },
                    onEraseClick = { viewModel.onErasePress() },
                    onSubmit = { viewModel.onSubmit() },
                    isComplete = uiState.isComplete
                )
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
    onCellClick: (Position) -> Unit
) {
    if (board.isEmpty()) return
    
    val size = board.size
    val cellSize = 54.dp
    
    Card(
        modifier = Modifier.wrapContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(3.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
        ) {
            Column {
                for (row in 0 until size) {
                    Row {
                        for (col in 0 until size) {
                            val position = Position(row, col)
                            val value = board[row][col]
                            val isFixed = position in fixedCells
                            val isSelected = position == selectedPosition
                            val isInvalid = position in invalidPositions
                            
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .background(
                                        when {
                                            isInvalid -> Color(0xFFFFCDD2)
                                            isSelected -> Color(0xFFBBDEFB)
                                            else -> Color.White
                                        }
                                    )
                                    .then(
                                        // Right border
                                        if (col < size - 1) {
                                            if (col % 3 == 2) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = Color(0xFF757575)
                                                )
                                            } else {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = Color(0xFFE0E0E0)
                                                )
                                            }
                                        } else Modifier
                                    )
                                    .then(
                                        // Bottom border
                                        if (row < size - 1) {
                                            if (row % 2 == 1) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = Color(0xFF757575)
                                                )
                                            } else {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = Color(0xFFE0E0E0)
                                                )
                                            }
                                        } else Modifier
                                    )
                                    .clickable(enabled = !isFixed) {
                                        onCellClick(position)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (value != 0) {
                                    Text(
                                        text = value.toString(),
                                        fontSize = 28.sp,
                                        fontWeight = if (isFixed) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isInvalid -> Color(0xFFD32F2F)
                                            isFixed -> Color(0xFF1A1A1A)
                                            else -> Color(0xFF6200EA)
                                        }
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
    onSubmit: () -> Unit,
    isComplete: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row: 1-3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (number in 1..3) {
                Button(
                    onClick = { onNumberClick(number) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = number.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Second row: 4-6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (number in 4..6) {
                Button(
                    onClick = { onNumberClick(number) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = number.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Clear button
        Button(
            onClick = onEraseClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Clear",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Submit button (when puzzle is complete)
        if (isComplete) {
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EA),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Submit Solution",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

