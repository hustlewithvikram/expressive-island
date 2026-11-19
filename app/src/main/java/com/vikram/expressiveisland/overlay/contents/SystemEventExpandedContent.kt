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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import com.vikram.expressiveisland.core.SystemEventType

private val SystemEventNotchSafeSpace = 36.dp

@Composable
fun SystemEventExpandedContent(type: SystemEventType) {
    val colors = MaterialTheme.colorScheme
    val title = stringResource(type.labelRes)
    val (status, description, detailLabel, detailValue) = systemEventPresentation(type)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val viewportHeight = (maxHeight - SystemEventNotchSafeSpace).coerceAtLeast(0.dp)
        val compact = viewportHeight < 92.dp
        val veryCompact = viewportHeight < 68.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // The expanded island spans behind the camera cutout. Keep this exclusion zone
            // independent of the event content so no card or text can drift into the notch.
            Spacer(Modifier.height(SystemEventNotchSafeSpace))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(viewportHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = viewportHeight),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (veryCompact) 5.dp else 7.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(if (compact) 19.dp else 23.dp),
                        color = colors.surfaceContainerLow,
                        tonalElevation = 1.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = if (compact) 11.dp else 14.dp,
                                vertical = if (veryCompact) 7.dp else if (compact) 9.dp else 12.dp,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (veryCompact) 32.dp else if (compact) 38.dp else 44.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    type.defaultIcon,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier.size(if (veryCompact) 18.dp else 22.dp),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    title,
                                    color = colors.onSurface,
                                    fontSize = if (veryCompact) 12.sp else if (compact) 14.sp else 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        Modifier.size(6.dp).clip(CircleShape).background(colors.primary),
                                    )
                                    Text(
                                        status,
                                        color = colors.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                }
                                if (!compact) {
                                    Text(
                                        description,
                                        color = colors.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    if (!veryCompact) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = colors.secondaryContainer.copy(alpha = 0.58f),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(
                                    horizontal = 12.dp,
                                    vertical = if (compact) 8.dp else 10.dp,
                                ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    detailLabel,
                                    color = colors.onSecondaryContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                                Text(
                                    detailValue,
                                    color = colors.onSecondaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    } else {
                        Text(
                            description,
                            color = colors.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 3.dp),
                        )
                    }
                    }
                }
            }
        }
    }
}

private data class EventPresentation(
    val status: String,
    val description: String,
    val detailLabel: String,
    val detailValue: String,
)

private fun systemEventPresentation(type: SystemEventType): EventPresentation {
    val enabled = when (type) {
        SystemEventType.WIFI_CONNECTED,
SystemEventType.USB_MOUNTED, SystemEventType.DEVICE_UNLOCKED,
        SystemEventType.VPN_CONNECTED, SystemEventType.ADB_CONNECTED,
        SystemEventType.WIRELESS_DEBUGGING_CONNECTED, SystemEventType.BLUETOOTH_CONNECTED,
        SystemEventType.HOTSPOT_ENABLED, SystemEventType.AIRPLANE_MODE_ENABLED,
        SystemEventType.DND_ENABLED, SystemEventType.BATTERY_SAVER_ENABLED,
        SystemEventType.NFC_ENABLED, SystemEventType.LOCATION_ENABLED,
        SystemEventType.FLASHLIGHT_ENABLED, SystemEventType.DATA_SAVER_ENABLED,
        SystemEventType.AUTO_ROTATE_ENABLED, SystemEventType.THERMAL_WARNING,
        SystemEventType.RINGER_NORMAL, SystemEventType.RINGER_VIBRATE -> true
        else -> false
    }
    val status = when (type) {
        SystemEventType.THERMAL_WARNING -> "Temperature elevated"
        SystemEventType.THERMAL_NORMAL -> "Temperature normal"
        SystemEventType.BATTERY_LOW -> "Battery low"
        SystemEventType.CHARGING_COMPLETE -> "Fully charged"
        SystemEventType.CHARGING_STARTED -> "Charging"
        SystemEventType.CHARGING_STOPPED -> "Charging stopped"
        else -> if (enabled) "Active" else "Inactive"
    }
    val description = when (type) {
        SystemEventType.BLUETOOTH_CONNECTED -> "A Bluetooth device connected to this phone."
        SystemEventType.BLUETOOTH_DISCONNECTED -> "The Bluetooth connection was disconnected."
        SystemEventType.USB_MOUNTED -> "A USB device or accessory is connected."
        SystemEventType.USB_UNMOUNTED -> "The USB device was disconnected."
        SystemEventType.VPN_CONNECTED -> "Network traffic is routed through a VPN connection."
        SystemEventType.VPN_DISCONNECTED -> "The VPN connection is no longer active."
        SystemEventType.HOTSPOT_ENABLED -> "Your phone is sharing its mobile connection."
        SystemEventType.HOTSPOT_DISABLED -> "Personal hotspot sharing is turned off."
        SystemEventType.AIRPLANE_MODE_ENABLED -> "Wireless radios are restricted by airplane mode."
        SystemEventType.AIRPLANE_MODE_DISABLED -> "Airplane mode is turned off."
        SystemEventType.DND_ENABLED -> "Interruptions are being filtered by Do Not Disturb."
        SystemEventType.DND_DISABLED -> "Normal notification interruption rules are restored."
        SystemEventType.BATTERY_SAVER_ENABLED -> "Background activity and power use are being reduced."
        SystemEventType.BATTERY_SAVER_DISABLED -> "Standard battery and background behaviour is restored."
        SystemEventType.NFC_ENABLED -> "Near-field communication is available for supported actions."
        SystemEventType.NFC_DISABLED -> "Near-field communication is turned off."
        SystemEventType.LOCATION_ENABLED -> "Apps with permission can access device location."
        SystemEventType.LOCATION_DISABLED -> "Device location is turned off."
        SystemEventType.FLASHLIGHT_ENABLED -> "The rear camera flash is being used as a torch."
        SystemEventType.FLASHLIGHT_DISABLED -> "The torch is turned off."
        SystemEventType.DATA_SAVER_ENABLED -> "Background mobile data is restricted where supported."
        SystemEventType.DATA_SAVER_DISABLED -> "Background data restrictions are turned off."
        SystemEventType.AUTO_ROTATE_ENABLED -> "The screen can rotate with the device orientation."
        SystemEventType.AUTO_ROTATE_DISABLED -> "The screen orientation is locked."
        SystemEventType.THERMAL_WARNING -> "The device reports elevated thermal status."
        SystemEventType.THERMAL_NORMAL -> "The device thermal status has returned to normal."
        SystemEventType.RINGER_NORMAL -> "Incoming calls use the normal ringtone."
        SystemEventType.RINGER_VIBRATE -> "Incoming calls use vibration."
        SystemEventType.RINGER_SILENT -> "The ringer is muted."
        SystemEventType.ADB_CONNECTED -> "Android Debug Bridge is connected."
        SystemEventType.ADB_DISCONNECTED -> "Android Debug Bridge is disconnected."
        SystemEventType.WIRELESS_DEBUGGING_CONNECTED -> "Wireless debugging is connected."
        SystemEventType.WIRELESS_DEBUGGING_DISCONNECTED -> "Wireless debugging is disconnected."
        SystemEventType.DEVICE_LOCKED -> "The device is locked."
        SystemEventType.DEVICE_UNLOCKED -> "The device is unlocked."
        SystemEventType.CHARGING_STARTED -> "The battery is receiving power."
        SystemEventType.CHARGING_STOPPED -> "The device is no longer charging."
        SystemEventType.CHARGING_COMPLETE -> "The battery has reached full charge."
        SystemEventType.BATTERY_LOW -> "The battery level has reached the low threshold."
        SystemEventType.WIFI_CONNECTED -> "The phone is connected to a Wi-Fi network."
        SystemEventType.WIFI_DISCONNECTED -> "The Wi-Fi connection has ended."
    }
    val detailLabel = when (type) {
        SystemEventType.BLUETOOTH_CONNECTED, SystemEventType.BLUETOOTH_DISCONNECTED -> "Connection"
        SystemEventType.THERMAL_WARNING, SystemEventType.THERMAL_NORMAL -> "Thermal state"
        SystemEventType.RINGER_NORMAL, SystemEventType.RINGER_VIBRATE, SystemEventType.RINGER_SILENT -> "Sound profile"
        else -> "Current state"
    }
    val detailValue = when (type) {
        SystemEventType.RINGER_NORMAL -> "Ring"
        SystemEventType.RINGER_VIBRATE -> "Vibrate"
        SystemEventType.RINGER_SILENT -> "Silent"
        SystemEventType.THERMAL_WARNING -> "Elevated"
        SystemEventType.THERMAL_NORMAL -> "Normal"
        else -> status
    }
    return EventPresentation(status, description, detailLabel, detailValue)
}
