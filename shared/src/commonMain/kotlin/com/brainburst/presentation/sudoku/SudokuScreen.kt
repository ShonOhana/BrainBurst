package com.brainburst.presentation.sudoku

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mini Sudoku 6×6",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackPress() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = {
            if (event is SudokuEvent.ValidationError) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
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
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Timer and info row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Puzzle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⏱️ ${uiState.elapsedTimeFormatted}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Moves: ${uiState.movesCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Number pad
                    NumberPad(
                        onNumberClick = { viewModel.onNumberPress(it) },
                        onEraseClick = { viewModel.onErasePress() }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Submit button
                    Button(
                        onClick = { viewModel.onSubmit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = uiState.isComplete && !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submitting...")
                        } else {
                            Text(
                                text = if (uiState.isComplete) "Submit Solution" else "Fill all cells to submit",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                
                // Full-screen loader overlay when submitting
                if (uiState.isSubmitting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading leaderboard",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
    val cellSize = 48.dp
    
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.outline)
            .border(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        for (row in 0 until size) {
            Row {
                for (col in 0 until size) {
                    val position = Position(row, col)
                    val value = board[row][col]
                    val isFixed = position in fixedCells
                    val isSelected = position == selectedPosition
                    val isInvalid = position in invalidPositions
                    
                    // Determine border widths for block highlighting
                    val topBorder = if (row % 2 == 0 && row != 0) 2.dp else 1.dp
                    val leftBorder = if (col % 3 == 0 && col != 0) 2.dp else 1.dp
                    
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .background(
                                when {
                                    isInvalid -> MaterialTheme.colorScheme.errorContainer
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isFixed -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            .then(
                                if (row % 2 == 0 && row != 0) {
                                    Modifier.border(
                                        width = topBorder,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else Modifier
                            )
                            .then(
                                if (col % 3 == 0 && col != 0) {
                                    Modifier.border(
                                        width = leftBorder,
                                        color = MaterialTheme.colorScheme.outline
                                    )
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
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = if (isFixed) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isInvalid -> MaterialTheme.colorScheme.error
                                    isFixed -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
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
    onEraseClick: () -> Unit
) {
    Column {
        // Numbers 1-6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (number in 1..6) {
                Button(
                    onClick = { onNumberClick(number) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Erase button
        Button(
            onClick = onEraseClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Erase",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Erase",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

