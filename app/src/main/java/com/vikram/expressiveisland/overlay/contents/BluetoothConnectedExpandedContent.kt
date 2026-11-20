package com.vikram.expressiveisland.overlay.contents

import android.media.AudioDeviceInfo
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
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

@Composable
fun BluetoothConnectedExpandedContent(
    deviceName: String?,
    deviceType: Int?,
    notchBottomInsetDp: Int,
    leftBattery: Int? = null,
    rightBattery: Int? = null,
    caseBattery: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    val name = deviceName?.takeIf { it.isNotBlank() } ?: "Bluetooth device"
    val isAudioRoute = deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        val contentHeight = (maxHeight - notchBottomInsetDp.coerceAtLeast(0).dp).coerceAtLeast(0.dp)
        val compact = contentHeight < 112.dp
        val veryCompact = contentHeight < 78.dp

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(notchBottomInsetDp.coerceAtLeast(0).dp))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 7.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Transparent outer surface lets the expanded island itself provide the backdrop.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (compact) 20.dp else 24.dp),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(
                            horizontal = if (compact) 10.dp else 13.dp,
                            vertical = if (veryCompact) 6.dp else if (compact) 8.dp else 11.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 9.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Surface(
                                Modifier.size(if (compact) 34.dp else 42.dp),
                                shape = CircleShape,
                                color = colors.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.BluetoothConnected,
                                        contentDescription = "Bluetooth connected",
                                        tint = colors.onPrimaryContainer,
                                        modifier = Modifier.size(if (compact) 19.dp else 23.dp),
                                    )
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Bluetooth connected",
                                    color = colors.onSurface,
                                    fontSize = if (compact) 13.sp else 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    name,
                                    color = colors.onSurfaceVariant,
                                    fontSize = if (compact) 10.sp else 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Surface(shape = RoundedCornerShape(50), color = colors.secondaryContainer) {
                                Text(
                                    "CONNECTED",
                                    Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    color = colors.onSecondaryContainer,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        if (!veryCompact && isAudioRoute) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BatteryItem("L", leftBattery, Icons.Rounded.Headphones, Modifier.weight(1f), compact)
                                BatteryItem("R", rightBattery, Icons.Rounded.Headphones, Modifier.weight(1f), compact)
                                BatteryItem("CASE", caseBattery, Icons.Rounded.BatteryFull, Modifier.weight(1f), compact)
                            }
                        } else if (!veryCompact) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                BluetoothInfoBlock("DEVICE", "Accessory", Icons.Rounded.Headphones, Modifier.weight(1f), compact)
                                BluetoothInfoBlock("LINK", "Connected", Icons.Rounded.BluetoothConnected, Modifier.weight(1f), compact)
                                BluetoothInfoBlock("AUDIO", "Available", Icons.Rounded.MusicNote, Modifier.weight(1f), compact)
                            }
                        }

                        if (isAudioRoute) {
                            Text(
                                "A2DP • AAC",
                                Modifier.align(Alignment.CenterHorizontally),
                                color = colors.onSurfaceVariant,
                                fontSize = if (compact) 9.sp else 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.25.sp,
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
private fun BatteryItem(
    label: String,
    percentage: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceContainerHigh.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = if (compact) 7.dp else 9.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, color = colors.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                percentage?.takeIf { it in 0..100 }?.let { "$it%" } ?: "—",
                color = colors.onSurface,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BluetoothInfoBlock(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    compact: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(15.dp), color = colors.secondaryContainer.copy(alpha = 0.58f)) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = if (compact) 7.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, null, tint = colors.onSecondaryContainer, modifier = Modifier.size(15.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = colors.onSecondaryContainer.copy(alpha = 0.72f), fontSize = 8.sp, maxLines = 1)
                Text(value, color = colors.onSecondaryContainer, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
