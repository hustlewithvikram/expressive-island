package com.vikram.expressiveisland.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import androidx.compose.ui.res.stringResource
import com.vikram.expressiveisland.permissions.Permissions
import com.vikram.expressiveisland.system.ShizukuState
import com.vikram.expressiveisland.system.ShizukuStatus
import kotlinx.coroutines.launch

/**
 * First-run access guide. Required grants are always presented first; optional access and
 * Shizuku's one-tap helper follow them. The sheet remains non-dismissible until both required
 * grants are enabled, then it leaves composition automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequiredPermissionsBottomSheet() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableStateOf(0) }
    val status = remember(refreshKey) {
        PermissionSheetStatus(
            notifications = Permissions.isNotificationAccessGranted(context),
            accessibility = Permissions.isAccessibilityGranted(context),
            batteryIgnored = Permissions.isBatteryOptimizationIgnored(context),
            wifiSsidAccess = Permissions.isWifiSsidAccessGranted(context),
        )
    }
    val shizukuStatus by ShizukuState.status.collectAsStateWithLifecycle()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
                ShizukuState.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var grantingWithShizuku by remember { mutableStateOf(false) }
    var shizukuMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val wifiPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshKey++ }

    if (status.allRequiredGranted) return

    val requestWifiPermissions = {
        val requested = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        if (requested.isNotEmpty()) wifiPermissionLauncher.launch(requested.toTypedArray())
    }

    ModalBottomSheet(
        onDismissRequest = {
            // Required access is a prerequisite for the island, so the setup sheet cannot
            // be swiped away or dismissed by tapping outside before both grants are enabled.
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Set up Expressive Island",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Enable the two required permissions to let Expressive Island display notifications and appear over other apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Required access",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            )
            SetupPermissionCard(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.perm_notifications_title),
                granted = status.notifications,
                onClick = { Permissions.openNotificationAccessSettings(context) },
            )
            SetupPermissionCard(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.perm_accessibility_title),
                granted = status.accessibility,
                onClick = { Permissions.openAccessibilitySettings(context) },
            )

            Text(
                text = "Optional access",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp),
            )
            SetupShizukuCard(
                status = shizukuStatus,
                granting = grantingWithShizuku,
                message = shizukuMessage,
                onAction = {
                    when (shizukuStatus) {
                        ShizukuStatus.READY -> if (!grantingWithShizuku) {
                            scope.launch {
                                grantingWithShizuku = true
                                shizukuMessage = null
                                val granted = Permissions.grantRequiredWithShizuku(context)
                                refreshKey++
                                grantingWithShizuku = false
                                shizukuMessage = if (granted) {
                                    "Permission status refreshed. Enable any remaining required access above."
                                } else {
                                    "Could not grant both permissions. Check Shizuku authorization and try again."
                                }
                            }
                        }
                        ShizukuStatus.PERMISSION_REQUIRED -> ShizukuState.requestPermission()
                        ShizukuStatus.NOT_INSTALLED,
                        ShizukuStatus.NOT_RUNNING -> Permissions.openShizuku(context)
                    }
                },
            )
            SetupPermissionCard(
                icon = Icons.Rounded.BatterySaver,
                title = "Ignore battery optimizations",
                granted = status.batteryIgnored,
                onClick = { Permissions.requestIgnoreBatteryOptimization(context) },
            )
            SetupPermissionCard(
                icon = Icons.Rounded.Wifi,
                title = "Wi-Fi network name access",
                granted = status.wifiSsidAccess,
                onClick = requestWifiPermissions,
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

private data class PermissionSheetStatus(
    val notifications: Boolean,
    val accessibility: Boolean,
    val batteryIgnored: Boolean,
    val wifiSsidAccess: Boolean,
) {
    val allRequiredGranted: Boolean get() = notifications && accessibility
}

@Composable
private fun SetupPermissionCard(
    icon: ImageVector,
    title: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (!granted) onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(25.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
            )
            if (granted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Enabled",
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(onClick = onClick) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
private fun SetupShizukuCard(
    status: ShizukuStatus,
    granting: Boolean,
    message: String?,
    onAction: () -> Unit,
) {
    val title = when (status) {
        ShizukuStatus.NOT_INSTALLED -> "Use Shizuku"
        ShizukuStatus.NOT_RUNNING -> "Start Shizuku"
        ShizukuStatus.PERMISSION_REQUIRED -> "Allow Shizuku access"
        ShizukuStatus.READY -> "Grant required permissions with Shizuku"
    }
    val actionLabel = when {
        granting -> "Granting…"
        status == ShizukuStatus.READY -> "Grant permissions"
        status == ShizukuStatus.PERMISSION_REQUIRED -> "Authorize"
        status == ShizukuStatus.NOT_RUNNING -> "Open Shizuku"
        else -> "Get Shizuku"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = "Optional shortcut: use Shizuku to enable both required permissions in one action.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Button(
                onClick = onAction,
                enabled = !granting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (granting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(actionLabel)
            }
        }
    }
}
