package com.brainburst.presentation.tango

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.tango.CellValue
import com.brainburst.domain.game.tango.ClueDirection
import com.brainburst.platform.PlatformLifecycleHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TangoScreen(viewModel: TangoViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    
    PlatformLifecycleHandler(
        onPause = { viewModel.onPause() },
        onResume = { viewModel.onResume() }
    )
    
    LaunchedEffect(Unit) {
        if (uiState.cells.isNotEmpty()) {
            viewModel.onResume()
        }
    }
    
    LaunchedEffect(event) {
        when (event) {
            is TangoEvent.PuzzleCompleted -> {
                viewModel.consumeEvent()
            }
            is TangoEvent.ValidationError -> {
                // Could show snackbar
            }
            null -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
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
                    Button(onClick = { viewModel.onBackClick() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = { viewModel.onBackClick() },
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
                
                Text(
                    text = "Tango",
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
                
                Text(
                    text = "Fill the grid with sun and moon symbols",
                    fontSize = 12.sp,
                    color = Color(0xFF6200EA).copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
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
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        
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
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Grid
                TangoGrid(
                    cells = uiState.cells,
                    fixedCells = uiState.fixedCells,
                    selectedPosition = uiState.selectedPosition,
                    invalidPositions = uiState.invalidPositions,
                    equalClues = uiState.equalClues,
                    oppositeClues = uiState.oppositeClues,
                    onCellClick = { position -> viewModel.onCellClick(position) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TangoGrid(
    cells: Map<Position, CellValue>,
    fixedCells: Set<Position>,
    selectedPosition: Position?,
    invalidPositions: List<Position>,
    equalClues: List<com.brainburst.domain.game.tango.EqualClue>,
    oppositeClues: List<com.brainburst.domain.game.tango.OppositeClue>,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Draw cells first
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 6) {
                Row(modifier = Modifier.weight(1f)) {
                    for (col in 0 until 6) {
                        val position = Position(row, col)
                        val cellValue = cells[position] ?: CellValue.EMPTY
                        val isFixed = fixedCells.contains(position)
                        val isSelected = selectedPosition == position
                        val isInvalid = invalidPositions.contains(position)
                        
                        TangoCell(
                            value = cellValue,
                            isFixed = isFixed,
                            isSelected = isSelected,
                            isInvalid = isInvalid,
                            onClick = { onCellClick(position) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Draw clues and grid lines on top
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / 6f
            
            // Draw clues (= and ×) between cells  
            equalClues.forEach { clue ->
                val startX = clue.col * cellSize
                val startY = clue.row * cellSize
                
                when (clue.direction) {
                    ClueDirection.HORIZONTAL -> {
                        // Draw = between cells horizontally (on the border)
                        val x = startX + cellSize
                        val y = startY + cellSize / 2
                        
                        // White background circle for visibility
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(x, y)
                        )
                        
                        // Two horizontal lines for =
                        drawLine(
                            color = Color(0xFF6200EA),
                            start = Offset(x - 8.dp.toPx(), y - 3.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y - 3.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF6200EA),
                            start = Offset(x - 8.dp.toPx(), y + 3.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y + 3.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    ClueDirection.VERTICAL -> {
                        // Draw = between cells vertically (on the border)
                        val x = startX + cellSize / 2
                        val y = startY + cellSize
                        
                        // White background circle for visibility
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(x, y)
                        )
                        
                        // Two horizontal lines for =
                        drawLine(
                            color = Color(0xFF6200EA),
                            start = Offset(x - 8.dp.toPx(), y - 3.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y - 3.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF6200EA),
                            start = Offset(x - 8.dp.toPx(), y + 3.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y + 3.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            oppositeClues.forEach { clue ->
                val startX = clue.col * cellSize
                val startY = clue.row * cellSize
                
                when (clue.direction) {
                    ClueDirection.HORIZONTAL -> {
                        // Draw × between cells horizontally
                        val x = startX + cellSize
                        val y = startY + cellSize / 2
                        
                        // White background circle for visibility
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(x, y)
                        )
                        
                        // Two diagonal lines for ×
                        drawLine(
                            color = Color(0xFFE91E63),
                            start = Offset(x - 8.dp.toPx(), y - 8.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y + 8.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFE91E63),
                            start = Offset(x - 8.dp.toPx(), y + 8.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y - 8.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    ClueDirection.VERTICAL -> {
                        // Draw × between cells vertically
                        val x = startX + cellSize / 2
                        val y = startY + cellSize
                        
                        // White background circle for visibility
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(x, y)
                        )
                        
                        // Two diagonal lines for ×
                        drawLine(
                            color = Color(0xFFE91E63),
                            start = Offset(x - 8.dp.toPx(), y - 8.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y + 8.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFE91E63),
                            start = Offset(x - 8.dp.toPx(), y + 8.dp.toPx()),
                            end = Offset(x + 8.dp.toPx(), y - 8.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            // Draw grid lines
            for (i in 0..6) {
                val offset = i * cellSize
                
                // Vertical lines
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(offset, 0f),
                    end = Offset(offset, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Horizontal lines
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(0f, offset),
                    end = Offset(size.width, offset),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun TangoCell(
    value: CellValue,
    isFixed: Boolean,
    isSelected: Boolean,
    isInvalid: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isInvalid -> Color(0xFFFFCDD2)
        else -> Color.White
    }
    
    val borderColor = when {
        isInvalid -> Color(0xFFE91E63)
        else -> Color.Transparent
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .border(2.dp, borderColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (value) {
            CellValue.SUN -> {
                // Yellow sun circle like LinkedIn
                Canvas(modifier = Modifier.size(32.dp)) {
                    drawCircle(
                        color = Color(0xFFFFA500), // Orange/yellow
                        radius = 16.dp.toPx()
                    )
                }
            }
            CellValue.MOON -> {
                // Blue moon crescent like LinkedIn
                Canvas(modifier = Modifier.size(32.dp)) {
                    drawCircle(
                        color = Color(0xFF4A90E2), // Blue
                        radius = 16.dp.toPx()
                    )
                    // Cut out a circle to make crescent shape
                    drawCircle(
                        color = backgroundColor,
                        radius = 12.dp.toPx(),
                        center = Offset(size.width * 0.4f, size.height * 0.4f)
                    )
                }
            }
            CellValue.EMPTY -> {}
        }
    }
}

