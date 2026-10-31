package com.vikram.expressiveisland.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Square
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.permissions.Permissions
import com.vikram.expressiveisland.system.ShizukuState
import com.vikram.expressiveisland.system.ShizukuStatus
import com.vikram.expressiveisland.ui.AppViewModel

private fun groupedShape(
    isFirst: Boolean,
    isLast: Boolean,
) = RoundedCornerShape(
    topStart = if (isFirst) 24.dp else 6.dp,
    topEnd = if (isFirst) 24.dp else 6.dp,
    bottomStart = if (isLast) 24.dp else 6.dp,
    bottomEnd = if (isLast) 24.dp else 6.dp,
)

@Composable
internal fun ShizukuScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenPermissionDot: () -> Unit,
) {
    val context = LocalContext.current

    val hideIcons by viewModel.hideNotificationIcons.collectAsStateWithLifecycle()
    val hideSystemInfo by viewModel.hideSystemInfo.collectAsStateWithLifecycle()
    val hideClock by viewModel.hideClock.collectAsStateWithLifecycle()
    val silenceAlerts by viewModel.silenceSystemAlerts.collectAsStateWithLifecycle()
    val permissionDot by viewModel.permissionDotEnabled.collectAsStateWithLifecycle()
    val shizuku by ShizukuState.status.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ShizukuState.refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val ready = shizuku == ShizukuStatus.READY

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Shizuku status
        AnimatedVisibility(
            visible = !ready,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        ) {
            ShizukuCard(
                status = shizuku,
                onClick = {
                    if (shizuku == ShizukuStatus.PERMISSION_REQUIRED) {
                        ShizukuState.requestPermission()
                    } else {
                        Permissions.openShizuku(context)
                    }
                },
            )
        }

        // Status bar
        CardSectionHeader(
            text = "Status bar",
            padding = PaddingValues(
                start = 4.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        )

        StatusBarPreview(
            hideIcons = hideIcons,
            hideSystem = hideSystemInfo,
            hideClock = hideClock,
        )

        Text(
            text = stringResource(R.string.status_bar_hide_icons_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsToggleCard(
                shape = groupedShape(
                    isFirst = true,
                    isLast = false,
                ),
                title = stringResource(R.string.status_bar_hide_icons_title),
                description = stringResource(R.string.status_bar_hide_icons_desc),
                checked = ready && hideIcons,
                onCheckedChange = viewModel::setHideNotificationIcons,
                enabled = ready,
            )

            SettingsToggleCard(
                shape = groupedShape(
                    isFirst = false,
                    isLast = false,
                ),
                title = stringResource(R.string.status_bar_hide_system_info_title),
                description = stringResource(R.string.status_bar_hide_system_info_desc),
                checked = ready && hideSystemInfo,
                onCheckedChange = viewModel::setHideSystemInfo,
                enabled = ready,
            )

            SettingsToggleCard(
                shape = groupedShape(
                    isFirst = false,
                    isLast = true,
                ),
                title = stringResource(R.string.status_bar_hide_clock_title),
                description = stringResource(R.string.status_bar_hide_clock_desc),
                checked = ready && hideClock,
                onCheckedChange = viewModel::setHideClock,
                enabled = ready,
            )
        }

        // System alerts
        CardSectionHeader(
            text = "System alerts",
            padding = PaddingValues(
                start = 4.dp,
                top = 8.dp,
                bottom = 4.dp,
            ),
        )

        SettingsToggleCard(
            shape = RoundedCornerShape(28.dp),
            title = stringResource(R.string.status_bar_silence_alerts_title),
            description = stringResource(R.string.status_bar_silence_alerts_desc),
            checked = ready && silenceAlerts,
            onCheckedChange = viewModel::setSilenceSystemAlerts,
            enabled = ready,
        )

        // Permissions
        CardSectionHeader(
            text = "Permissions",
            padding = PaddingValues(
                start = 4.dp,
                top = 8.dp,
                bottom = 4.dp,
            ),
        )

        SettingsToggleNavCard(
            shape = RoundedCornerShape(28.dp),
            title = stringResource(R.string.permission_dot_title),
            description = stringResource(R.string.permission_dot_desc),
            checked = ready && permissionDot,
            onCheckedChange = viewModel::setPermissionDotEnabled,
            onClick = onOpenPermissionDot,
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Mock status bar showing what the selected hiding options look like.
 */
@Composable
private fun StatusBarPreview(
    hideIcons: Boolean,
    hideSystem: Boolean,
    hideClock: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 13.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                // Clock
                AnimatedVisibility(
                    visible = !hideClock,
                ) {
                    Text(
                        text = stringResource(
                            R.string.statusbar_preview_time
                        ),
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Notification icons
                AnimatedVisibility(
                    visible = !hideIcons,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Square,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(45f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "Notification icon",
                        )

                        Icon(
                            imageVector = Icons.Rounded.Square,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "Notification icon",
                        )

                        Icon(
                            imageVector = Icons.Rounded.Circle,
                            modifier = Modifier
                                .size(14.dp)
                                .rotate(45f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "Notification icon",
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                // System icons
                AnimatedVisibility(
                    visible = !hideSystem,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NetworkWifi,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "System icon",
                        )

                        Icon(
                            imageVector = Icons.Rounded.NetworkCell,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "System icon",
                        )

                        Icon(
                            imageVector = Icons.Rounded.BatterySaver,
                            modifier = Modifier
                                .size(17.dp)
                                .rotate(90f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "System icon",
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dynamic Shizuku status card.
 */
@Composable
private fun ShizukuCard(
    status: ShizukuStatus,
    onClick: () -> Unit,
) {
    val title = when (status) {
        ShizukuStatus.NOT_INSTALLED ->
            R.string.shizuku_not_installed_title

        ShizukuStatus.NOT_RUNNING ->
            R.string.shizuku_not_running_title

        else ->
            R.string.shizuku_permission_title
    }

    val subtitle = when (status) {
        ShizukuStatus.NOT_INSTALLED ->
            R.string.shizuku_not_installed_desc

        ShizukuStatus.NOT_RUNNING ->
            R.string.shizuku_not_running_desc

        else ->
            R.string.shizuku_permission_desc
    }

    val recoverable =
        status == ShizukuStatus.NOT_RUNNING ||
                status == ShizukuStatus.PERMISSION_REQUIRED

    SettingsListItem(
        icon = Icons.Rounded.ErrorOutline,
        title = stringResource(title),
        subtitle = stringResource(subtitle),
        onClick = onClick,
        bgColor = if (recoverable) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        fgColor = if (recoverable) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
    )
}