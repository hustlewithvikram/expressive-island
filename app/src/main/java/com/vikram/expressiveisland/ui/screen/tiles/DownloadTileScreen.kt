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
        expanded = true,
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
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showIconPicker = true },
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
            ColorPickerCard(
                label = "Icon background",
                selected = settings.iconContainerColor,
                onSelect = viewModel::setDownloadIconContainerColor,
                defaultLabel = "Download accent",
                defaultColor = Color(DynamicTile.DOWNLOAD.accent),
            )
        }

        DownloadSettingsGroup(
            title = "Content",
            description = "Choose which download details appear in the expanded island",
        ) {
            SettingsToggleCard(
                shape = RoundedCornerShape(24.dp),
                title = "Show text",
                description = "Show the file name and supporting download information",
                checked = settings.showText,
                onCheckedChange = viewModel::setDownloadShowText,
            )
            SettingsToggleCard(
                shape = RoundedCornerShape(24.dp),
                title = "Show percentage in expanded tile",
                description = "Display the completion percentage beside the file name when the island is expanded",
                checked = settings.showPercentage,
                onCheckedChange = viewModel::setDownloadShowPercentage,
            )
        }

        DownloadSettingsGroup(
            title = "Progress indicator",
            description = "Choose how download progress is displayed",
        ) {
            SettingsToggleCard(
                shape = RoundedCornerShape(24.dp),
                title = "Show progress",
                description = "Display the selected progress indicator in the pill and expanded tile",
                checked = settings.showProgress,
                onCheckedChange = viewModel::setDownloadShowProgress,
            )
            if (settings.showProgress) {
                Text(
                    text = "Pill progress indicator",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
                ExpressiveSegmentedRow(
                    options = DownloadPillProgressStyle.entries.map { it.label },
                    selectedIndex = DownloadPillProgressStyle.entries.indexOf(settings.pillProgressStyle),
                    onSelect = { viewModel.setDownloadPillProgressStyle(DownloadPillProgressStyle.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                    allowFullLabels = true,
                )
                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = "Immersive progress",
                    description = "Use the entire expanded island as a progress surface. The classic bar is hidden while this is enabled.",
                    checked = settings.immersiveProgress,
                    onCheckedChange = viewModel::setDownloadImmersiveProgress,
                )
                if (!settings.immersiveProgress) {
                    Text(
                        text = "Progress style",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    )
                    ExpressiveSegmentedRow(
                        options = visibleProgressStyles.map { it.label },
                        selectedIndex = visibleProgressStyles.indexOf(settings.progressStyle)
                            .takeIf { it >= 0 } ?: visibleProgressStyles.indexOf(DownloadProgressStyle.LINEAR),
                        onSelect = { viewModel.setDownloadProgressStyle(visibleProgressStyles[it]) },
                        modifier = Modifier.fillMaxWidth(),
                        scrollable = true,
                        allowFullLabels = true,
                    )
                }
                Text(
                    text = if (settings.immersiveProgress) "Immersive progress colours" else "Progress colours",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )
                ColorPickerCard(
                    label = "Progress",
                    selected = settings.progressColor,
                    onSelect = viewModel::setDownloadProgressColor,
                    defaultLabel = "Theme primary",
                    defaultColor = MaterialTheme.colorScheme.primary,
                )
                if (!settings.immersiveProgress) {
                    ColorPickerCard(
                        label = "Track",
                        selected = settings.trackColor,
                        onSelect = viewModel::setDownloadTrackColor,
                        defaultLabel = "Theme surface",
                        defaultColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                }
            }
        }

        DownloadSettingsGroup(
            title = "Download actions",
            description = "Show notification-provided controls when the source app supports them",
        ) {
            SettingsToggleCard(
                shape = RoundedCornerShape(24.dp),
                title = "Pause / resume",
                description = "Show the available Pause or Resume action",
                checked = settings.showPauseResume,
                onCheckedChange = viewModel::setDownloadShowPauseResume,
            )
            SettingsToggleCard(
                shape = RoundedCornerShape(24.dp),
                title = "Cancel download",
                description = "Show the notification's Cancel action when available",
                checked = settings.showCancel,
                onCheckedChange = viewModel::setDownloadShowCancel,
            )
            Text(
                text = "These buttons use the download app's own notification actions. They appear only when that app exposes a matching action; Expressive Island cannot create pause or cancel controls for apps that do not provide them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun DownloadSettingsGroup(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}
