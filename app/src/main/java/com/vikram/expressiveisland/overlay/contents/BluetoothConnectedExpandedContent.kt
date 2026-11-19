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
) {
    val colors = MaterialTheme.colorScheme
    val name = deviceName?.takeIf { it.isNotBlank() } ?: "Bluetooth device"
    val isAudioRoute = deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_SCO

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
    ) {
        val contentHeight = (maxHeight - notchBottomInsetDp.coerceAtLeast(0).dp).coerceAtLeast(0.dp)
        val compact = contentHeight < 100.dp
        val veryCompact = contentHeight < 72.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(notchBottomInsetDp.coerceAtLeast(0).dp))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (compact) 20.dp else 24.dp),
                    color = colors.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = if (compact) 10.dp else 13.dp,
                            vertical = if (veryCompact) 8.dp else if (compact) 10.dp else 13.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Surface(
                                modifier = Modifier.size(if (compact) 34.dp else 42.dp),
                                shape = CircleShape,
                                color = colors.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.BluetoothConnected,
                                        contentDescription = "Bluetooth connected",
                                        tint = colors.onPrimaryContainer,
                                        modifier = Modifier.size(if (compact) 19.dp else 23.dp),
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "Bluetooth connected",
                                    color = colors.onSurface,
                                    fontSize = if (compact) 13.sp else 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = name,
                                    color = colors.onSurfaceVariant,
                                    fontSize = if (compact) 10.sp else 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.secondaryContainer,
                            ) {
                                Text(
                                    text = "CONNECTED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    color = colors.onSecondaryContainer,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }

                        if (!veryCompact) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                BluetoothInfoBlock(
                                    modifier = Modifier.weight(1f),
                                    label = "DEVICE",
                                    value = if (isAudioRoute) "Earbuds" else "Accessory",
                                    icon = Icons.Rounded.Headphones,
                                    compact = compact,
                                )
                                BluetoothInfoBlock(
                                    modifier = Modifier.weight(1f),
                                    label = "LINK",
                                    value = "Connected",
                                    icon = Icons.Rounded.BluetoothConnected,
                                    compact = compact,
                                )
                                BluetoothInfoBlock(
                                    modifier = Modifier.weight(1f),
                                    label = "AUDIO",
                                    value = if (isAudioRoute) "Ready" else "Available",
                                    icon = Icons.Rounded.MusicNote,
                                    compact = compact,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(4.dp).clip(CircleShape)
                                    .background(colors.primary),
                            )
                            Text(
                                text = "A2DP • AAC",
                                modifier = Modifier.padding(start = 6.dp),
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
private fun BluetoothInfoBlock(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    compact: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = colors.secondaryContainer.copy(alpha = 0.58f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = if (compact) 7.dp else 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSecondaryContainer,
                modifier = Modifier.size(if (compact) 15.dp else 18.dp),
            )
            Text(
                text = label,
                color = colors.onSecondaryContainer.copy(alpha = 0.72f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = value,
                color = colors.onSecondaryContainer,
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
