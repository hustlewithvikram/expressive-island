package com.vikram.expressiveisland.overlay.contents

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.core.UsbState

@Composable
fun UsbConnectedExpandedContent(
    usb: UsbState?,
    notchBottomInsetDp: Int,
) {
    val foreground = Color.White
    val secondary = Color.White.copy(alpha = 0.68f)
    val accent = Color(0xFF9CCBFA)
    val isPc = usb?.isPcConnection == true
    val deviceName = usb?.name?.takeIf(String::isNotBlank)
        ?: if (isPc) "Connected computer" else "USB device"
    val deviceType = usb?.deviceType?.takeIf(String::isNotBlank)
        ?: if (isPc) "Computer connection" else "External USB device"
    val connectionMode = usb?.connectionMode?.takeIf(String::isNotBlank)
        ?: if (isPc) "USB connection" else "OTG"

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        val inset = notchBottomInsetDp.coerceAtLeast(0).dp
        val availableHeight = (maxHeight - inset).coerceAtLeast(0.dp)
        val compact = availableHeight < 104.dp
        val showDetails = availableHeight >= 116.dp
        val showAllDetails = availableHeight >= 158.dp
        val iconSize = if (compact) 36.dp else 44.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(inset))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .padding(top = if (compact) 0.dp else 5.dp, bottom = 5.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                // A single connection row: the device itself is the connection.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Box(
                        modifier = Modifier.size(iconSize)
                            .clip(CircleShape)
                            .background(Color(0xFF263441)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPc) Icons.Filled.Computer else Icons.Filled.Usb,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = deviceName,
                            color = foreground,
                            fontSize = if (compact) 14.sp else 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = deviceType,
                            color = secondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape)
                                    .background(Color(0xFF69D994)),
                            )
                            Text(
                                "Connected",
                                color = Color(0xFF83D9A5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                        Text(
                            connectionMode,
                            color = Color(0xFFC8DDF5),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (showDetails) {
                    Spacer(Modifier.height(if (compact) 8.dp else 13.dp))
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Color.White.copy(alpha = 0.12f)),
                    )
                    Spacer(Modifier.height(9.dp))
                    if (isPc) {
                        UsbDetailRow("USB function", connectionMode)
                    } else {
                        usb?.manufacturer?.takeIf(String::isNotBlank)?.let {
                            UsbDetailRow("Manufacturer", it)
                        }
                        if (showAllDetails) {
                            usb?.vendorId?.let {
                                UsbDetailRow("Vendor ID", "0x%04X".format(it))
                            }
                            usb?.productId?.let {
                                UsbDetailRow("Product ID", "0x%04X".format(it))
                            }
                            usb?.deviceId?.let {
                                UsbDetailRow("Device ID", it.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsbDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 10.sp,
            maxLines = 1,
        )
        Text(
            value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
