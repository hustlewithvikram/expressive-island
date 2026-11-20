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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    val deviceName = usb?.name?.takeIf(String::isNotBlank) ?: if (isPc) "Connected computer" else "USB device"
    val deviceType = usb?.deviceType?.takeIf(String::isNotBlank) ?: if (isPc) "Computer connection" else "External USB device"
    val connectionMode = usb?.connectionMode?.takeIf(String::isNotBlank) ?: if (isPc) "USB" else "OTG"

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        val contentHeight = (maxHeight - notchBottomInsetDp.coerceAtLeast(0).dp).coerceAtLeast(0.dp)
        val compact = contentHeight < 108.dp
        val veryCompact = contentHeight < 76.dp
        val horizontalInset = if (compact) 11.dp else 14.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(notchBottomInsetDp.coerceAtLeast(0).dp))
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .padding(top = if (compact) 1.dp else 5.dp, bottom = 7.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (compact) 20.dp else 23.dp),
                    color = Color(0xFF191B21),
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = horizontalInset,
                            vertical = if (veryCompact) 9.dp else 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (veryCompact) 7.dp else 10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(if (compact) 36.dp else 42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF263441)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isPc) Icons.Filled.Computer else Icons.Filled.Usb,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(if (compact) 20.dp else 23.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isPc) "USB connected to PC" else "USB connected",
                                    color = foreground,
                                    fontSize = if (compact) 13.sp else 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (isPc) "Computer connection detected" else "External device detected",
                                    color = secondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF69D994)))
                                Text("Connected", color = Color(0xFF83D9A5), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(if (compact) 40.dp else 44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF222832)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isPc) Icons.Filled.Computer else Icons.Filled.Usb,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(23.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    deviceName,
                                    color = foreground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    deviceType,
                                    color = secondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF292E38),
                            ) {
                                Text(
                                    connectionMode,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    color = Color(0xFFC8DDF5),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }

                        if (!veryCompact) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF111318),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (isPc) {
                                        UsbDetailRow("Connection", "USB")
                                        UsbDetailDivider()
                                        UsbDetailRow("USB function", usb?.connectionMode ?: "USB")
                                    } else {
                                        usb?.manufacturer?.takeIf(String::isNotBlank)?.let {
                                            UsbDetailRow("Manufacturer", it)
                                            UsbDetailDivider()
                                        }
                                        usb?.deviceId?.let {
                                            UsbDetailRow("Device ID", it.toString())
                                            UsbDetailDivider()
                                        }
                                        usb?.vendorId?.let {
                                            UsbDetailRow("Vendor ID", "0x%04X".format(it))
                                            UsbDetailDivider()
                                        }
                                        usb?.productId?.let {
                                            UsbDetailRow("Product ID", "0x%04X".format(it))
                                        }
                                        if (usb?.manufacturer == null && usb?.deviceId == null &&
                                            usb?.vendorId == null && usb?.productId == null
                                        ) {
                                            UsbDetailRow("Connection", "USB OTG")
                                        }
                                    }
                                }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, maxLines = 1)
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

@Composable
private fun UsbDetailDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
}
