package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.core.WifiState
import kotlin.math.roundToInt

@Composable
fun WifiConnectedExpandedContent(wifi: WifiState) {
    val colors = MaterialTheme.colorScheme
    val signalLevel = wifi.signalLevel?.coerceIn(0, 4)
    val signalProgress = signalLevel?.div(4f) ?: 0f
    val animatedSignalProgress = animateFloatAsState(
        targetValue = signalProgress,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "wifiSignalProgress",
    ).value
    val signalText = when (signalLevel) {
        4 -> "Excellent"
        3 -> "Good"
        2 -> "Fair"
        1 -> "Weak"
        0 -> "Very weak"
        else -> "Signal unavailable"
    }
    val networkName = wifi.ssid
        ?.takeUnless { it.isBlank() || it.equals("<unknown ssid>", ignoreCase = true) || it == "0x" }
        ?: "Wi-Fi network"
    val linkSpeed = wifi.linkSpeedMbps?.takeIf { it > 0 }?.let { "$it Mbps" } ?: "Unavailable"
    val ipAddress = wifi.ipAddress?.takeIf { it.isNotBlank() && it != "0.0.0.0" } ?: "Unavailable"
    val metered = when (wifi.isMetered) {
        true -> "Metered"
        false -> "Unmetered"
        null -> "Unknown"
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        val compact = maxHeight < 190.dp
        val veryCompact = maxHeight < 145.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (veryCompact) 5.dp else if (compact) 6.dp else 8.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(if (compact) 20.dp else 24.dp),
                color = colors.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = if (compact) 10.dp else 13.dp,
                        vertical = if (veryCompact) 6.dp else if (compact) 8.dp else 11.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.size(if (veryCompact) 32.dp else if (compact) 38.dp else 44.dp)
                            .clip(CircleShape).background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = "Wi-Fi connected",
                            modifier = Modifier.size(if (compact) 21.dp else 24.dp),
                            tint = colors.onPrimaryContainer,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Connected to Wi-Fi",
                            color = colors.onSurfaceVariant,
                            fontSize = if (veryCompact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                        Text(
                            text = networkName,
                            color = colors.onSurface,
                            fontSize = if (veryCompact) 13.sp else if (compact) 14.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!veryCompact) {
                            Text(
                                text = signalText,
                                color = colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                            )
                        }
                    }
                    WifiSignalBars(level = signalLevel, progress = animatedSignalProgress, compact = compact)
                }
            }

            if (veryCompact) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    WifiMetricCard("Signal", wifi.rssi?.let { "$it dBm" } ?: signalText, Modifier.weight(1f), true)
                    WifiMetricCard("Link speed", linkSpeed, Modifier.weight(1f), true)
                    WifiMetricCard("Band", wifi.band ?: "N/A", Modifier.weight(1f), true)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    WifiMetricCard("Frequency band", wifi.band ?: "Unavailable", Modifier.weight(1f), compact)
                    WifiMetricCard("Link speed", linkSpeed, Modifier.weight(1f), compact)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    WifiMetricCard("Signal strength", wifi.rssi?.let { "$it dBm" } ?: signalText, Modifier.weight(1f), compact)
                    WifiMetricCard("IP address", ipAddress, Modifier.weight(1f), compact)
                }
                if (!compact) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.secondaryContainer.copy(alpha = 0.62f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Data usage", color = colors.onSecondaryContainer, fontSize = 11.sp)
                            Text("$metered connection", color = colors.onSecondaryContainer, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = colors.surfaceContainer) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, color = colors.onSurfaceVariant, fontSize = if (compact) 8.sp else 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = colors.onSurface, fontSize = if (compact) 10.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WifiSignalBars(level: Int?, progress: Float, compact: Boolean) {
    val colors = MaterialTheme.colorScheme
    val activeBars = (progress * 4f).roundToInt().coerceIn(0, 4)
    Row(
        modifier = Modifier.height(if (compact) 22.dp else 27.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier.size(width = 4.dp, height = (6 + index * if (compact) 4 else 6).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (level != null && index < activeBars) colors.primary else colors.onSurface.copy(alpha = 0.14f)),
            )
        }
    }
}
