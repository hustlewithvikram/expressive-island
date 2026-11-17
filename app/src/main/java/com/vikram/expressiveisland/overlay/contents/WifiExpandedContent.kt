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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
fun WifiConnectedExpandedContent(
    wifi: WifiState,
) {
    val colors = MaterialTheme.colorScheme
    val signalLevel = wifi.signalLevel?.coerceIn(0, 4)
    val signalProgress = signalLevel?.div(4f) ?: 0f
    val animatedSignalProgress = animateFloatAsState(
        targetValue = signalProgress,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
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
        ?.takeUnless { it.isBlank() || it.equals("<unknown ssid>", ignoreCase = true) }
        ?: "Wi-Fi network"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
    ) {
        val compact = maxHeight < 250.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.BottomStart)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp, Alignment.Bottom),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (compact) 11.dp else 13.dp, vertical = if (compact) 9.dp else 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 40.dp else 46.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = "Wi-Fi connected",
                            modifier = Modifier.size(if (compact) 23.dp else 26.dp),
                            tint = colors.onPrimaryContainer,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Wi-Fi connected",
                            color = colors.onSurfaceVariant,
                            fontSize = if (compact) 10.sp else 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = networkName,
                            color = colors.onSurface,
                            fontSize = if (compact) 14.sp else 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                append(signalText)
                                wifi.band?.let {
                                    append(" · ")
                                    append(it)
                                }
                            },
                            color = colors.onSurfaceVariant,
                            fontSize = if (compact) 10.sp else 11.sp,
                            maxLines = 1,
                        )
                    }

                    WifiSignalBars(
                        level = signalLevel,
                        progress = animatedSignalProgress,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
            ) {
                WifiMetricCard(
                    label = "Frequency band",
                    value = wifi.band ?: "Unavailable",
                    modifier = Modifier.weight(1f),
                    compact = compact,
                )
                WifiMetricCard(
                    label = "Link speed",
                    value = wifi.linkSpeedMbps
                        ?.takeIf { it > 0 }
                        ?.let { "$it Mbps" }
                        ?: "Unavailable",
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                WifiMetricCard(
                    label = "Signal strength",
                    value = wifi.rssi
                        ?.let { "$it dBm" }
                        ?: signalText,
                    modifier = Modifier.weight(1f),
                )
                WifiMetricCard(
                    label = "IP address",
                    value = wifi.ipAddress ?: "Unavailable",
                    modifier = Modifier.weight(1f),
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Data usage",
                        color = colors.onSurfaceVariant,
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = when (wifi.isMetered) {
                            true -> "Metered connection"
                            false -> "Unmetered connection"
                            null -> "Status unavailable"
                        },
                        color = colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = colors.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (compact) 7.dp else 9.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
        ) {
            Text(
                text = label,
                color = colors.onSurfaceVariant,
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = colors.onSurface,
                fontSize = if (compact) 11.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WifiSignalBars(
    level: Int?,
    progress: Float,
) {
    val colors = MaterialTheme.colorScheme
    val activeBars = (progress * 4f).roundToInt().coerceIn(0, 4)

    Row(
        modifier = Modifier.height(27.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = (7 + index * 6).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (level != null && index < activeBars) {
                            colors.primary
                        } else {
                            colors.onSurface.copy(alpha = 0.13f)
                        },
                    ),
            )
        }
    }
}
