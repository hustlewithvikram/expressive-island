package com.vikram.expressiveisland.overlay.contents

import android.net.TrafficStats
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import java.util.Locale

private const val TRAFFIC_SAMPLE_INTERVAL_MS = 1_000L

private data class WifiTrafficSnapshot(
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
    val band = wifi.band
    val security = wifi.security

    var traffic by remember(wifi.ssid) { mutableStateOf(WifiTrafficSnapshot()) }
    LaunchedEffect(wifi.ssid) {
        var previousRx = TrafficStats.getTotalRxBytes()
        var previousTx = TrafficStats.getTotalTxBytes()
        while (true) {
            delay(TRAFFIC_SAMPLE_INTERVAL_MS)
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()
            val rxDelta = if (previousRx >= 0L && currentRx >= previousRx) currentRx - previousRx else 0L
            val txDelta = if (previousTx >= 0L && currentTx >= previousTx) currentTx - previousTx else 0L
            traffic = WifiTrafficSnapshot(rxRateBytesPerSecond = rxDelta, txRateBytesPerSecond = txDelta)
            previousRx = currentRx
            previousTx = currentTx
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
    ) {
        val contentHeight = (maxHeight - notchBottomInsetDp.coerceAtLeast(0).dp).coerceAtLeast(0.dp)
        val compact = contentHeight < 76.dp
        val veryCompact = contentHeight < 52.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(notchBottomInsetDp.coerceAtLeast(0).dp))
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .padding(top = if (compact) 1.dp else 5.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Connected to",
                        color = colors.onSurfaceVariant,
                        fontSize = if (veryCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    Text(
                        text = networkName,
                        color = colors.onSurface,
                        fontSize = if (veryCompact) 13.sp else if (compact) 15.sp else 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val hasMetadata = band != null || security != null || wifi.signalLevel != null
                    if (hasMetadata) {
                        Row(
                            modifier = Modifier.padding(top = if (compact) 4.dp else 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            var hasPrevious = false
                            if (band != null) {
                                Text(band, color = colors.onSurfaceVariant, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1)
                                hasPrevious = true
                            }
                            if (security != null) {
                                if (hasPrevious) MetaDot()
                                Text(security, color = colors.onSurfaceVariant, fontSize = if (compact) 9.sp else 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                hasPrevious = true
                            }
                            if (wifi.signalLevel != null) {
                                if (hasPrevious) MetaDot()
                                WifiSignalBars(level = wifi.signalLevel, modifier = Modifier.size(width = 24.dp, height = 15.dp))
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.width(if (compact) 88.dp else 100.dp).fillMaxHeight(if (compact) 0.82f else 0.78f),
                    shape = RoundedCornerShape(22.dp),
                    color = colors.surfaceContainerHigh,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = if (compact) 5.dp else 8.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SpeedValue(value = formatRate(traffic.txRateBytesPerSecond), arrow = "↑", label = "UP", compact = compact, tint = colors.primary)
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 3.dp else 5.dp)
                                .height(1.dp).background(colors.outlineVariant.copy(alpha = 0.65f)),
                        )
                        SpeedValue(value = formatRate(traffic.rxRateBytesPerSecond), arrow = "↓", label = "DOWN", compact = compact, tint = colors.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedValue(
    value: String,
    arrow: String,
    label: String,
    compact: Boolean,
    tint: androidx.compose.ui.graphics.Color,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = value, color = tint, fontSize = if (compact) 11.sp else 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            Text(text = arrow, color = tint, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun MetaDot() {
    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
}

@Composable
private fun WifiSignalBars(level: Int?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val active = (level ?: 0).coerceIn(0, 4)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        repeat(4) { index ->
            Box(
                modifier = Modifier.weight(1f).height((4 + index * 3).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(if (index < active) colors.primary else colors.outlineVariant.copy(alpha = 0.65f)),
            )
        }
    }
}

private fun formatRate(bytesPerSecond: Long): String {
    // Convert bytes/s to megabits/s, the unit shown in the speed capsule.
    val mbps = bytesPerSecond.coerceAtLeast(0L) * 8.0 / 1_000_000.0
    val decimals = when { mbps >= 100.0 -> 0; mbps >= 10.0 -> 1; else -> 2 }
    return String.format(Locale.US, "%." + decimals + "f Mbps", mbps)
}