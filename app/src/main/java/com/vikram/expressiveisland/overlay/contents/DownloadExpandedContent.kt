package com.vikram.expressiveisland.overlay.contents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.vikram.expressiveisland.overlay.resolve
import com.vikram.expressiveisland.overlay.island.IslandEvent

@Composable
fun DownloadExpandedContent(event: IslandEvent) {
    val options = event.download ?: return
    val progress = event.progressData
    val fraction = progress?.takeIf { !it.isIndeterminate && it.max > 0 }
        ?.let { (it.current.toFloat() / it.max).coerceIn(0f, 1f) }
    val percentage = fraction?.let { (it * 100).toInt() }
    val progressColor = options.progressColor?.resolve() ?: MaterialTheme.colorScheme.primary
    val trackColor = options.trackColor?.resolve() ?: MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = event.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (options.showText && !event.detail.isNullOrBlank()) {
                    Text(
                        text = event.detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (options.showPercentage && percentage != null) {
                Text(
                    text = "$percentage%",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
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
                            progress = { fraction },
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
                                progress = { fraction },
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
                            fraction = fraction,
                            color = progressColor,
                            trackColor = trackColor,
                        )
                    }
                }
            }
        }
        if (options.showPercentage && percentage == null) {
            Text(
                text = "Downloading",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
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
        val filled = (fraction.coerceIn(0f, 1f) * count).toInt()
        repeat(count) { index ->
            drawRoundRect(
                color = if (index < filled || (index == filled && fraction * count % 1f > 0f)) color else trackColor,
                topLeft = Offset(index * (segmentWidth + gap), 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(size.height / 2f),
            )
        }
    }
}
