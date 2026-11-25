package com.vikram.expressiveisland.ui.screen.tiles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.core.DynamicTile
import com.vikram.expressiveisland.core.IslandPreviewCategory
import com.vikram.expressiveisland.data.DownloadProgressStyle
import com.vikram.expressiveisland.data.DownloadPillProgressStyle
import com.vikram.expressiveisland.data.IconSource
import com.vikram.expressiveisland.overlay.MaterialIconCatalog
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.components.ExpressiveSegmentedRow
import com.vikram.expressiveisland.ui.screen.ColorPickerCard
import com.vikram.expressiveisland.ui.screen.IslandLivePreviewEffect
import com.vikram.expressiveisland.ui.screen.IslandPreviewSpacer
import com.vikram.expressiveisland.ui.screen.MaterialIconPickerSheet
import com.vikram.expressiveisland.ui.screen.SettingsToggleCard

@Composable
internal fun DownloadTileScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.downloadTile.collectAsStateWithLifecycle()
    var showIconPicker by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(0) }
    var resetRotation by remember { mutableStateOf(0f) }
    val resetRotationAnimated by animateFloatAsState(resetRotation, label = "downloadIconResetRotation")
    val visibleProgressStyles = DownloadProgressStyle.entries.filterNot { it == DownloadProgressStyle.CIRCULAR }
    val selectedIcon = (settings.icon as? IconSource.Material)
        ?.let { MaterialIconCatalog.iconFor(it.iconName) }
        ?: DynamicTile.DOWNLOAD.defaultIcon

    IslandLivePreviewEffect(
        category = IslandPreviewCategory.DOWNLOAD_TILE,
        label = "Downloading update.zip",
        detail = "64% • 128 MB of 200 MB",
        expanded = selectedSection != 1,
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IslandPreviewSpacer()

        DownloadSettingsGroup(
            title = "Download icon",
            description = "Choose the icon used for active downloads",
        ) {
            SettingsToggleCard(
                shape = downloadGroupShape(0, if (settings.showAppIcon) 1 else 3),
                title = "Use app icon",
                description = "Use the downloading app's launcher icon. Custom icon and background options are hidden while enabled.",
                checked = settings.showAppIcon,
                onCheckedChange = viewModel::setDownloadShowAppIcon,
            )
            if (!settings.showAppIcon) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showIconPicker = true },
                    shape = downloadGroupShape(1, 3),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = selectedIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Choose icon", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Select a Material icon for the download tile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier.height(38.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    IconButton(
                        onClick = {
                            viewModel.setDownloadIcon(null)
                            resetRotation += 360f
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset download icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(resetRotationAnimated),
                        )
                    }
                }
                }
                ColorPickerCard(
                    label = "Icon background",
                    selected = settings.iconContainerColor,
                    onSelect = viewModel::setDownloadIconContainerColor,
                    defaultLabel = "Download accent",
                    defaultColor = Color(DynamicTile.DOWNLOAD.accent),
                    shape = downloadGroupShape(2, 3)
                )
            }
        }

        ExpressiveSegmentedRow(
            options = listOf("Expanded", "Pill", "Actions"),
            selectedIndex = selectedSection,
            onSelect = { selectedSection = it },
            modifier = Modifier.fillMaxWidth(),
            allowFullLabels = true,
        )

        if (selectedSection == 0) {
            DownloadSettingsGroup(
                title = "Expanded content",
                description = "These settings only affect the expanded island",
            ) {
                SettingsToggleCard(
                    shape = downloadGroupShape(0, 3 + if (settings.showProgress) 1 + (if (settings.immersiveProgress) 0 else 1) + 2 else 0),
                    title = "Show text",
                    description = "Show the file name and supporting download information",
                    checked = settings.showText,
                    onCheckedChange = viewModel::setDownloadShowText,
                )
                SettingsToggleCard(
                    shape = downloadGroupShape(1, 3 + if (settings.showProgress) 1 + (if (settings.immersiveProgress) 0 else 1) + 2 else 0),
                    title = "Show percentage",
                    description = "Display the completion percentage before the transfer amount",
                    checked = settings.showPercentage,
                    onCheckedChange = viewModel::setDownloadShowPercentage,
                )
                SettingsToggleCard(
                    shape = downloadGroupShape(2, 3 + if (settings.showProgress) 1 + (if (settings.immersiveProgress) 0 else 1) + 2 else 0),
                    title = "Show progress",
                    description = "Display a progress indicator in the expanded island",
                    checked = settings.showProgress,
                    onCheckedChange = viewModel::setDownloadShowProgress,
                )
                if (settings.showProgress) {
                    SettingsToggleCard(
                        shape = downloadGroupShape(3, 3 + if (settings.showProgress) 1 + (if (settings.immersiveProgress) 0 else 1) + 2 else 0),
                        title = "Immersive progress",
                        description = "Use the expanded island as a full progress surface",
                        checked = settings.immersiveProgress,
                        onCheckedChange = viewModel::setDownloadImmersiveProgress,
                    )
                    if (!settings.immersiveProgress) {
                        Text("Progress style", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                        ExpressiveSegmentedRow(
                            options = visibleProgressStyles.map { it.label },
                            selectedIndex = visibleProgressStyles.indexOf(settings.progressStyle)
                                .takeIf { it >= 0 } ?: 0,
                            onSelect = { viewModel.setDownloadProgressStyle(visibleProgressStyles[it]) },
                            modifier = Modifier.fillMaxWidth(),
                            scrollable = true,
                            allowFullLabels = true,
                        )
                    }
                    ColorPickerCard(
                        label = "Progress colour",
                        selected = settings.progressColor,
                        onSelect = viewModel::setDownloadProgressColor,
                        defaultLabel = "Theme primary",
                        defaultColor = MaterialTheme.colorScheme.primary,
                    shape = downloadGroupShape(if (settings.immersiveProgress) 4 else 5, 3 + if (settings.showProgress) 1 + (if (settings.immersiveProgress) 0 else 1) + 2 else 0)
                )
                    ColorPickerCard(
                        label = "Remaining track colour",
                        selected = settings.trackColor,
                        onSelect = viewModel::setDownloadTrackColor,
                        defaultLabel = "Theme surface",
                        defaultColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = downloadGroupShape(if (settings.immersiveProgress) 5 else 6, 3 + if (settings.showProgress) 1 + (if (settings.immersiveProgress) 0 else 1) + 2 else 0)
                )
                }
            }
        } else if (selectedSection == 1) {
            DownloadSettingsGroup(
                title = "Pill progress",
                description = "Control the compact indicator independently from expanded progress",
            ) {
                SettingsToggleCard(
                    shape = downloadGroupShape(0, if (settings.pillShowProgress) 2 + (if (settings.pillImmersiveProgress) 0 else 1) + 2 else 1),
                    title = "Show pill progress",
                    description = "Show progress beside the icon while the island is collapsed",
                    checked = settings.pillShowProgress,
                    onCheckedChange = viewModel::setDownloadPillShowProgress,
                )
                if (settings.pillShowProgress) {
                    SettingsToggleCard(
                        shape = downloadGroupShape(1, if (settings.pillShowProgress) 2 + (if (settings.pillImmersiveProgress) 0 else 1) + 2 else 1),
                        title = "Immersive progress",
                        description = "Fill the collapsed pill with the download progress",
                        checked = settings.pillImmersiveProgress,
                        onCheckedChange = viewModel::setDownloadPillImmersiveProgress,
                    )
                    if (!settings.pillImmersiveProgress) {
                    Text("Pill progress style", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    ExpressiveSegmentedRow(
                        options = DownloadPillProgressStyle.entries.map { it.label },
                        selectedIndex = DownloadPillProgressStyle.entries.indexOf(settings.pillProgressStyle),
                        onSelect = { viewModel.setDownloadPillProgressStyle(DownloadPillProgressStyle.entries[it]) },
                        modifier = Modifier.fillMaxWidth(),
                        scrollable = true,
                        allowFullLabels = true,
                    )
                    }
                    ColorPickerCard(
                        label = "Pill progress colour",
                        selected = settings.pillProgressColor,
                        onSelect = viewModel::setDownloadPillProgressColor,
                        defaultLabel = "Theme primary",
                        defaultColor = MaterialTheme.colorScheme.primary,
                    shape = downloadGroupShape(if (settings.pillImmersiveProgress) 2 else 3, if (settings.pillShowProgress) 2 + (if (settings.pillImmersiveProgress) 0 else 1) + 2 else 1)
                )
                    ColorPickerCard(
                        label = "Pill remaining track colour",
                        selected = settings.pillTrackColor,
                        onSelect = viewModel::setDownloadPillTrackColor,
                        defaultLabel = "Theme surface",
                        defaultColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = downloadGroupShape(if (settings.pillImmersiveProgress) 3 else 4, if (settings.pillShowProgress) 2 + (if (settings.pillImmersiveProgress) 0 else 1) + 2 else 1)
                )
                }
            }
        } else {
            DownloadSettingsGroup(
                title = "Action buttons",
                description = "Choose which notification controls appear and how they are tinted",
            ) {
                SettingsToggleCard(
                    shape = downloadGroupShape(0, 3),
                    title = "Pause / resume",
                    description = "Show the available Pause or Resume action",
                    checked = settings.showPauseResume,
                    onCheckedChange = viewModel::setDownloadShowPauseResume,
                )
                SettingsToggleCard(
                    shape = downloadGroupShape(1, 3),
                    title = "Cancel download",
                    description = "Show the notification's Cancel action when available",
                    checked = settings.showCancel,
                    onCheckedChange = viewModel::setDownloadShowCancel,
                )
                ColorPickerCard(
                    label = "Action button colour",
                    selected = settings.actionButtonColor,
                    onSelect = viewModel::setDownloadActionButtonColor,
                    defaultLabel = "Theme primary",
                    defaultColor = MaterialTheme.colorScheme.primary,
                    shape = downloadGroupShape(2, 3)
                )
                Text(
                    text = "Actions invoke the original notification PendingIntent. Pause/resume keeps the tile visible while the source app updates; cancel is destructive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "Downloads are detected from active notifications that publish real progress. The tile remains visible while a transfer is active and clears when its notification is removed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }

    if (showIconPicker) {
        MaterialIconPickerSheet(
            onPick = { name ->
                viewModel.setDownloadIcon(IconSource.Material(name))
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false },
        )
    }
}

@Composable
private fun DownloadSettingsGroup(title: String, description: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}
private fun downloadGroupShape(index: Int, count: Int): RoundedCornerShape = when {
    count <= 1 -> RoundedCornerShape(24.dp)
    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    index >= count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    else -> RoundedCornerShape(4.dp)
}
