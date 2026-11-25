package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.state.CategorySpend
import com.example.ui.state.DailySpend
import kotlin.math.roundToInt

@Composable
fun CategoryDonutChart(
    categories: List<CategorySpend>,
    totalExpense: Long,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "donut_anim"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(132.dp)
        ) {
            Canvas(modifier = Modifier.size(132.dp)) {
                val strokeWidth = 18.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                if (categories.isNotEmpty() && totalExpense > 0L) {
                    var startAngle = -90f
                    val gapDegrees = if (categories.size > 1) 4f else 0f
                    categories.forEach { item ->
                        val rawSweep = item.fraction * 360f * animProgress
                        val sweep = (rawSweep - gapDegrees).coerceAtLeast(2f)
                        drawArc(
                            color = categoryColor(item.category),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += rawSweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCompactRupiah(totalExpense),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.take(4).forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(categoryColor(item.category))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(item.fraction * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (categories.size > 4) {
                Text(
                    text = "+${categories.size - 4} kategori lainnya",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeeklySpendBarChart(
    bars: List<DailySpend>,
    modifier: Modifier = Modifier
) {
    val maxVal = (bars.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 8.dp)
        ) {
            if (bars.isEmpty()) return@Canvas
            val count = bars.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.42f).coerceAtMost(36.dp.toPx())
            val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            bars.forEachIndexed { index, item ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = corner
                )
                val ratio = (item.amount.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val barHeight = if (item.amount > 0L) (size.height * ratio).coerceAtLeast(10.dp.toPx()) else 0f
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = corner
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bars.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.dayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (item.amount > 0L) formatCompactRupiah(item.amount) else "-",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsProgressRing(
    progress: Float,
    accentColor: Color,
    size: Dp = 54.dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(450),
        label = "savings_ring"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 5.5.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = "${(animatedProgress * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
