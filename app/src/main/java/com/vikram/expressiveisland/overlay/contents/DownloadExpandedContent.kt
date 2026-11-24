package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.data.DownloadProgressStyle
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.resolve

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
    val percentage = fraction?.let { (it * 100).toInt() }
    val progressColor = options.progressColor?.resolve() ?: MaterialTheme.colorScheme.primary
    val trackColor = options.trackColor?.resolve() ?: MaterialTheme.colorScheme.surfaceContainerHighest

    // Expanded content starts below the resting pill and is anchored to the bottom, just like
    // the timer tile. This avoids the title and progress bar crowding the camera cutout.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = collapsedHeightDp.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(event = event, badgeSize = 44.dp, iconSize = 25.dp)
                if (options.showText) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = event.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!event.detail.isNullOrBlank()) {
                            Text(
                                text = event.detail,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else if (progress != null && !progress.isIndeterminate && progress.max > 0) {
                            Text(
                                text = "${progress.current} / ${progress.max}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Box(Modifier.weight(1f))
                }
                if (options.showPercentage) {
                    Text(
                        text = percentage?.let { "$it%" } ?: "…",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (options.showProgress) {
                when (options.progressStyle) {
                    DownloadProgressStyle.LINEAR -> {
                        if (fraction == null) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = progressColor,
                                trackColor = trackColor,
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { animatedFraction },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = progressColor,
                                trackColor = trackColor,
                            )
                        }
                    }
                    DownloadProgressStyle.CIRCULAR -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (fraction == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(34.dp),
                                    color = progressColor,
                                    trackColor = trackColor,
                                    strokeWidth = 4.dp,
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { animatedFraction },
                                    modifier = Modifier.size(34.dp),
                                    color = progressColor,
                                    trackColor = trackColor,
                                    strokeWidth = 4.dp,
                                )
                            }
                        }
                    }
                    DownloadProgressStyle.SEGMENTED -> {
                        if (fraction == null) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = progressColor,
                                trackColor = trackColor,
                            )
                        } else {
                            SegmentedDownloadProgress(
                                fraction = animatedFraction,
                                color = progressColor,
                                trackColor = trackColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedDownloadProgress(
    fraction: Float,
    color: Color,
    trackColor: Color,
) {
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val count = 14
        val gap = 3.dp.toPx()
        val segmentWidth = (size.width - gap * (count - 1)) / count
        val progress = fraction.coerceIn(0f, 1f) * count
        repeat(count) { index ->
            val segmentFraction = (progress - index).coerceIn(0f, 1f)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(index * (segmentWidth + gap), 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(size.height / 2f),
            )
            if (segmentFraction > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(index * (segmentWidth + gap), 0f),
                    size = Size(segmentWidth * segmentFraction, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
        }
    }
}
