package com.vikram.expressiveisland.overlay.contents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.WifiState

@Composable
fun WifiConnectedExpandedContent(
    wifi: WifiState,
) {
    val contentColor = LocalContentColor.current
    val secondaryColor = contentColor.copy(alpha = 0.68f)

    val signalLevel = wifi.signalLevel?.coerceIn(0, 4)

    val signalText = when (signalLevel) {
        4 -> "Excellent signal"
        3 -> "Good signal"
        2 -> "Fair signal"
        1 -> "Weak signal"
        else -> "Signal unavailable"
    }

    val networkName = wifi.ssid
        ?.takeUnless { it.isBlank() || it == "<unknown ssid>" }
        ?: "Unknown network"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = Color(0xFF60A5FA),
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.event_wifi_connected,
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    )

                    Text(
                        text = networkName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                    )

                    Text(
                        text = buildString {
                            append(signalText)

                            wifi.band?.let {
                                append(" • ")
                                append(it)
                            }
                        },
                        fontSize = 11.sp,
                        color = secondaryColor,
                    )
                }

                WifiSignalIndicator(
                    level = signalLevel,
                )
            }
        }
    }
}

@Composable
private fun WifiSignalIndicator(
    level: Int?,
) {
    val activeColor = Color(0xFF60A5FA)
    val inactiveColor =
        LocalContentColor.current.copy(alpha = 0.14f)

    Row(
        modifier = Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((7 + index * 5).dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 3.dp,
                            topEnd = 3.dp,
                            bottomStart = 3.dp,
                            bottomEnd = 3.dp,
                        ),
                    )
                    .background(
                        if (level != null && index < level) {
                            activeColor
                        } else {
                            inactiveColor
                        },
                    ),
            )
        }
    }
}