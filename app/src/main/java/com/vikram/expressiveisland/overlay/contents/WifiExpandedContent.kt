package com.vikram.expressiveisland.overlay.contents

import android.net.TrafficStats
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.core.WifiState
import kotlinx.coroutines.delay
import kotlin.math.max

private const val TRAFFIC_SAMPLE_INTERVAL_MS = 1_000L
private val WIFI_NOTCH_GAP = 8.dp

private data class WifiTrafficSnapshot(
    val rxBytes: Long,
    val txBytes: Long,
    val rxRateBytesPerSecond: Long = 0L,
    val txRateBytesPerSecond: Long = 0L,
)

@Composable
fun WifiConnectedExpandedContent(
    wifi: WifiState,
    collapsedHeightDp: Int,
) {
    val colors = MaterialTheme.colorScheme
    val networkName = wifi.ssid
        ?.takeUnless { it.isBlank() || it.equals("<unknown ssid>", true) || it == "0x" }
        ?: "Wi-Fi network"
    val signalText = when (wifi.signalLevel?.coerceIn(0, 4)) {
        4 -> "Excellent signal"
        3 -> "Good signal"
        2 -> "Fair signal"
        1 -> "Weak signal"
        0 -> "Very weak signal"
        else -> "Connected"
    }

    // TrafficStats is sampled once per second. Deltas provide live throughput and session
    // totals without polling from the system-event monitor or introducing another global bus.
    var traffic by remember(wifi.ssid) {
        mutableStateOf(
            WifiTrafficSnapshot(rxBytes = 0L, txBytes = 0L)
        )
    }
    LaunchedEffect(wifi.ssid) {
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        var sessionRx = 0L
        var sessionTx = 0L
        while (true) {
            delay(TRAFFIC_SAMPLE_INTERVAL_MS)
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            if (previousRx >= 0L && currentRx >= previousRx) {
                val delta = currentRx - previousRx
                sessionRx += delta
                traffic = traffic.copy(
                    rxBytes = sessionRx,
                    rxRateBytesPerSecond = delta,
                )
            }
            if (previousTx >= 0L && currentTx >= previousTx) {
                val delta = currentTx - previousTx
                sessionTx += delta
                traffic = traffic.copy(
                    txBytes = sessionTx,
                    txRateBytesPerSecond = delta,
                )
            }
            previousRx = currentRx
            previousTx = currentTx
        }
    }

    val rxProgress by animateFloatAsState(
        targetValue = traffic.rxBytes.toFloat() / max(traffic.rxBytes, traffic.txBytes).coerceAtLeast(1L),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "wifiDownloadProgress",
    )
    val txProgress by animateFloatAsState(
        targetValue = traffic.txBytes.toFloat() / max(traffic.rxBytes, traffic.txBytes).coerceAtLeast(1L),
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "wifiUploadProgress",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        // The pill occupies the top part of the expanded island. Use its actual configured
        // height, then leave a small optical gap before placing any Wi-Fi content.
        val reservedTop = collapsedHeightDp.dp + WIFI_NOTCH_GAP
        val contentHeight = (maxHeight - reservedTop).coerceAtLeast(0.dp)
        val compact = contentHeight < 78.dp
        val tiny = contentHeight < 58.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(reservedTop))
            if (contentHeight > 0.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentHeight),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (compact) 4.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TrafficSpeed(
                            value = formatRate(traffic.rxRateBytesPerSecond),
                            label = "MB/S IN",
                            compact = compact,
                        )
                        TrafficSpeed(
                            value = formatRate(traffic.txRateBytesPerSecond),
                            label = "MB/S OUT",
                            compact = compact,
                            alignEnd = true,
                        )
                    }

                    Spacer(Modifier.height(if (tiny) 4.dp else 7.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TrafficTotal(
                            title = "DOWNLOADED",
                            value = formatBytes(traffic.rxBytes),
                            progress = rxProgress,
                            modifier = Modifier.weight(1f),
                            compact = compact,
                        )
                        TrafficTotal(
                            title = "UPLOADED",
                            value = formatBytes(traffic.txBytes),
                            progress = txProgress,
                            modifier = Modifier.weight(1f),
                            compact = compact,
                        )
                    }

                    if (!tiny) {
                        Spacer(Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Wifi,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.height(12.dp),
                                )
                                Text(
                                    text = networkName,
                                    color = colors.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                            }
                            Text(
                                text = signalText,
                                color = colors.onSurfaceVariant,
                                fontSize = 8.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficSpeed(
    value: String,
    label: String,
    compact: Boolean,
    alignEnd: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = value,
            color = colors.primary,
            fontSize = if (compact) 20.sp else 25.sp,
            lineHeight = if (compact) 22.sp else 27.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = label,
            color = colors.onSurfaceVariant,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun TrafficTotal(
    title: String,
    value: String,
    progress: Float,
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 5.dp),
    ) {
        Text(
            text = title,
            color = colors.onSurfaceVariant,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = value,
            color = colors.onSurface,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 3.dp else 4.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
                    .background(colors.primary),
            )
        }
    }
}

private fun formatRate(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0L) return "0.00"
    return String.format(java.util.Locale.US, "%.2f", bytesPerSecond / 1_000_000.0)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000L -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
