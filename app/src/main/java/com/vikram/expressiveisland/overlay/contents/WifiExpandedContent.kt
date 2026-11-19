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

private data class WifiTrafficSnapshot(
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val rxRateBytesPerSecond: Long = 0L,
    val txRateBytesPerSecond: Long = 0L,
)

@Composable
fun WifiConnectedExpandedContent(
    wifi: WifiState,
    notchBottomInsetDp: Int,
) {
    val colors = MaterialTheme.colorScheme
    val networkName = wifi.ssid
        ?.takeUnless { it.isBlank() || it.equals("<unknown ssid>", true) || it == "0x" }
        ?: "Wi-Fi network"
    val signalText = when (wifi.signalLevel?.coerceIn(0, 4)) {
        4 -> "Excellent"
        3 -> "Good"
        2 -> "Fair"
        1 -> "Weak"
        0 -> "Very weak"
        else -> "Connected"
    }

    // Sample total device traffic once per second and show session deltas while this tile is open.
    var traffic by remember(wifi.ssid) { mutableStateOf(WifiTrafficSnapshot()) }
    LaunchedEffect(wifi.ssid) {
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        var sessionRx = 0L
        var sessionTx = 0L
        while (true) {
            delay(TRAFFIC_SAMPLE_INTERVAL_MS)
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            val rxDelta = if (previousRx >= 0L && currentRx >= previousRx) currentRx - previousRx else 0L
            val txDelta = if (previousTx >= 0L && currentTx >= previousTx) currentTx - previousTx else 0L
            sessionRx += rxDelta
            sessionTx += txDelta
            traffic = WifiTrafficSnapshot(sessionRx, sessionTx, rxDelta, txDelta)
            previousRx = currentRx
            previousTx = currentTx
        }
    }

    val maxTotal = max(traffic.rxBytes, traffic.txBytes).coerceAtLeast(1L)
    val rxProgress by animateFloatAsState(
        targetValue = traffic.rxBytes.toFloat() / maxTotal,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "wifiDownloadProgress",
    )
    val txProgress by animateFloatAsState(
        targetValue = traffic.txBytes.toFloat() / maxTotal,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "wifiUploadProgress",
    )

    // IMPORTANT: constraints are measured from the actual expanded surface. Reserve the
    // notch's bottom edge using both pill and expanded offsets, then give the rest to content.
    // A weighted viewport guarantees the content can never claim more height than remains.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(notchBottomInsetDp.coerceAtLeast(0).dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            val availableHeight = maxHeight
            val ultraCompact = availableHeight < 48.dp
            val compact = availableHeight < 82.dp

            if (ultraCompact) {
                // At extremely small custom heights keep only the two live readings.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TrafficSpeed(formatRate(traffic.rxRateBytesPerSecond), "IN", true)
                    TrafficSpeed(formatRate(traffic.txRateBytesPerSecond), "OUT", true, true)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TrafficSpeed(formatRate(traffic.rxRateBytesPerSecond), "MB/S IN", compact)
                        TrafficSpeed(formatRate(traffic.txRateBytesPerSecond), "MB/S OUT", compact, true)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TrafficTotal(
                            title = "DOWNLOADED",
                            value = formatBytes(traffic.rxBytes),
                            progress = rxProgress,
                            compact = compact,
                            modifier = Modifier.weight(1f),
                        )
                        TrafficTotal(
                            title = "UPLOADED",
                            value = formatBytes(traffic.txBytes),
                            progress = txProgress,
                            compact = compact,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (!compact && availableHeight >= 104.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = networkName,
                                color = colors.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
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
            fontSize = if (compact) 17.sp else 25.sp,
            lineHeight = if (compact) 19.sp else 27.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = label,
            color = colors.onSurfaceVariant,
            fontSize = if (compact) 7.sp else 8.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun TrafficTotal(
    title: String,
    value: String,
    progress: Float,
    compact: Boolean,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
    ) {
        Text(
            text = title,
            color = colors.onSurfaceVariant,
            fontSize = if (compact) 7.sp else 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Text(
            text = value,
            color = colors.onSurface,
            fontSize = if (compact) 11.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 2.dp else 4.dp)
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

private fun formatRate(bytesPerSecond: Long): String =
    String.format(java.util.Locale.US, "%.2f", bytesPerSecond.coerceAtLeast(0L) / 1_000_000.0)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000L -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
