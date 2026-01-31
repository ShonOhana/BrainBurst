package com.brainburst.presentation.zip

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.brainburst.domain.game.Position
import com.brainburst.domain.game.zip.WallSide
import com.brainburst.platform.PlatformLifecycleHandler
import com.brainburst.presentation.ads.BannerAdView
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipScreen(viewModel: ZipViewModel, adManager: com.brainburst.domain.ads.AdManager) {
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
        if (uiState.dots.isNotEmpty()) {
            viewModel.onAppResumed()
        }
    }
    
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
    
    // Purple gradient background (matching Sudoku)
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header (fixed at top)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Title with gradient (matching Sudoku)
                    Text(
                        text = "Zip",
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
                    
                    // Subtitle
                    Text(
                        text = "Use pathfinding skills to move through the grid",
                        fontSize = 12.sp,
                        color = Color(0xFF6200EA).copy(alpha = 0.7f)
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
                            
                            // Dots connected
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${uiState.lastConnectedDotIndex} / ${uiState.dots.size}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6200EA)
                                )
                                Text(
                                    text = "Connected",
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
                                    text = "${uiState.movesCount}",
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
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Clear Button
                    Button(
                        onClick = { viewModel.onClearClick() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6200EA)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // ZIP Grid
                    ZipGrid(
                        gridSize = uiState.gridSize,
                        dots = uiState.dots,
                        path = uiState.path,
                        hintPosition = uiState.hintPosition,
                        hintType = uiState.hintType,
                        walls = uiState.walls,
                        onDragStart = { position -> viewModel.onDragStart(position) },
                        onDragMove = { position -> viewModel.onDragMove(position) },
                        onDragEnd = { viewModel.onDragEnd() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Control buttons (Undo and Hint swapped)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Undo button (was Reset)
                        Button(
                            onClick = { viewModel.onUndoPress() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !uiState.isCompleted
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Undo",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Undo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Hint button with cooldown animation
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Button(
                                onClick = { viewModel.onHintPress() },
                                modifier = Modifier.fillMaxSize(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9810FA),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF9810FA).copy(alpha = 0.6f),
                                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !uiState.isCompleted && !uiState.isHintOnCooldown
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
                            if (uiState.isHintOnCooldown) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(uiState.hintCooldownProgress)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // How to Play Section
                    HowToPlayZipSection()
                    
                    // Banner ad at bottom
                    Spacer(modifier = Modifier.height(8.dp))
                    BannerAdView(adManager = adManager)
                    
                    if (uiState.isSubmitting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // 3D Completion Animation Overlay
        if (uiState.showCompletionAnimation) {
            CompletionAnimation3D(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
            )
        }
    }
}

@Composable
fun HowToPlayZipSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "How to play",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EA)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Rule 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Connect all numbered dots in order from 1 to the last number by drawing a continuous path through the grid.",
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rule 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Draw your path by dragging your finger from cell to cell. You can move horizontally or vertically (not diagonally).",
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rule 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Black walls block your path - you cannot cross them.",
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rule 4
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Your path must visit every cell on the grid exactly once.",
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rule 5
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "To go back, drag to the previous cell. Use the Undo button to undo one step.",
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Rule 6
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Complete the puzzle by connecting all dots in order while filling every cell.",
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 20.sp
                    )
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
    walls: List<ZipWallUi> = emptyList(),
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
                        val lineWidth = minOf(cellWidth, cellHeight) * 0.55f // 75% of cell size
                        
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
                                color = Color(0xFF9810FA), // Purple line
                                start = startOffset,
                                end = endOffset,
                                strokeWidth = lineWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    
                    // Draw walls
                    if (walls.isNotEmpty() && gridSize.width > 0 && gridSize.height > 0) {
                        val cellWidth = size.width / 6f
                        val cellHeight = size.height / 6f
                        val wallThickness = minOf(cellWidth, cellHeight) * 0.15f
                        
                        for (wall in walls) {
                            val wallStart: Offset
                            val wallEnd: Offset
                            
                            when (wall.side) {
                                WallSide.TOP -> {
                                    wallStart = Offset(wall.col * cellWidth, wall.row * cellHeight)
                                    wallEnd = Offset((wall.col + 1) * cellWidth, wall.row * cellHeight)
                                }
                                WallSide.RIGHT -> {
                                    wallStart = Offset((wall.col + 1) * cellWidth, wall.row * cellHeight)
                                    wallEnd = Offset((wall.col + 1) * cellWidth, (wall.row + 1) * cellHeight)
                                }
                                WallSide.BOTTOM -> {
                                    wallStart = Offset(wall.col * cellWidth, (wall.row + 1) * cellHeight)
                                    wallEnd = Offset((wall.col + 1) * cellWidth, (wall.row + 1) * cellHeight)
                                }
                                WallSide.LEFT -> {
                                    wallStart = Offset(wall.col * cellWidth, wall.row * cellHeight)
                                    wallEnd = Offset(wall.col * cellWidth, (wall.row + 1) * cellHeight)
                                }
                            }
                            
                            drawLine(
                                color = Color.Black,
                                start = wallStart,
                                end = wallEnd,
                                strokeWidth = wallThickness,
                                cap = StrokeCap.Square
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
        isLastInPath -> Color(0xFF9810FA).copy(alpha = 0.3f)
        isInPath -> Color(0xFF9810FA).copy(alpha = 0.15f)
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
            // Draw numbered dot with purple background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9810FA)),
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

// Data class for confetti particles
private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun CompletionAnimation3D(modifier: Modifier = Modifier) {
    // Main 3D pulsing and rotation animation
    val infiniteTransition = rememberInfiniteTransition()
    
    // Pulsing scale effect
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // 3D rotation effect on Y-axis
    val rotationY by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // 3D rotation effect on X-axis
    val rotationX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Fade in animation for entrance
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500)
    )
    
    // Star glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Generate confetti particles once
    val confettiParticles = remember {
        List(50) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -0.1f - Random.nextFloat() * 0.2f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.002f,
                velocityY = Random.nextFloat() * 0.005f + 0.003f,
                color = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFF9810FA), // Purple
                    Color(0xFF155DFC), // Blue
                    Color(0xFFFF5722), // Red
                    Color(0xFF4CAF50)  // Green
                ).random(),
                size = Random.nextFloat() * 8f + 4f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 10f
            )
        }
    }
    
    // Animate confetti falling
    var confettiTime by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                confettiTime += 0.016f // ~60fps
            }
        }
    }
    
    Box(
        modifier = modifier.graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center
    ) {
        // Confetti background
        Canvas(modifier = Modifier.fillMaxSize()) {
            confettiParticles.forEach { particle ->
                val currentY = particle.y + particle.velocityY * confettiTime
                val currentX = particle.x + particle.velocityX * confettiTime
                
                // Wrap particles that go off screen
                val wrappedY = if (currentY > 1.1f) -0.1f else currentY
                val wrappedX = when {
                    currentX < -0.1f -> 1.1f
                    currentX > 1.1f -> -0.1f
                    else -> currentX
                }
                
                // Draw rotating circle as confetti
                val centerX = wrappedX * size.width
                val centerY = wrappedY * size.height
                
                drawCircle(
                    color = particle.color,
                    radius = particle.size,
                    center = Offset(centerX, centerY),
                    alpha = 0.9f
                )
            }
        }
        
        // Main completion content with 3D effect
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.rotationY = rotationY
                    this.rotationX = rotationX
                    cameraDistance = 12f * density
                }
        ) {
            // Glowing background for star
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                
                // Star icon
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(140.dp),
                    tint = Color(0xFFFFD700) // Gold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Completion text with gradient
            Text(
                text = "Puzzle Complete!",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA726)
                        )
                    )
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Celebration emoji
            Text(
                text = "🎉",
                fontSize = 48.sp
            )
        }
    }
}
