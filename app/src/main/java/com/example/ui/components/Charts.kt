package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.ChartPoint
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.BorderColor

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
    val stockPercent = if (total > 0) (stockValue / total).toFloat() else 0.5f
    val fiiPercent = if (total > 0) (fiiValue / total).toFloat() else 0.5f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(BorderColor, shape = RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stockPercent > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(stockPercent)
                        .background(
                            brush = Brush.horizontalGradient(listOf(GoldPrimary, Color(0xFFFBBF24))),
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        )
                )
            }
            if (fiiPercent > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(listOf(SuccessGreen, Color(0xFF059669))),
                            shape = if (stockPercent > 0f) {
                                RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            } else {
                                RoundedCornerShape(12.dp)
                            }
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
                Box(modifier = Modifier.size(10.dp).background(GoldPrimary, shape = RoundedCornerShape(5.dp)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ações: ${String.format("%.1f", stockPercent * 100)}%",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(SuccessGreen, shape = RoundedCornerShape(5.dp)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FIIs: ${String.format("%.1f", fiiPercent * 100)}%",
                    color = TextPrimary,
                    fontSize = 12.sp,
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
    if (points.isEmpty()) return

    val minPrice = points.minOf { it.close }
    val maxPrice = points.maxOf { it.close }
    val priceRange = maxPrice - minPrice
    val paddingPercent = 0.15f // add vertical breathing space

    val displayMin = minPrice - (priceRange * paddingPercent)
    val displayMax = maxPrice + (priceRange * paddingPercent)
    val displayRange = if (displayMax - displayMin > 0.0) displayMax - displayMin else 1.0

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw background horizontal grid lines (3 grid lines)
                val gridLines = 3
                for (g in 0..gridLines) {
                    val y = height * g / gridLines
                    drawLine(
                        color = BorderColor.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // If only 1 point, center it
                if (points.size == 1) {
                    drawCircle(
                        color = lineColor,
                        radius = 6.dp.toPx(),
                        center = Offset(width / 2, height / 2)
                    )
                    return@Canvas
                }

                val path = Path()
                val bgPath = Path()
                
                val itemWidth = width / (points.size - 1)

                points.forEachIndexed { index, point ->
                    val x = index * itemWidth
                    // Calculate normalized Y coordinate (inverted because 0 is at the top)
                    val normalizedY = ((point.close - displayMin) / displayRange).toFloat()
                    val y = height - (normalizedY * height)

                    if (index == 0) {
                        path.moveTo(x, y)
                        bgPath.moveTo(x, height)
                        bgPath.lineTo(x, y)
                    } else {
                        // Curved smooth bezier transition
                        val prevX = (index - 1) * itemWidth
                        val prevNormalizedY = ((points[index - 1].close - displayMin) / displayRange).toFloat()
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
                    
                    if (index == points.size - 1) {
                        bgPath.lineTo(x, height)
                        bgPath.close()
                    }
                }

                // Draw soft filled glow background under line
                drawPath(
                    path = bgPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
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

                // Highlight the absolute last price point
                val lastNormalY = ((points.last().close - displayMin) / displayRange).toFloat()
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(width, height - (lastNormalY * height))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Draw date and price extremities below the chart
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.firstOrNull()?.dateLabel ?: "",
                color = TextSecondary,
                fontSize = 11.sp
            )
            Text(
                text = "Min: R$ ${String.format("%.2f", minPrice)}  |  Máx: R$ ${String.format("%.2f", maxPrice)}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = points.lastOrNull()?.dateLabel ?: "",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
