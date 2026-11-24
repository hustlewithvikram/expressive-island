package com.vikram.expressiveisland.ui.screen.tiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.core.DynamicTile
import com.vikram.expressiveisland.core.IslandPreviewCategory
import com.vikram.expressiveisland.data.DownloadProgressStyle
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IslandPreviewSpacer()

        Text(
            text = "Download tile",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth().clickable { showIconPicker = true },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = selectedIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Download icon", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose the icon shown while a download is active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { viewModel.setDownloadIcon(null) }) {
                    Text("Reset")
                }
            }
        }

        SettingsToggleCard(
            shape = RoundedCornerShape(28.dp),
            title = "Show text",
            description = "Show the file name and supporting download information",
            checked = settings.showText,
            onCheckedChange = viewModel::setDownloadShowText,
        )
        SettingsToggleCard(
            shape = RoundedCornerShape(28.dp),
            title = "Show percentage",
            description = "Display the current completion percentage",
            checked = settings.showPercentage,
            onCheckedChange = viewModel::setDownloadShowPercentage,
        )
        SettingsToggleCard(
            shape = RoundedCornerShape(28.dp),
            title = "Show progress",
            description = "Display a progress indicator for the active download",
            checked = settings.showProgress,
            onCheckedChange = viewModel::setDownloadShowProgress,
        )

        if (settings.showProgress) {
            Text(
                text = "Progress style",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            )
            ExpressiveSegmentedRow(
                options = DownloadProgressStyle.entries.map { it.label },
                selectedIndex = DownloadProgressStyle.entries.indexOf(settings.progressStyle),
                onSelect = { viewModel.setDownloadProgressStyle(DownloadProgressStyle.entries[it]) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )

            Text(
                text = "Progress colours",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            )
            ColorPickerCard(
                label = "Progress",
                selected = settings.progressColor,
                onSelect = viewModel::setDownloadProgressColor,
                defaultLabel = "Theme primary",
                defaultColor = MaterialTheme.colorScheme.primary,
            )
            ColorPickerCard(
                label = "Track",
                selected = settings.trackColor,
                onSelect = viewModel::setDownloadTrackColor,
                defaultLabel = "Theme surface",
                defaultColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }

        Text(
            text = "Icon container",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
        )
        ColorPickerCard(
            label = "Icon background",
            selected = settings.iconContainerColor,
            onSelect = viewModel::setDownloadIconContainerColor,
            defaultLabel = "Download accent",
            defaultColor = Color(DynamicTile.DOWNLOAD.accent),
        )

        Text(
            text = "Downloads are detected from active notifications that publish real progress. The tile stays visible while the transfer is active and clears when its notification is removed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
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
