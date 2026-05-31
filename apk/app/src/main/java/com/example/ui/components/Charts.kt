package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ChartPoint
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldPale
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DangerRed
import kotlin.math.roundToInt

/**
 * Renders a premium horizontal segmented allocation bar with numeric feedback below it.
 */
@Composable
fun SegmentedAllocationBar(
    stockValue: Double,
    fiiValue: Double,
    modifier: Modifier = Modifier
) {
    val total = stockValue + fiiValue
    val stockPercent = if (total > 0 && !total.isNaN() && !total.isInfinite()) {
        (stockValue / total).toFloat().let { if (it.isNaN() || it.isInfinite()) 0f else it }
    } else 0f
    val fiiPercent = if (total > 0 && !total.isNaN() && !total.isInfinite()) {
        (fiiValue / total).toFloat().let { if (it.isNaN() || it.isInfinite()) 0f else it }
    } else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(BorderColor.copy(alpha = 0.2f), shape = RoundedCornerShape(5.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stockPercent > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(stockPercent)
                        .background(
                            color = GoldPrimary,
                            shape = if (fiiPercent <= 0.01f) RoundedCornerShape(5.dp) else RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp)
                        )
                )
            }
            if (fiiPercent > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(fiiPercent)
                        .background(
                            color = GoldPale,
                            shape = if (stockPercent <= 0.01f) RoundedCornerShape(5.dp) else RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(GoldPrimary, shape = CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ações: ${String.format("%.1f", stockPercent * 100)}%",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(GoldPale, shape = CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FIIs: ${String.format("%.1f", fiiPercent * 100)}%",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Draw a beautiful interactive line chart of historical points using Canvas.
 */
@Composable
fun HistoricalPriceLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = GoldPrimary
) {
    val cleanPoints = remember(points) {
        points
            .filter { it.close.isFinite() && it.close > 0.0 }
            .sortedBy { it.timestamp }
    }
    if (cleanPoints.isEmpty()) return

    val minPrice = cleanPoints.minOf { it.close }
    val maxPrice = cleanPoints.maxOf { it.close }
    val priceRange = maxPrice - minPrice
    val paddingPercent = 0.15f // add vertical breathing space

    val displayMin = minPrice - (priceRange * paddingPercent)
    val displayMax = maxPrice + (priceRange * paddingPercent)
    val displayRange = if (displayMax - displayMin > 0.0) displayMax - displayMin else 1.0

    // Final safety check for NaN/Infinite in display values
    val finalDisplayRange = if (displayRange.isNaN() || displayRange.isInfinite()) 1.0 else displayRange
    val finalDisplayMin = if (displayMin.isNaN() || displayMin.isInfinite()) 0.0 else displayMin

    var activePointIndex by remember(points) { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main chart box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(cleanPoints) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val anyPressed = changes.any { it.pressed }
                                if (anyPressed) {
                                    val change = changes.first()
                                    val x = change.position.x
                                    val width = size.width.coerceAtLeast(0)
                                    if (cleanPoints.size > 1 && width > 0) {
                                        val itemWidth = width.toFloat() / (cleanPoints.size - 1)
                                        val index = (x / itemWidth).roundToInt().coerceIn(0, cleanPoints.size - 1)
                                        activePointIndex = index
                                    }
                                } else {
                                    activePointIndex = null
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width.coerceAtLeast(0f)
                    val height = size.height.coerceAtLeast(0f)

                    // Draw background horizontal grid lines (3 grid lines)
                    val gridLines = 3
                    for (g in 0..gridLines) {
                        val y = height * g / gridLines
                        drawLine(
                            color = BorderColor.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // If only 1 point, center it
                    if (cleanPoints.size == 1) {
                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = Offset(width / 2, height / 2)
                        )
                        return@Canvas
                    }

                    val path = Path()
                    val bgPath = Path()
                    
                    val itemWidth = width / (cleanPoints.size - 1)

                    cleanPoints.forEachIndexed { index, point ->
                        val x = index * itemWidth
                        
                        val safeClose = if (point.close.isNaN() || point.close.isInfinite()) 0.0 else point.close

                        // Calculate normalized Y coordinate (inverted because 0 is at the top)
                        val normalizedY = ((safeClose - finalDisplayMin) / finalDisplayRange).toFloat().let {
                            if (it.isNaN() || it.isInfinite()) 0f else it
                        }
                        val y = height - (normalizedY * height)

                        if (index == 0) {
                            path.moveTo(x, y)
                            bgPath.moveTo(x, height)
                            bgPath.lineTo(x, y)
                        } else {
                            // Curved smooth bezier transition
                            val prevX = (index - 1) * itemWidth
                            val prevCloseValue = if (cleanPoints[index-1].close.isNaN()) 0.0 else cleanPoints[index-1].close
                            val prevNormalizedY = ((prevCloseValue - finalDisplayMin) / finalDisplayRange).toFloat().let {
                                if (it.isNaN() || it.isInfinite()) 0f else it
                            }
                            val prevY = height - (prevNormalizedY * height)
                            
                            path.cubicTo(
                                prevX + itemWidth / 2, prevY,
                                x - itemWidth / 2, y,
                                x, y
                            )
                            bgPath.cubicTo(
                                prevX + itemWidth / 2, prevY,
                                x - itemWidth / 2, y,
                                x, y
                            )
                        }
                        
                        if (index == cleanPoints.size - 1) {
                            bgPath.lineTo(x, height)
                            bgPath.close()
                        }
                    }

                    // Draw soft filled glow background under line
                    drawPath(
                        path = bgPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    // Draw main price path line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Highlight the active touch point (or default to last point)
                    val selectedIndex = activePointIndex ?: (cleanPoints.size - 1)
                    val isHotTouch = activePointIndex != null

                    val pX = selectedIndex * itemWidth
                    val pClose = if (cleanPoints[selectedIndex].close.isNaN()) 0.0 else cleanPoints[selectedIndex].close
                    val pNormalY = ((pClose - finalDisplayMin) / finalDisplayRange).toFloat().let {
                        if (it.isNaN() || it.isInfinite()) 0f else it
                    }
                    val pY = height - (pNormalY * height)

                    if (isHotTouch) {
                        // Draw vertical touch guideline
                        drawLine(
                            color = lineColor.copy(alpha = 0.4f),
                            start = Offset(pX, 0f),
                            end = Offset(pX, height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Draw glowing intersection dot
                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 12.dp.toPx(),
                            center = Offset(pX, pY)
                        )
                    }

                    // Main indicator dot at selected index
                    drawCircle(
                        color = lineColor,
                        radius = if (isHotTouch) 6.dp.toPx() else 5.dp.toPx(),
                        center = Offset(pX, pY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Draw date and price extremities below the chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cleanPoints.firstOrNull()?.dateLabel ?: "",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (activePointIndex == null) {
                    Text(
                        text = "Min: R$ ${String.format("%.2f", minPrice)}  |  Máx: R$ ${String.format("%.2f", maxPrice)}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Arraste para ver o histórico",
                        color = lineColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = cleanPoints.lastOrNull()?.dateLabel ?: "",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Overlay HUD
        androidx.compose.animation.AnimatedVisibility(
            visible = activePointIndex != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        ) {
            val safeActivePointIndex = activePointIndex?.takeIf { it in cleanPoints.indices }
            if (safeActivePointIndex != null) {
                val point = cleanPoints[safeActivePointIndex]
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, lineColor.copy(alpha = 0.3f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "R$ ${String.format("%.2f", point.close)}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(modifier = Modifier.width(1.dp).height(12.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                        Text(
                            text = point.dateLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Premium Custom Bar Chart utilizing Compose Canvas to avoid library version mismatches
 */
@Composable
fun CustomBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = GoldPrimary
) {
    if (values.isEmpty()) return
    val maxValue = values.maxOrNull()?.let { if (it <= 0f) 1f else it } ?: 1f

    var activeIndex by remember(values) { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableStateOf(0f) }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(values) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width
                            val spacing = 8.dp.toPx()
                            val barCount = values.size
                            val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)
                            activeIndex = ((offset.x - spacing) / (barWidth + spacing)).toInt().coerceIn(0, barCount - 1)
                        },
                        onDrag = { change, _ ->
                            val width = size.width
                            val spacing = 8.dp.toPx()
                            val barCount = values.size
                            val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)
                            activeIndex = ((change.position.x - spacing) / (barWidth + spacing)).toInt().coerceIn(0, barCount - 1)
                        },
                        onDragEnd = { activeIndex = null },
                        onDragCancel = { activeIndex = null }
                    )
                }
                .pointerInput(values) {
                    detectTapGestures(
                        onPress = { offset ->
                            val width = size.width
                            val spacing = 8.dp.toPx()
                            val barCount = values.size
                            val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)
                            activeIndex = ((offset.x - spacing) / (barWidth + spacing)).toInt().coerceIn(0, barCount - 1)
                            tryAwaitRelease()
                            activeIndex = null
                        }
                    )
                }
            ) {
                val width = size.width
                val height = size.height
                val spacing = 8.dp.toPx()
                val barCount = values.size
                val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)

                // Draw background horizontal grid
                val gridLines = 3
                for (g in 0..gridLines) {
                    val y = height * g / gridLines
                    drawLine(
                        color = BorderColor.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width.coerceAtLeast(0f), y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                values.forEachIndexed { index, value ->
                    val x = spacing + index * (barWidth + spacing)
                    val barHeight = ((value / maxValue) * height).coerceAtLeast(0f)
                    val y = (height - barHeight).coerceAtLeast(0f)

                    val isSelected = activeIndex == index
                    val alpha = if (activeIndex == null || isSelected) 1f else 0.4f

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor.copy(alpha = alpha), barColor.copy(alpha = alpha * 0.4f))
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
            
            // Tooltip Overlay
            activeIndex?.let { idx ->
                val value = values[idx]
                val months = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
                val m = months.getOrNull(idx) ?: ""
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .background(com.example.ui.theme.DarkSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor.copy(alpha=0.2f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = m, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = String.format("R$ %,.2f", value), color = barColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Month Labels equal space Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val months = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
            months.take(values.size).forEach { m ->
                Text(
                    text = m,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class StackedBarData(
    val received: Float,
    val projected: Float,
    val label: String
)

@Composable
fun StackedBarChart(
    data: List<StackedBarData>,
    modifier: Modifier = Modifier,
    receivedColor: Color = GoldPrimary,
    projectedColor: Color = GoldPale
) {
    if (data.isEmpty()) return
    
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1000, 
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "bar_animation"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }
    
    val maxValue = data.maxOfOrNull { it.received + it.projected }?.let { if (it <= 0f) 1f else it } ?: 1f

    // Calculate Y-axis labels
    val yLabels = listOf(
        String.format("%.0f", maxValue),
        String.format("%.0f", maxValue * 0.75f),
        String.format("%.0f", maxValue * 0.5f),
        String.format("%.0f", maxValue * 0.25f),
        "0"
    )

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        Row(modifier = Modifier.weight(1f)) {
            // Y-axis labels
            Column(
                modifier = Modifier.fillMaxHeight().padding(bottom = 24.dp).padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yLabels.forEach { label ->
                    Text(
                        text = label,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Canvas(modifier = Modifier.fillMaxSize().pointerInput(data) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val width = size.width
                                val spacing = 12.dp.toPx()
                                val barCount = data.size
                                val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)
                                var draggedIndex: Int? = null
                                for (index in data.indices) {
                                    val x = spacing + index * (barWidth + spacing)
                                    if (offset.x >= x && offset.x <= x + barWidth) {
                                        draggedIndex = index
                                        break
                                    }
                                }
                                selectedIndex = draggedIndex
                            },
                            onDrag = { change, _ ->
                                val width = size.width
                                val spacing = 12.dp.toPx()
                                val barCount = data.size
                                val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)
                                var draggedIndex: Int? = null
                                for (index in data.indices) {
                                    val x = spacing + index * (barWidth + spacing)
                                    if (change.position.x >= x && change.position.x <= x + barWidth) {
                                        draggedIndex = index
                                        break
                                    }
                                }
                                if (draggedIndex != null) selectedIndex = draggedIndex
                            },
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null }
                        )
                    }.pointerInput(data) {
                        detectTapGestures(
                            onPress = { offset ->
                                val width = size.width
                                val spacing = 12.dp.toPx()
                                val barCount = data.size
                                val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)
                                var clickedIndex: Int? = null
                                for (index in data.indices) {
                                    val x = spacing + index * (barWidth + spacing)
                                    if (offset.x >= x && offset.x <= x + barWidth) {
                                        clickedIndex = index
                                        break
                                    }
                                }
                                selectedIndex = clickedIndex
                                tryAwaitRelease()
                                selectedIndex = null
                            }
                        )
                    }) {
                        val width = size.width
                        val height = size.height
                        val spacing = 12.dp.toPx()
                        val barCount = data.size
                        val barWidth = ((width - (spacing * (barCount + 1))) / barCount).coerceAtLeast(0f)

                        // Draw background horizontal grid
                        val gridLines = 4
                        for (g in 0..gridLines) {
                            val y = height * g / gridLines
                            drawLine(
                                color = BorderColor.copy(alpha = 0.15f),
                                start = Offset(0f, y),
                                end = Offset(width.coerceAtLeast(0f), y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        data.forEachIndexed { index, item ->
                            val x = spacing + index * (barWidth + spacing)
                            // Multiply heights by progress for animation
                            val totalHeight = (((item.received + item.projected) / maxValue) * height * progress).coerceAtLeast(0f)
                            val receivedHeight = ((item.received / maxValue) * height * progress).coerceAtLeast(0f)
                            val projectedHeight = ((item.projected / maxValue) * height * progress).coerceAtLeast(0f)
                            
                            val bottomY = height
                            
                            val alpha = if (selectedIndex == null || selectedIndex == index) 1f else 0.3f

                            if (totalHeight > 0) {
                                drawRoundRect(
                                    color = receivedColor.copy(alpha = alpha),
                                    topLeft = Offset(x, bottomY - receivedHeight),
                                    size = Size(barWidth, receivedHeight),
                                    cornerRadius = if (projectedHeight == 0f) androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()) else androidx.compose.ui.geometry.CornerRadius(0f, 0f)
                                )
                                
                                if (projectedHeight > 0) {
                                    drawRoundRect(
                                        color = projectedColor.copy(alpha = alpha),
                                        topLeft = Offset(x, bottomY - totalHeight),
                                        size = Size(barWidth, projectedHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                    if (receivedHeight > 0) {
                                        drawRect(
                                            color = projectedColor.copy(alpha = alpha),
                                            topLeft = Offset(x, bottomY - totalHeight + 4.dp.toPx()),
                                            size = Size(barWidth, minOf(projectedHeight - 4.dp.toPx(), 4.dp.toPx()).coerceAtLeast(0f))
                                        )
                                    }
                                }
                                
                                // Draw total label on top if selected (brief indicator)
                                if (selectedIndex == index) {
                                    val textStr = "R$ %.2f".format(item.received + item.projected)
                                    val textPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 10.sp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                    val textY = (bottomY - totalHeight - 6.dp.toPx()).coerceAtLeast(15.dp.toPx())
                                    drawContext.canvas.nativeCanvas.drawText(
                                        textStr,
                                        x + barWidth / 2,
                                        textY,
                                        textPaint
                                    )
                                }
                            }
                        }
                    }
                    
                    // Glassmorphic absolute floating tooltip overlay
                    val item = selectedIndex?.let { data.getOrNull(it) }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = item != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        if (item != null) {
                            Surface(
                                color = com.example.ui.theme.DarkSurfaceElevated.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.16f)),
                                shadowElevation = 8.dp,
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = item.label,
                                        color = GoldPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val partRev = if (item.received > 0f) "Recebido: R$ %.2f".format(item.received) else ""
                                        val partProj = if (item.projected > 0f) "A receber: R$ %.2f".format(item.projected) else ""
                                        if (partRev.isNotEmpty()) {
                                            Text(
                                                text = partRev,
                                                color = receivedColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                        if (partRev.isNotEmpty() && partProj.isNotEmpty()) {
                                            Text("•", color = TextSecondary.copy(alpha = 0.5f), fontSize = 10.sp)
                                        }
                                        if (partProj.isNotEmpty()) {
                                            Text(
                                                text = partProj,
                                                color = projectedColor.copy(alpha = 0.95f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Alignment-safe X-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val step = when {
                        data.size <= 6 -> 1
                        data.size <= 12 -> 2
                        else -> 4
                    }
                    data.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index % step == 0) {
                                Text(
                                    text = item.label,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun resampleFloatSeries(values: List<Float>, targetSize: Int): List<Float> {
    val clean = values.filter { it.isFinite() }
    if (targetSize <= 0) return emptyList()
    if (clean.isEmpty()) return List(targetSize) { 0f }
    if (clean.size == targetSize) return clean
    if (clean.size == 1) return List(targetSize) { clean.first() }
    return List(targetSize) { index ->
        val sourceIndex = if (targetSize == 1) 0.0 else index.toDouble() * (clean.size - 1).toDouble() / (targetSize - 1).toDouble()
        val lower = kotlin.math.floor(sourceIndex).toInt().coerceIn(0, clean.lastIndex)
        val upper = kotlin.math.ceil(sourceIndex).toInt().coerceIn(0, clean.lastIndex)
        if (lower == upper) {
            clean[lower]
        } else {
            val t = (sourceIndex - lower).toFloat().coerceIn(0f, 1f)
            clean[lower] + ((clean[upper] - clean[lower]) * t)
        }
    }
}

/**
 * Premium Dual Line Compare Canvas Chart comparing Portfolio Return vs IPCA Accumulated
 */
@Composable
fun CustomLineChartCompare(
    portfolioValues: List<Float>,
    ipcaValues: List<Float>,
    modifier: Modifier = Modifier
) {
    val cleanPortfolioValues = portfolioValues.filter { it.isFinite() }
    if (cleanPortfolioValues.isEmpty()) return
    val alignedIpcaValues = resampleFloatSeries(ipcaValues, cleanPortfolioValues.size)

    val maxVal = maxOf(cleanPortfolioValues.maxOrNull() ?: 0f, alignedIpcaValues.maxOrNull() ?: 0f).let { if (it <= 0f) 1f else it } + 1f
    val minVal = minOf(cleanPortfolioValues.minOfOrNull { it } ?: 0f, alignedIpcaValues.minOfOrNull { it } ?: 0f).let { if (it >= 0f) -1f else it } - 1f
    val range = if (maxVal - minVal > 0f) maxVal - minVal else 1f

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(cleanPortfolioValues) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val itemWidth = size.width / (cleanPortfolioValues.size - 1).coerceAtLeast(1)
                                selectedIndex = (offset.x / itemWidth).roundToInt().coerceIn(0, cleanPortfolioValues.size - 1)
                            },
                            onDrag = { change, _ ->
                                val itemWidth = size.width / (cleanPortfolioValues.size - 1).coerceAtLeast(1)
                                selectedIndex = (change.position.x / itemWidth).roundToInt().coerceIn(0, cleanPortfolioValues.size - 1)
                            },
                            onDragEnd = {
                                selectedIndex = null
                            },
                            onDragCancel = {
                                selectedIndex = null
                            }
                        )
                    }
                    .pointerInput(cleanPortfolioValues) {
                        detectTapGestures(
                            onPress = { offset ->
                                val itemWidth = size.width / (cleanPortfolioValues.size - 1).coerceAtLeast(1)
                                selectedIndex = (offset.x / itemWidth).roundToInt().coerceIn(0, cleanPortfolioValues.size - 1)
                                tryAwaitRelease()
                                selectedIndex = null
                            }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width.coerceAtLeast(0f)
                    val height = size.height.coerceAtLeast(0f)
                    val itemWidth = width / (cleanPortfolioValues.size - 1).coerceAtLeast(1)

                    // Background horizontal grids
                    val gridLines = 3
                    for (g in 0..gridLines) {
                        val y = height * g / gridLines
                        drawLine(
                            color = BorderColor.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val portPath = Path()
                    val ipcaPath = Path()
                    val portBgPath = Path()

                    cleanPortfolioValues.forEachIndexed { index, value ->
                        val x = index * itemWidth
                        val normY = (value - minVal) / range
                        val y = height - (normY * height)
                        if (index == 0) {
                            portPath.moveTo(x, y)
                            portBgPath.moveTo(x, height)
                            portBgPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * itemWidth
                            val prevNormY = (cleanPortfolioValues[index - 1] - minVal) / range
                            val prevY = height - (prevNormY * height)
                            portPath.cubicTo(
                                prevX + itemWidth / 2, prevY,
                                x - itemWidth / 2, y,
                                x, y
                            )
                            portBgPath.cubicTo(
                                prevX + itemWidth / 2, prevY,
                                x - itemWidth / 2, y,
                                x, y
                            )
                        }
                        if (index == cleanPortfolioValues.size - 1) {
                            portBgPath.lineTo(x, height)
                            portBgPath.close()
                        }
                    }

                    alignedIpcaValues.forEachIndexed { index, value ->
                        val x = index * itemWidth
                        val normY = (value - minVal) / range
                        val y = height - (normY * height)
                        if (index == 0) {
                            ipcaPath.moveTo(x, y)
                        } else {
                            val prevX = (index - 1) * itemWidth
                            val prevNormY = (alignedIpcaValues[index - 1] - minVal) / range
                            val prevY = height - (prevNormY * height)
                            ipcaPath.cubicTo(
                                prevX + itemWidth / 2, prevY,
                                x - itemWidth / 2, y,
                                x, y
                            )
                        }
                    }

                    drawPath(
                        path = portBgPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(GoldPrimary.copy(alpha = 0.15f), Color.Transparent),
                            startY = 0f,
                            endY = height
                        )
                    )

                    drawPath(
                        path = ipcaPath,
                        color = Color(0xFF94A3B8),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )

                    drawPath(
                        path = portPath,
                        color = GoldPrimary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val safeSelectedIndex = selectedIndex?.takeIf { it in cleanPortfolioValues.indices }
                    if (safeSelectedIndex != null) {
                        val idx = safeSelectedIndex
                        val x = idx * itemWidth
                        drawLine(
                            color = GoldPrimary.copy(alpha = 0.4f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        val portY = height - ((((cleanPortfolioValues.getOrNull(idx) ?: 0f) - minVal) / range) * height)
                        val ipcaY = height - ((((alignedIpcaValues.getOrNull(idx) ?: 0f) - minVal) / range) * height)

                        drawCircle(color = GoldPrimary, radius = 7.dp.toPx(), center = Offset(x, portY))
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, portY))

                        drawCircle(color = Color(0xFF94A3B8), radius = 6.dp.toPx(), center = Offset(x, ipcaY))
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, ipcaY))
                    } else {
                        val lastIdx = cleanPortfolioValues.size - 1
                        val lastX = lastIdx * itemWidth
                        val lastPortY = height - (((cleanPortfolioValues[lastIdx] - minVal) / range) * height)
                        val lastIpcaY = height - (((alignedIpcaValues[lastIdx] - minVal) / range) * height)

                        drawCircle(color = GoldPrimary, radius = 5.dp.toPx(), center = Offset(lastX, lastPortY))
                        drawCircle(color = Color(0xFF94A3B8), radius = 4.dp.toPx(), center = Offset(lastX, lastIpcaY))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Inicio", "Mês 3", "Mês 6", "Mês 9", "Mês 12").forEach { h ->
                    Text(
                        text = h,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Stats overlay HUD (Floating)
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedIndex != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            selectedIndex?.let { idx ->
                val portVal = cleanPortfolioValues.getOrNull(idx) ?: 0f
                val ipcaVal = alignedIpcaValues.getOrNull(idx) ?: 0f
                val jurosReais = portVal - ipcaVal
                
                Surface(
                    color = com.example.ui.theme.DarkSurfaceElevated.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (idx == 0) "INÍCIO" else "MÊS $idx",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CARTEIRA", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(String.format("%+.2f%%", portVal), color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("IPCA", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(String.format("%+.2f%%", ipcaVal), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("REAL", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(String.format("%+.2f%%", jurosReais), color = if (jurosReais >= 0f) SuccessGreen else DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium Donut Pie Chart using Canvas to support seamless dynamic allocations.
 */
@Composable
fun PieChart(
    data: List<Pair<String, Float>>,
    colors: List<Color>,
    centerText: String,
    centerSubtext: String,
    modifier: Modifier = Modifier
) {
    val totalSum = data.sumOf { it.second.toDouble() }.toFloat().let { if (it <= 0f) 1f else it }
    
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1000, 
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "pie_animation"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }
    
    var selectedSlice by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = modifier.size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(data) {
            detectDragGestures(
                onDragStart = { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val strokeWidthX = 12.dp.toPx()
                    val radiusOuter = (kotlin.math.min(size.width, size.height)) / 2f
                    val radiusInner = radiusOuter - strokeWidthX * 2
                    if (dist in radiusInner..radiusOuter) {
                        val clickAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f
                        var currentAngle = 0f
                        var clicked = -1
                        for (i in data.indices) {
                            val sliceSweep = (data[i].second / totalSum) * 360f * progress
                            if (clickAngle >= currentAngle && clickAngle < currentAngle + sliceSweep) {
                                clicked = i
                                break
                            }
                            currentAngle += sliceSweep
                        }
                        selectedSlice = clicked
                    } else {
                        selectedSlice = null
                    }
                },
                onDrag = { change, _ ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = change.position.x - centerX
                    val dy = change.position.y - centerY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val strokeWidthX = 12.dp.toPx()
                    val radiusOuter = (kotlin.math.min(size.width, size.height)) / 2f
                    val radiusInner = radiusOuter - strokeWidthX * 2
                    if (dist in radiusInner..radiusOuter) {
                        val clickAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f
                        var currentAngle = 0f
                        var clicked = -1
                        for (i in data.indices) {
                            val sliceSweep = (data[i].second / totalSum) * 360f * progress
                            if (clickAngle >= currentAngle && clickAngle < currentAngle + sliceSweep) {
                                clicked = i
                                break
                            }
                            currentAngle += sliceSweep
                        }
                        if (clicked != -1) selectedSlice = clicked
                    } // preserve previous slice if dragged outside slightly, or set to null
                },
                onDragEnd = { selectedSlice = null },
                onDragCancel = { selectedSlice = null }
            )
        }.pointerInput(data) {
            detectTapGestures(
                onPress = { offset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    
                    // Calculate distance to check if within donut ring (roughly)
                    val dx = offset.x - centerX
                    val dy = offset.y - centerY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    
                    val strokeWidthX = 12.dp.toPx()
                    val radiusOuter = (kotlin.math.min(size.width, size.height)) / 2f
                    val radiusInner = radiusOuter - strokeWidthX * 2
                    
                    if (dist in radiusInner..radiusOuter) {
                        val clickAngle = (Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f
                        var currentAngle = 0f
                        var clicked = -1
                        for (i in data.indices) {
                            val sliceSweep = (data[i].second / totalSum) * 360f * progress
                            if (clickAngle >= currentAngle && clickAngle < currentAngle + sliceSweep) {
                                clicked = i
                                break
                            }
                            currentAngle += sliceSweep
                        }
                        selectedSlice = clicked
                    } else {
                        selectedSlice = null // Deselect if tapped outside/inside ring
                    }
                    tryAwaitRelease()
                    selectedSlice = null
                }
            )
        }) {
            val strokeWidth = 12.dp.toPx()
            val canvasSize = size.minDimension
            val radius = ((canvasSize - strokeWidth) / 2).coerceAtLeast(0f)
            val computedSize = (radius * 2).coerceAtLeast(0f)
            val rectSize = Size(computedSize, computedSize)
            val topLeftOffset = Offset(
                ((size.width - rectSize.width) / 2).coerceAtLeast(0f), 
                ((size.height - rectSize.height) / 2).coerceAtLeast(0f)
            )
            
            var currentStartAngle = -90f
            
            data.forEachIndexed { index, pair ->
                val sliceSweep = (pair.second / totalSum) * 360f * progress
                val color = colors.getOrElse(index) { Color.Gray }
                
                val currentStrokeWidth = if (selectedSlice == index) strokeWidth * 1.5f else strokeWidth
                val currentAlpha = if (selectedSlice == null || selectedSlice == index) 1f else 0.5f
                
                if (sliceSweep > 0f) {
                    drawArc(
                        color = color.copy(alpha = currentAlpha),
                        startAngle = currentStartAngle,
                        sweepAngle = sliceSweep,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = rectSize,
                        style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
                    )
                }
                currentStartAngle += sliceSweep
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val safeSelectedSlice = selectedSlice?.takeIf { it in data.indices }
            val displayTitle = safeSelectedSlice?.let { data[it].first } ?: centerText
            val displayValue = safeSelectedSlice?.let { "R$ %.2f".format(data[it].second) } ?: centerSubtext
            Text(
                text = displayTitle,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
            Text(
                text = displayValue,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

