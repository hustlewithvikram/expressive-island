package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.data.DownloadProgressStyle
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.resolve
import kotlin.math.sin

@Composable
fun DownloadExpandedContent(event: IslandEvent, collapsedHeightDp: Int) {
    val options = event.download ?: return
    val progress = event.progressData
    val fraction = progress?.takeIf { !it.isIndeterminate && it.max > 0 }
        ?.let { (it.current.toFloat() / it.max).coerceIn(0f, 1f) }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction ?: 0f,
        label = "downloadExpandedProgress",
    )
    val indeterminateTransition = rememberInfiniteTransition(label = "downloadImmersiveIndeterminate")
    val indeterminateFraction by indeterminateTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "downloadImmersiveIndeterminateFraction",
    )
    val displayFraction = if (fraction == null) indeterminateFraction else animatedFraction
    val percentage = fraction?.let { (it * 100).toInt() }
    val progressColor = options.progressColor?.resolve() ?: MaterialTheme.colorScheme.primary
    val trackColor = options.trackColor?.resolve() ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val secondaryDetail = event.detail
        ?.replace(Regex("^\\s*\\d{1,3}%\\s*[•·|–-]?\\s*"), "")
        ?.takeIf { it.isNotBlank() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (options.immersiveProgress && options.showProgress) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = progressColor.copy(alpha = 0.22f),
                            topLeft = Offset.Zero,
                            size = Size(size.width * displayFraction, size.height),
                            cornerRadius = CornerRadius(30.dp.toPx()),
                        )
                    }
                } else Modifier
            )
            .padding(start = 18.dp, end = 18.dp, top = collapsedHeightDp.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(event = event, badgeSize = 44.dp, iconSize = 25.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (options.showText) {
                            Text(
                                text = event.label,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            Box(Modifier.weight(1f))
                        }
                        if (options.showPercentage) {
                            Text(
                                text = percentage?.let { "$it%" } ?: "…",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (options.showText && !secondaryDetail.isNullOrBlank()) {
                        Text(
                            text = secondaryDetail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else if (options.showText && progress != null && !progress.isIndeterminate && progress.max > 0) {
                        Text(
                            text = "${progress.current} / ${progress.max}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                    if (options.showProgress && !options.immersiveProgress) {
                        DownloadProgressBar(
                            fraction = displayFraction,
                            color = progressColor,
                            trackColor = trackColor,
                            style = options.progressStyle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressBar(
    fraction: Float,
    color: Color,
    trackColor: Color,
    style: DownloadProgressStyle,
) {
    val normalized = fraction.coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxWidth().height(7.dp)) {
        val centerY = size.height / 2f
        val radius = size.height / 2f
        when (style) {
            DownloadProgressStyle.SEGMENTED -> {
                val count = 16
                val gap = 3.dp.toPx()
                val segmentWidth = (size.width - gap * (count - 1)) / count
                val amount = normalized * count
                repeat(count) { index ->
                    val left = index * (segmentWidth + gap)
                    drawRoundRect(trackColor, Offset(left, 0f), Size(segmentWidth, size.height), CornerRadius(radius))
                    val fill = (amount - index).coerceIn(0f, 1f)
                    if (fill > 0f) drawRoundRect(
                        color, Offset(left, 0f), Size(segmentWidth * fill, size.height), CornerRadius(radius)
                    )
                }
            }
            DownloadProgressStyle.DOTTED -> {
                val count = 38
                val dotRadius = radius * 0.72f
                repeat(count) { index ->
                    val x = (index + 0.5f) * size.width / count
                    val filled = (index + 1f) / count <= normalized
                    drawCircle(if (filled) color else trackColor, dotRadius, Offset(x, centerY))
                }
            }
            DownloadProgressStyle.WAVY -> {
                drawLine(trackColor, Offset(0f, centerY), Offset(size.width, centerY), size.height * 0.48f, cap = StrokeCap.Round)
                val path = Path()
                val endX = size.width * normalized
                val steps = 80
                for (i in 0..steps) {
                    val x = endX * i / steps
                    val y = centerY + sin(i * 0.55f).toFloat() * size.height * 0.22f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = size.height * 0.72f, cap = StrokeCap.Round))
            }
            DownloadProgressStyle.EXPRESSIVE -> {
                drawLine(trackColor, Offset(0f, centerY), Offset(size.width, centerY), size.height * 0.48f, cap = StrokeCap.Round)
                drawLine(
                    color, Offset(0f, centerY), Offset(size.width * normalized, centerY),
                    size.height, cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 4.dp.toPx()), 0f),
                )
            }
            DownloadProgressStyle.LINEAR, DownloadProgressStyle.CIRCULAR -> {
                drawLine(trackColor, Offset(0f, centerY), Offset(size.width, centerY), size.height, cap = StrokeCap.Round)
                if (normalized > 0f) drawLine(
                    color, Offset(0f, centerY), Offset(size.width * normalized, centerY),
                    size.height, cap = StrokeCap.Round,
                )
            }
        }
    }
}
