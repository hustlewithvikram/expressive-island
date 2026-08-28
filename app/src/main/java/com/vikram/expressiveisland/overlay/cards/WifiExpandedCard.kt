package com.vikram.expressiveisland.overlay.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.core.WifiState
import com.vikram.expressiveisland.data.AppearanceSettings

@Composable
fun WifiExpandedCard(
    wifi: WifiState,
    appearance: AppearanceSettings,
) {
    val contentColor = LocalContentColor.current
    val secondaryColor = contentColor.copy(alpha = 0.68f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = "Wi-Fi",
                    modifier = Modifier.size(44.dp),
                    tint = contentColor,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {

                Text(
                    text = "Wi-Fi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryColor,
                    maxLines = 1,
                )

                Text(
                    text = wifi.ssid ?: "Connected",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    wifi.band?.let {
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            color = secondaryColor,
                            maxLines = 1,
                        )
                    }

                    wifi.rssi?.let {
                        Text(
                            text = "$it dBm",
                            fontSize = 10.sp,
                            color = secondaryColor,
                            maxLines = 1,
                        )
                    }

                    wifi.linkSpeedMbps?.let {
                        Text(
                            text = "$it Mbps",
                            fontSize = 10.sp,
                            color = secondaryColor,
                            maxLines = 1,
                        )
                    }

                    wifi.isMetered?.let {
                        Text(
                            text = if (it) "Metered" else "Unmetered",
                            fontSize = 10.sp,
                            color = secondaryColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}