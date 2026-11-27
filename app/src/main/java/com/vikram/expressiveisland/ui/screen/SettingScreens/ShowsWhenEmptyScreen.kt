package com.vikram.expressiveisland.ui.screen

import com.vikram.expressiveisland.core.IslandPreviewCategory
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.CenterShortcut
import com.vikram.expressiveisland.data.CutoutColor
import com.vikram.expressiveisland.data.EmptyClickAction
import com.vikram.expressiveisland.data.IconSource
import com.vikram.expressiveisland.data.PillWidgetType
import com.vikram.expressiveisland.data.NetworkSpeedFormat
import com.vikram.expressiveisland.data.PillBatteryStyle
import com.vikram.expressiveisland.data.PillClockFormat
import com.vikram.expressiveisland.overlay.CenterShortcutCatalog
import com.vikram.expressiveisland.overlay.MaterialIconCatalog
import com.vikram.expressiveisland.overlay.loadImageBitmapOrNull
import com.vikram.expressiveisland.overlay.resolve
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.components.ExpressiveSegmentedRow
import com.vikram.expressiveisland.ui.screen.AppIcon
import com.vikram.expressiveisland.ui.screen.IconChooserSheet
import com.vikram.expressiveisland.ui.screen.InstalledApp
import com.vikram.expressiveisland.ui.screen.MaterialIconPickerSheet
import com.vikram.expressiveisland.ui.screen.SettingsToggleCard
import com.vikram.expressiveisland.ui.screen.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.filter

private fun persistentPillGroupShape(isFirst: Boolean, isLast: Boolean) = RoundedCornerShape(
    topStart = if (isFirst) 32.dp else 4.dp,
    topEnd = if (isFirst) 32.dp else 4.dp,
    bottomStart = if (isLast) 32.dp else 4.dp,
    bottomEnd = if (isLast) 32.dp else 4.dp,
)

/**
 * The empty pill's detail screen, reached from the Behaviour list. Lets the user give the resting
 * (event-less) pill a glyph: a "Show icon" toggle, and — while it's on — an icon picker (a custom
 * image or a built-in Material icon) plus a container-colour picker for the disc behind it.
 */
@Composable
internal fun ShowsWhenEmptyScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    IslandLivePreviewEffect(
        category = IslandPreviewCategory.EMPTY_STATE,
        label = "Empty island",
        detail = "Preview the island when no notification is active",
        expanded = false,
    )

    val context = LocalContext.current
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    var selectedSideLeft by remember { mutableStateOf(true) }
    val selectedWidget = if (selectedSideLeft) behaviour.persistentPillLeftWidget else behaviour.persistentPillRightWidget
    val showIcon = selectedWidget == PillWidgetType.ICON || selectedWidget == PillWidgetType.ANIMATED_ICON
    val source = behaviour.showsWhenEmptyIcon
    val containerColor = behaviour.showsWhenEmptyIconColor
    val clickAction = behaviour.showsWhenEmptyClickAction
    val clickPackage = behaviour.showsWhenEmptyClickPackage
    val holdAction = behaviour.persistentPillHoldAction
    val holdPackage = behaviour.persistentPillHoldPackage
    val doubleTapAction = behaviour.persistentPillDoubleTapAction
    val doubleTapPackage = behaviour.persistentPillDoubleTapPackage

    var showIconSheet by remember { mutableStateOf(false) }
    var showMaterialPicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showHoldAppPicker by remember { mutableStateOf(false) }
    var showDoubleTapAppPicker by remember { mutableStateOf(false) }
    var showAddShortcut by remember { mutableStateOf(false) }
    var showCenterAppPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setShowsWhenEmptyImageIcon(uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PersistentPillSectionTitle(stringResource(R.string.persistent_pill_appearance))
        ExpressiveSegmentedRow(
            options = listOf("Left side", "Right side"),
            selectedIndex = if (selectedSideLeft) 0 else 1,
            onSelect = { selectedSideLeft = it == 0 },
            modifier = Modifier.fillMaxWidth(),
        )
        PillWidgetSlotCard(
            title = if (selectedSideLeft) "Left side" else "Right side",
            selected = selectedWidget,
            shape = RoundedCornerShape(24.dp),
            onSelect = { viewModel.setPersistentPillWidget(selectedSideLeft, it) },
        )
        val widgetPosition = if (selectedSideLeft) behaviour.persistentPillLeftPosition else behaviour.persistentPillRightPosition
        AnimatedVisibility(visible = selectedWidget == PillWidgetType.NETWORK_SPEED || selectedWidget == PillWidgetType.BATTERY || selectedWidget == PillWidgetType.CLOCK || selectedWidget == PillWidgetType.DATE || selectedWidget == PillWidgetType.WEEKDAY) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Horizontal position", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = widgetPosition,
                        onValueChange = { viewModel.setPersistentPillPosition(selectedSideLeft, it) },
                        valueRange = 0f..1f,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Center", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("End", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selectedWidget == PillWidgetType.BATTERY) {
                        Text("Battery style", style = MaterialTheme.typography.titleSmall)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PillBatteryStyle.entries.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { style ->
                                        FilterChip(
                                            selected = behaviour.persistentPillBatteryStyle == style,
                                            onClick = { viewModel.setPersistentPillBatteryStyle(style) },
                                            label = { Text(when (style) {
                                                PillBatteryStyle.PERCENTAGE -> "Percentage"
                                                PillBatteryStyle.ICON_WITH_PERCENTAGE -> "Icon + %"
                                                PillBatteryStyle.VERTICAL -> "Vertical"
                                                PillBatteryStyle.HORIZONTAL_FILL -> "Horizontal fill"
                                            }) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (selectedWidget == PillWidgetType.CLOCK) {
                        Text("Clock format", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillClockFormat.entries.forEach { format ->
                                FilterChip(
                                    selected = behaviour.persistentPillClockFormat == format,
                                    onClick = { viewModel.setPersistentPillClockFormat(format) },
                                    label = { Text(if (format == PillClockFormat.HOUR_12) "12-hour" else "24-hour") },
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showIcon) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsToggleCard(
                    shape = RoundedCornerShape(24.dp),
                    title = "Animate icon",
                    description = "Add a subtle pulse animation to this side's icon",
                    checked = selectedWidget == PillWidgetType.ANIMATED_ICON,
                    onCheckedChange = { enabled ->
                        viewModel.setPersistentPillWidget(
                            selectedSideLeft,
                            if (enabled) PillWidgetType.ANIMATED_ICON else PillWidgetType.ICON,
                        )
                    },
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        EmptyIconThumbnail(source = source, containerColor = containerColor, size = 64.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Pill icon", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Use a custom image or a Material icon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(onClick = { showIconSheet = true }, shape = RoundedCornerShape(18.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Text(stringResource(R.string.event_change_icon), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                ColorPickerCard(
                    label = stringResource(R.string.shows_when_empty_container_color),
                    selected = containerColor,
                    onSelect = { viewModel.setShowsWhenEmptyIconColor(it) },
                    defaultLabel = stringResource(R.string.label_default),
                    defaultColor = Color.White,
                )
            }
        }

        AnimatedVisibility(visible = selectedWidget == PillWidgetType.NETWORK_SPEED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Internet speed format", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Choose how the network speed is displayed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NetworkSpeedFormat.entries.forEach { format ->
                            FilterChip(
                                selected = behaviour.networkSpeedFormat == format,
                                onClick = { viewModel.setNetworkSpeedFormat(format) },
                                label = {
                                    Text(
                                        when (format) {
                                            NetworkSpeedFormat.AUTO -> "Auto"
                                            NetworkSpeedFormat.KBPS -> "KB/s"
                                            NetworkSpeedFormat.MBPS -> "MB/s"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        SettingsToggleCard(
            shape = RoundedCornerShape(24.dp),
            title = "Invisible Persistent Pill",
            description = "Hide the pill visually while keeping its touch area and actions active.",
            checked = !behaviour.showsWhenEmpty,
            onCheckedChange = { invisible -> viewModel.setShowsWhenEmpty(!invisible) },
        )

        PersistentPillSectionTitle(stringResource(R.string.persistent_pill_interaction))
        val quickActionsContent: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PersistentPillSectionTitle(stringResource(R.string.persistent_pill_shortcuts))
                SettingsToggleCard(
                    shape = persistentPillGroupShape(isFirst = true, isLast = false),
                    title = stringResource(R.string.shows_when_empty_center_labels),
                    description = stringResource(R.string.shows_when_empty_center_labels_desc),
                    checked = behaviour.centerShowLabels,
                    onCheckedChange = viewModel::setCenterShowLabels,
                )
                SettingsToggleCard(
                    shape = persistentPillGroupShape(isFirst = false, isLast = false),
                    title = stringResource(R.string.shows_when_empty_center_fill),
                    description = stringResource(R.string.shows_when_empty_center_fill_desc),
                    checked = behaviour.centerFillContainers,
                    onCheckedChange = viewModel::setCenterFillContainers,
                )
                if (behaviour.centerShortcuts.any { it is CenterShortcut.LaunchApp }) {
                    SettingsToggleCard(
                        shape = persistentPillGroupShape(isFirst = false, isLast = false),
                        title = stringResource(R.string.shows_when_empty_center_themed),
                        description = stringResource(R.string.shows_when_empty_center_themed_desc),
                        checked = behaviour.centerThemedIcons,
                        onCheckedChange = viewModel::setCenterThemedIcons,
                    )
                }
                CenterShortcutsCard(
                    shape = persistentPillGroupShape(isFirst = false, isLast = true),
                    shortcuts = behaviour.centerShortcuts,
                    onAdd = { showAddShortcut = true },
                    onRemove = { index ->
                        viewModel.setCenterShortcuts(
                            behaviour.centerShortcuts.toMutableList().apply { removeAt(index) },
                        )
                    },
                    onMove = { index, delta ->
                        viewModel.setCenterShortcuts(behaviour.centerShortcuts.moved(index, delta))
                    },
                )
            }
        }
        PersistentPillActionCard(
            index = "01",
            title = "Tap",
            description = "A quick, single tap on the resting pill.",
            selected = clickAction,
            packageName = clickPackage,
            onActionSelected = viewModel::setShowsWhenEmptyClickAction,
            onChooseApp = { showAppPicker = true },
            quickActionsContent = quickActionsContent,
        )
        PersistentPillActionCard(
            index = "02",
            title = "Double tap",
            description = "Two quick taps in succession.",
            selected = doubleTapAction,
            packageName = doubleTapPackage,
            onActionSelected = viewModel::setPersistentPillDoubleTapAction,
            onChooseApp = { showDoubleTapAppPicker = true },
            quickActionsContent = quickActionsContent,
        )
        PersistentPillActionCard(
            index = "03",
            title = "Touch & Hold",
            description = "Press and hold the resting pill.",
            selected = holdAction,
            packageName = holdPackage,
            onActionSelected = viewModel::setPersistentPillHoldAction,
            onChooseApp = { showHoldAppPicker = true },
            quickActionsContent = quickActionsContent,
        )
    }

    if (showIconSheet) {
        IconChooserSheet(
            hasOverride = source != null,
            onChooseImage = {
                showIconSheet = false
                imagePicker.launch(arrayOf("image/*"))
            },
            onChooseMaterial = {
                showIconSheet = false
                showMaterialPicker = true
            },
            onUseDefault = {
                showIconSheet = false
                viewModel.resetShowsWhenEmptyIcon()
            },
            onDismiss = { showIconSheet = false },
        )
    }

    if (showMaterialPicker) {
        MaterialIconPickerSheet(
            onPick = { iconName ->
                showMaterialPicker = false
                viewModel.setShowsWhenEmptyMaterialIcon(iconName)
            },
            onDismiss = { showMaterialPicker = false },
        )
    }

    if (showAppPicker) {
        AppPickerSheet(
            onPick = { packageName ->
                showAppPicker = false
                viewModel.setShowsWhenEmptyClickPackage(packageName)
            },
            onDismiss = { showAppPicker = false },
        )
    }
    if (showDoubleTapAppPicker) {
        AppPickerSheet(
            onPick = { packageName ->
                showDoubleTapAppPicker = false
                viewModel.setPersistentPillDoubleTapPackage(packageName)
            },
            onDismiss = { showDoubleTapAppPicker = false },
        )
    }
    if (showHoldAppPicker) {
        AppPickerSheet(
            onPick = { packageName ->
                showHoldAppPicker = false
                viewModel.setPersistentPillHoldPackage(packageName)
            },
            onDismiss = { showHoldAppPicker = false },
        )
    }

    if (showAddShortcut) {
        AddShortcutSheet(
            onPickBuiltin = { shortcut ->
                showAddShortcut = false
                viewModel.setCenterShortcuts(behaviour.centerShortcuts + shortcut)
            },
            onChooseApp = {
                showAddShortcut = false
                showCenterAppPicker = true
            },
            onDismiss = { showAddShortcut = false },
        )
    }

    if (showCenterAppPicker) {
        AppPickerSheet(
            onPick = { packageName ->
                showCenterAppPicker = false
                viewModel.setCenterShortcuts(
                    behaviour.centerShortcuts + CenterShortcut.LaunchApp(packageName),
                )
            },
            onDismiss = { showCenterAppPicker = false },
        )
    }
}

/** Moves the item at [index] by [delta] positions, returning the list unchanged if out of range. */
private fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}

/** The chosen "open app" target: the app's icon + name, or a hint to pick one, plus a choose button. */
@Composable
private fun AppChoiceCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    packageName: String?,
    onChoose: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (packageName != null) {
                AppIcon(packageName = packageName)
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = packageName?.let { rememberAppLabel(it) }
                        ?: stringResource(R.string.shows_when_empty_no_app),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (packageName != null) {
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = onChoose) {
                Text(stringResource(R.string.shows_when_empty_choose_app))
            }
        }
    }
}

/**
 * The editor for the expanded center's shortcut row: the current shortcuts in order, each with
 * move-up / move-down / remove controls, plus an "Add shortcut" button. Shown while the empty
 * pill's on-click is [EmptyClickAction.OPEN_CENTER].
 */
@Composable
private fun CenterShortcutsCard(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    shortcuts: List<CenterShortcut>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.shows_when_empty_center_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.shows_when_empty_center_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (shortcuts.isEmpty()) {
                Text(
                    text = stringResource(R.string.center_shortcuts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                shortcuts.forEachIndexed { index, shortcut ->
                    CenterShortcutRow(
                        shortcut = shortcut,
                        canMoveUp = index > 0,
                        canMoveDown = index < shortcuts.lastIndex,
                        onMoveUp = { onMove(index, -1) },
                        onMoveDown = { onMove(index, 1) },
                        onRemove = { onRemove(index) },
                    )
                }
            }
            FilledTonalButton(onClick = onAdd, modifier = Modifier.padding(top = 4.dp)) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.center_add_shortcut),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** One shortcut in the editor: its glyph (or app icon) and label, with reorder and remove controls. */
@Composable
private fun CenterShortcutRow(
    shortcut: CenterShortcut,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CenterShortcutIcon(shortcut)
        Text(
            text = centerShortcutLabel(shortcut),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.center_move_up))
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.center_move_down))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.center_remove))
        }
    }
}

/** A round badge for a shortcut: the real launcher icon for an app, otherwise its catalog glyph. */
@Composable
private fun CenterShortcutIcon(shortcut: CenterShortcut) {
    if (shortcut is CenterShortcut.LaunchApp) {
        AppIcon(packageName = shortcut.packageName)
        return
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CenterShortcutCatalog.iconFor(shortcut),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** A shortcut's label: its fixed string resource, or the app's display name for a launcher. */
@Composable
private fun centerShortcutLabel(shortcut: CenterShortcut): String {
    CenterShortcutCatalog.labelResFor(shortcut)?.let { return stringResource(it) }
    val pkg = (shortcut as? CenterShortcut.LaunchApp)?.packageName ?: return ""
    return rememberAppLabel(pkg)
}

/** A bottom sheet to add a shortcut: pick an app, or any of the built-in actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShortcutSheet(
    onPickBuiltin: (CenterShortcut) -> Unit,
    onChooseApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.center_add_shortcut),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        Text(
            text = stringResource(R.string.center_shizuku_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            item {
                AddShortcutRow(
                    icon = Icons.Rounded.Apps,
                    label = stringResource(R.string.center_add_choose_app),
                    onClick = onChooseApp,
                )
            }
            items(CenterShortcut.BUILTINS, key = { it.encode() }) { shortcut ->
                AddShortcutRow(
                    icon = CenterShortcutCatalog.iconFor(shortcut),
                    label = centerShortcutLabel(shortcut),
                    onClick = { onPickBuiltin(shortcut) },
                )
            }
        }
    }
}

/** A single tappable row in the add-shortcut sheet: a round glyph badge and a label. */
@Composable
private fun AddShortcutRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}

/** A searchable bottom-sheet list of launchable apps; reports the chosen package via [onPick]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val apps by produceState<List<InstalledApp>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.shows_when_empty_choose_app),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.apps_search_hint)) },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))

        val loaded = apps
        if (loaded == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val filtered = remember(loaded, query) {
                val needle = query.trim()
                if (needle.isEmpty()) loaded
                else loaded.filter {
                    it.label.contains(needle, ignoreCase = true) ||
                        it.packageName.contains(needle, ignoreCase = true)
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(app.packageName) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(packageName = app.packageName)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Resolves a package's display label off the main thread, falling back to the package name. */
@Composable
private fun rememberAppLabel(packageName: String): String {
    val context = LocalContext.current
    val label by produceState(initialValue = packageName, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }
    }
    return label
}

/**
 * A round badge previewing the empty pill's chosen icon: a [containerColor] disc (a faint neutral
 * one when unset) behind the glyph — a picked image, a Material vector, or a placeholder when no
 * icon has been chosen yet. Mirrors how the overlay draws it on the resting pill.
 */
@Composable
private fun EmptyIconThumbnail(
    source: IconSource?,
    containerColor: CutoutColor?,
    size: Dp = 48.dp,
) {
    val context = LocalContext.current
    val disc = containerColor?.resolve() ?: MaterialTheme.colorScheme.surfaceVariant
    // Ink that reads on the disc: dark on a light fill, light on a dark one.
    val glyph = if (disc.luminance() > 0.5f) Color.Black.copy(alpha = 0.75f) else Color.White

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = source) {
        value = when (val current = source) {
            is IconSource.Image -> withContext(Dispatchers.IO) {
                Uri.parse(current.uri).loadImageBitmapOrNull(context)
            }
            is IconSource.Material, null -> null
        }
    }
    val materialIcon = (source as? IconSource.Material)?.let { MaterialIconCatalog.iconFor(it.iconName) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(disc),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bitmap
        when {
            loaded != null -> Image(
                bitmap = loaded,
                contentDescription = null,
                modifier = Modifier.size(size * 0.66f).clip(CircleShape),
            )

            materialIcon != null -> Icon(
                imageVector = materialIcon,
                contentDescription = null,
                tint = glyph,
                modifier = Modifier.size(size * 0.5f),
            )

            // No icon picked yet: a faint "add an image" hint glyph.
            else -> Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = glyph.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.4f),
            )
        }
    }
}


@Composable
private fun PillWidgetSlotCard(
    title: String,
    selected: PillWidgetType,
    shape: androidx.compose.ui.graphics.Shape,
    onSelect: (PillWidgetType) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var pending by remember(selected) { mutableStateOf(selected) }
    val label = when (selected) {
        PillWidgetType.NONE -> "Nothing"
        PillWidgetType.ICON -> "Icon"
        PillWidgetType.ANIMATED_ICON -> "Icon"
        PillWidgetType.NETWORK_SPEED -> "Internet speed"
        PillWidgetType.BATTERY -> "Battery percentage"
        PillWidgetType.CLOCK -> "Clock"
        PillWidgetType.DATE -> "Date"
        PillWidgetType.WEEKDAY -> "Weekday"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { pending = selected; showDialog = true },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Change", tint = MaterialTheme.colorScheme.primary)
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    PillWidgetType.entries.filter { it != PillWidgetType.ANIMATED_ICON }.forEach { type ->
                        val optionLabel = when (type) {
                            PillWidgetType.NONE -> "Nothing"
                            PillWidgetType.ICON -> "Icon"
                            PillWidgetType.ANIMATED_ICON -> "Animated icon"
                            PillWidgetType.NETWORK_SPEED -> "Internet speed"
                            PillWidgetType.BATTERY -> "Battery percentage"
                            PillWidgetType.CLOCK -> "Clock"
                            PillWidgetType.DATE -> "Date"
                            PillWidgetType.WEEKDAY -> "Weekday"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { pending = type }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(optionLabel, style = MaterialTheme.typography.bodyLarge)
                            }
                            RadioButton(selected = pending == type, onClick = { pending = type })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onSelect(pending); showDialog = false }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
        )
    }
}


@Composable
private fun PersistentPillSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 0.dp),
    )
}

@Composable
private fun PersistentPillActionCard(
    index: String,
    title: String,
    description: String,
    selected: EmptyClickAction,
    packageName: String?,
    onActionSelected: (EmptyClickAction) -> Unit,
    onChooseApp: () -> Unit,
    quickActionsContent: @Composable () -> Unit,
) {
    val actions = listOf(
        EmptyClickAction.NONE to "None",
        EmptyClickAction.OPEN_APP to "Open app",
        EmptyClickAction.OPEN_CENTER to "Quick actions",
        EmptyClickAction.CIRCLE_TO_SEARCH to "Circle to Search",
    )
    val selectedDescription = when (selected) {
        EmptyClickAction.NONE -> "No action will run for this trigger."
        EmptyClickAction.OPEN_APP -> "Launch the app selected below."
        EmptyClickAction.OPEN_CENTER -> "Expand the configured shortcut panel below."
        EmptyClickAction.CIRCLE_TO_SEARCH -> "Open the system Circle to Search experience."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(index, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEach { (action, label) ->
                    FilterChip(
                        selected = selected == action,
                        onClick = { onActionSelected(action) },
                        label = { Text(label, maxLines = 1) },
                    )
                }
            }
            Text(
                selectedDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedVisibility(visible = selected == EmptyClickAction.OPEN_APP) {
                AppChoiceCard(
                    shape = RoundedCornerShape(18.dp),
                    packageName = packageName,
                    onChoose = onChooseApp,
                )
            }
            AnimatedVisibility(visible = selected == EmptyClickAction.OPEN_CENTER) {
                quickActionsContent()
            }
        }
    }
}
