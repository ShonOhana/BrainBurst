package com.brainburst.presentation.zip

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.brainburst.domain.game.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipScreen(viewModel: ZipViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()
    
    // Handle events
    LaunchedEffect(event) {
        when (val e = event) {
            is ZipEvent.PuzzleCompleted -> {
                viewModel.clearEvent()
            }
            is ZipEvent.ValidationError -> {
                // Could show snackbar here
            }
            null -> {}
        }
    }
    
    // Blue gradient background (different from Sudoku's purple)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD5E8FF),
                        Color(0xFFBBD8FF)
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Back button
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
                            tint = Color(0xFF0062EA),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Title
                Text(
                    text = "Zip",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1098FA),
                                Color(0xFF155DFC)
                            )
                        )
                    )
                )
                
                // Subtitle
                Text(
                    text = "Connect the Dots",
                    fontSize = 12.sp,
                    color = Color(0xFF0062EA).copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stats Card
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
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = uiState.elapsedTimeFormatted,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0062EA)
                            )
                        }
                        
                        Divider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp),
                            color = Color(0xFFE0E0E0)
                        )
                        
                        // Dots connected
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${uiState.lastConnectedDotIndex} / ${uiState.dots.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0062EA)
                            )
                            Text(
                                text = "Dots",
                                fontSize = 10.sp,
                                color = Color(0xFF666666)
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
                                text = "${uiState.movesCount}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0062EA)
                            )
                            Text(
                                text = "Moves",
                                fontSize = 10.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // ZIP Grid
                ZipGrid(
                    gridSize = uiState.gridSize,
                    dots = uiState.dots,
                    path = uiState.path,
                    hintPosition = uiState.hintPosition,
                    hintType = uiState.hintType,
                    onDragStart = { position -> viewModel.onDragStart(position) },
                    onDragMove = { position -> viewModel.onDragMove(position) },
                    onDragEnd = { viewModel.onDragEnd() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Control buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.onHintPress() },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFA726)
                        ),
                        enabled = !uiState.isCompleted && uiState.path.size < 36
                    ) {
                        Text("💡 Hint")
                    }
                    
                    Button(
                        onClick = { viewModel.onResetPress() },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF666666)
                        ),
                        enabled = !uiState.isCompleted
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
                
                if (uiState.isSubmitting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ZipGrid(
    @Suppress("UNUSED_PARAMETER") gridSize: Int,
    dots: List<ZipDotUi>,
    path: List<Position>,
    hintPosition: Position?,
    hintType: HintType,
    onDragStart: (Position) -> Unit,
    onDragMove: (Position) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .onGloballyPositioned { coordinates ->
                    gridSize = coordinates.size
                }
                .drawBehind {
                    // Draw lines connecting path positions
                    if (path.size > 1 && gridSize.width > 0 && gridSize.height > 0) {
                        val cellWidth = size.width / 6f
                        val cellHeight = size.height / 6f
                        
                        for (i in 0 until path.size - 1) {
                            val start = path[i]
                            val end = path[i + 1]
                            
                            val startOffset = Offset(
                                x = (start.col + 0.5f) * cellWidth,
                                y = (start.row + 0.5f) * cellHeight
                            )
                            val endOffset = Offset(
                                x = (end.col + 0.5f) * cellWidth,
                                y = (end.row + 0.5f) * cellHeight
                            )
                            
                            drawLine(
                                color = Color(0xFFFF5722),
                                start = startOffset,
                                end = endOffset,
                                strokeWidth = 12f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val position = offsetToPosition(offset, gridSize, 6)
                            position?.let { onDragStart(it) }
                        },
                        onDrag = { change, _ ->
                            if (isDragging) {
                                val position = offsetToPosition(change.position, gridSize, 6)
                                position?.let { onDragMove(it) }
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onDragEnd()
                        },
                        onDragCancel = {
                            isDragging = false
                            onDragEnd()
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0 until 6) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (col in 0 until 6) {
                            val position = Position(row, col)
                            val dot = dots.find { it.position == position }
                            val isInPath = path.contains(position)
                            val pathIndex = path.indexOf(position)
                            val isHinted = position == hintPosition
                            
                            ZipCell(
                                position = position,
                                dot = dot,
                                isInPath = isInPath,
                                isLastInPath = pathIndex == path.size - 1,
                                isHinted = isHinted,
                                hintType = hintType,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun offsetToPosition(offset: Offset, gridSize: IntSize, cellCount: Int): Position? {
    if (gridSize.width == 0 || gridSize.height == 0) return null
    
    val cellWidth = gridSize.width.toFloat() / cellCount
    val cellHeight = gridSize.height.toFloat() / cellCount
    
    val col = (offset.x / cellWidth).toInt().coerceIn(0, cellCount - 1)
    val row = (offset.y / cellHeight).toInt().coerceIn(0, cellCount - 1)
    
    return Position(row, col)
}

@Composable
fun ZipCell(
    @Suppress("UNUSED_PARAMETER") position: Position,
    dot: ZipDotUi?,
    isInPath: Boolean,
    isLastInPath: Boolean,
    isHinted: Boolean,
    hintType: HintType,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for hint
    val infiniteTransition = rememberInfiniteTransition()
    val hintAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val backgroundColor = when {
        isHinted && hintType == HintType.NextCell -> Color(0xFF4CAF50).copy(alpha = hintAlpha)
        isHinted && hintType == HintType.UndoSegment -> Color(0xFFFF5722).copy(alpha = hintAlpha)
        isHinted && hintType == HintType.NextDot -> Color(0xFFFFA726).copy(alpha = hintAlpha)
        isLastInPath -> Color(0xFF1098FA).copy(alpha = 0.3f)
        isInPath -> Color(0xFF1098FA).copy(alpha = 0.15f)
        else -> Color.Transparent
    }
    
    val borderColor = when {
        isHinted && hintType == HintType.NextCell -> Color(0xFF4CAF50)
        isHinted && hintType == HintType.UndoSegment -> Color(0xFFFF5722)
        isHinted && hintType == HintType.NextDot -> Color(0xFFFFA726)
        else -> Color(0xFFE0E0E0)
    }
    
    val borderWidth = if (isHinted) 2.dp else 1.dp
    
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (dot != null) {
            // Draw numbered dot
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1098FA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${dot.index}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
