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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
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

/**
 * Shared, transparent system-event layout. The island owns the background; event content only
 * presents the event's primary state and the most useful information available for that event.
 */
@Composable
fun SystemEventExpandedContent(
    type: SystemEventType,
    deviceName: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val title = stringResource(type.labelRes)
    val presentation = systemEventPresentation(type, deviceName)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        val safeInset = SystemEventNotchSafeSpace.coerceAtMost(maxHeight)
        val contentHeight = (maxHeight - safeInset).coerceAtLeast(0.dp)
        val compact = contentHeight < 116.dp
        val veryCompact = contentHeight < 78.dp

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(safeInset))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = contentHeight)
                    .height(contentHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(if (veryCompact) 6.dp else 9.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (veryCompact) 34.dp else if (compact) 40.dp else 46.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                type.defaultIcon,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(if (veryCompact) 19.dp else 23.dp),
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                title,
                                color = colors.onSurface,
                                fontSize = if (veryCompact) 13.sp else if (compact) 15.sp else 17.sp,
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
                                    presentation.status,
                                    color = colors.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (!veryCompact) {
                        HorizontalDivider(
                            color = colors.outlineVariant.copy(alpha = 0.55f),
                            thickness = 0.7.dp,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                presentation.detailLabel,
                                color = colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                            Text(
                                presentation.detailValue,
                                color = colors.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }

                    if (!compact) {
                        Text(
                            presentation.description,
                            color = colors.onSurfaceVariant,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
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

private fun systemEventPresentation(
    type: SystemEventType,
    deviceName: String?,
): EventPresentation {
    val enabled = when (type) {
        SystemEventType.DEVICE_UNLOCKED,
        SystemEventType.VPN_CONNECTED,
        SystemEventType.ADB_CONNECTED,
        SystemEventType.WIRELESS_DEBUGGING_CONNECTED,
        SystemEventType.BLUETOOTH_CONNECTED,
        SystemEventType.HOTSPOT_ENABLED,
        SystemEventType.AIRPLANE_MODE_ENABLED,
        SystemEventType.DND_ENABLED,
        SystemEventType.BATTERY_SAVER_ENABLED,
        SystemEventType.NFC_ENABLED,
        SystemEventType.LOCATION_ENABLED,
        SystemEventType.FLASHLIGHT_ENABLED,
        SystemEventType.DATA_SAVER_ENABLED,
        SystemEventType.AUTO_ROTATE_ENABLED,
        SystemEventType.THERMAL_WARNING,
        SystemEventType.RINGER_NORMAL,
        SystemEventType.RINGER_VIBRATE -> true
        else -> false
    }

    val status = when (type) {
        SystemEventType.THERMAL_WARNING -> "Temperature elevated"
        SystemEventType.THERMAL_NORMAL -> "Temperature normal"
        SystemEventType.RINGER_NORMAL -> "Ring mode"
        SystemEventType.RINGER_VIBRATE -> "Vibrate mode"
        SystemEventType.RINGER_SILENT -> "Silent mode"
        SystemEventType.DEVICE_LOCKED -> "Device locked"
        SystemEventType.DEVICE_UNLOCKED -> "Device unlocked"
        SystemEventType.BLUETOOTH_CONNECTED -> "Connected"
        SystemEventType.BLUETOOTH_DISCONNECTED -> "Disconnected"
        else -> if (enabled) "Active" else "Inactive"
    }

    val description = when (type) {
        SystemEventType.DEVICE_LOCKED -> "Your phone is now secured. Notifications and protected content follow your lock-screen settings."
        SystemEventType.DEVICE_UNLOCKED -> "Your phone is unlocked and ready to use."
        SystemEventType.VPN_CONNECTED -> "Your active network is using a VPN tunnel."
        SystemEventType.VPN_DISCONNECTED -> "No active VPN tunnel was detected. Your network is using its normal route."
        SystemEventType.ADB_CONNECTED -> "Android Debug Bridge is available over a USB debugging connection."
        SystemEventType.ADB_DISCONNECTED -> "The USB debugging connection is no longer active."
        SystemEventType.WIRELESS_DEBUGGING_CONNECTED -> "Wireless debugging is active and can accept authorized debugging connections."
        SystemEventType.WIRELESS_DEBUGGING_DISCONNECTED -> "Wireless debugging is no longer active."
        SystemEventType.BLUETOOTH_CONNECTED -> "A Bluetooth accessory is connected to your phone."
        SystemEventType.BLUETOOTH_DISCONNECTED -> "The Bluetooth accessory connection has ended."
        SystemEventType.HOTSPOT_ENABLED -> "Your phone is sharing its mobile connection with nearby devices."
        SystemEventType.HOTSPOT_DISABLED -> "Your phone is no longer sharing its mobile connection."
        SystemEventType.AIRPLANE_MODE_ENABLED -> "Wireless connectivity is restricted by airplane mode."
        SystemEventType.AIRPLANE_MODE_DISABLED -> "Airplane mode is off and wireless radios can operate normally."
        SystemEventType.DND_ENABLED -> "Calls, alerts and notifications are being filtered by Do Not Disturb."
        SystemEventType.DND_DISABLED -> "Do Not Disturb is off. Your normal interruption settings apply."
        SystemEventType.BATTERY_SAVER_ENABLED -> "Background activity and selected visual effects are restricted to reduce power use."
        SystemEventType.BATTERY_SAVER_DISABLED -> "Standard background activity and power settings are restored."
        SystemEventType.NFC_ENABLED -> "Near-field communication is ready for supported taps and payments."
        SystemEventType.NFC_DISABLED -> "Near-field communication is turned off."
        SystemEventType.LOCATION_ENABLED -> "Apps with location permission can access the device's location."
        SystemEventType.LOCATION_DISABLED -> "Device location is turned off for apps and system services."
        SystemEventType.FLASHLIGHT_ENABLED -> "The rear camera flash is currently being used as a torch."
        SystemEventType.FLASHLIGHT_DISABLED -> "The camera torch is turned off."
        SystemEventType.DATA_SAVER_ENABLED -> "Background mobile data is restricted for apps where supported."
        SystemEventType.DATA_SAVER_DISABLED -> "Data Saver restrictions are turned off."
        SystemEventType.AUTO_ROTATE_ENABLED -> "The screen can rotate when you change the device orientation."
        SystemEventType.AUTO_ROTATE_DISABLED -> "The screen orientation is currently locked."
        SystemEventType.THERMAL_WARNING -> "Android reports elevated device temperature. Performance may be reduced to protect the hardware."
        SystemEventType.THERMAL_NORMAL -> "Android reports that device temperature has returned to a normal range."
        SystemEventType.RINGER_NORMAL -> "Incoming calls and alerts can play their configured sounds."
        SystemEventType.RINGER_VIBRATE -> "Incoming calls and alerts use vibration instead of a ringtone."
        SystemEventType.RINGER_SILENT -> "The ringer is muted."
        else -> "The system setting has changed."
    }

    val detailLabel = when (type) {
        SystemEventType.DEVICE_LOCKED, SystemEventType.DEVICE_UNLOCKED -> "Security"
        SystemEventType.VPN_CONNECTED, SystemEventType.VPN_DISCONNECTED -> "Network route"
        SystemEventType.ADB_CONNECTED, SystemEventType.ADB_DISCONNECTED -> "Debug bridge"
        SystemEventType.WIRELESS_DEBUGGING_CONNECTED, SystemEventType.WIRELESS_DEBUGGING_DISCONNECTED -> "Connection"
        SystemEventType.BLUETOOTH_CONNECTED, SystemEventType.BLUETOOTH_DISCONNECTED -> "Device"
        SystemEventType.HOTSPOT_ENABLED, SystemEventType.HOTSPOT_DISABLED -> "Sharing"
        SystemEventType.AIRPLANE_MODE_ENABLED, SystemEventType.AIRPLANE_MODE_DISABLED -> "Wireless radios"
        SystemEventType.DND_ENABLED, SystemEventType.DND_DISABLED -> "Interruption filter"
        SystemEventType.BATTERY_SAVER_ENABLED, SystemEventType.BATTERY_SAVER_DISABLED -> "Power profile"
        SystemEventType.NFC_ENABLED, SystemEventType.NFC_DISABLED -> "Contactless"
        SystemEventType.LOCATION_ENABLED, SystemEventType.LOCATION_DISABLED -> "Location access"
        SystemEventType.FLASHLIGHT_ENABLED, SystemEventType.FLASHLIGHT_DISABLED -> "Torch"
        SystemEventType.DATA_SAVER_ENABLED, SystemEventType.DATA_SAVER_DISABLED -> "Background data"
        SystemEventType.AUTO_ROTATE_ENABLED, SystemEventType.AUTO_ROTATE_DISABLED -> "Orientation"
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
        SystemEventType.DEVICE_LOCKED -> "Secured"
        SystemEventType.DEVICE_UNLOCKED -> "Unlocked"
        SystemEventType.VPN_CONNECTED -> "VPN tunnel"
        SystemEventType.VPN_DISCONNECTED -> "Direct network"
        SystemEventType.ADB_CONNECTED -> "USB debugging"
        SystemEventType.ADB_DISCONNECTED -> "Not connected"
        SystemEventType.WIRELESS_DEBUGGING_CONNECTED -> "Wireless"
        SystemEventType.WIRELESS_DEBUGGING_DISCONNECTED -> "Off"
        SystemEventType.BLUETOOTH_CONNECTED -> deviceName?.takeIf { it.isNotBlank() } ?: "Bluetooth accessory"
        SystemEventType.BLUETOOTH_DISCONNECTED -> deviceName?.takeIf { it.isNotBlank() } ?: "Not connected"
        SystemEventType.HOTSPOT_ENABLED -> "Sharing on"
        SystemEventType.HOTSPOT_DISABLED -> "Off"
        SystemEventType.AIRPLANE_MODE_ENABLED -> "Restricted"
        SystemEventType.AIRPLANE_MODE_DISABLED -> "Available"
        SystemEventType.DND_ENABLED -> "Filtering"
        SystemEventType.DND_DISABLED -> "Allowing alerts"
        SystemEventType.BATTERY_SAVER_ENABLED -> "Power saving"
        SystemEventType.BATTERY_SAVER_DISABLED -> "Standard"
        SystemEventType.NFC_ENABLED -> "Ready"
        SystemEventType.NFC_DISABLED -> "Off"
        SystemEventType.LOCATION_ENABLED -> "Available"
        SystemEventType.LOCATION_DISABLED -> "Off"
        SystemEventType.FLASHLIGHT_ENABLED -> "On"
        SystemEventType.FLASHLIGHT_DISABLED -> "Off"
        SystemEventType.DATA_SAVER_ENABLED -> "Restricted"
        SystemEventType.DATA_SAVER_DISABLED -> "Unrestricted"
        SystemEventType.AUTO_ROTATE_ENABLED -> "Auto rotate"
        SystemEventType.AUTO_ROTATE_DISABLED -> "Locked"
        else -> status
    }

    return EventPresentation(status, description, detailLabel, detailValue)
}
