package com.vikram.expressiveisland.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.DynamicTile
import com.vikram.expressiveisland.core.SystemEventType
import com.vikram.expressiveisland.permissions.Permissions
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.pageTransition
import com.vikram.expressiveisland.ui.screen.tiles.TileSettingsScreen

// TESTING ONLY — flip to true to force both "needs a restart" cards visible even when the grants
// are healthy, so the layout and copy can be eyeballed without breaking a real binding. Must be
// false in anything shipped.
private const val FORCE_STALLED_CARDS_FOR_TESTING = false

/**
 * "Settings" destination. A lightweight list that navigates to focused sub-screens — so the
 * heavy live preview (and the pinned overlay) only exist while the "Size & position" screen
 * is open, never on the list or the icons screen.
 *
 * The individual sub-screens live in the SettingScreens/ folder (same package).
 */
@Composable
fun SettingsTab(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    route: SettingsRoute,
    selectedTile: DynamicTile?,
    selectedEvent: SystemEventType?,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenEvent: (SystemEventType) -> Unit,
    onOpenDynamicTiles: () -> Unit,
    onOpenTile: (DynamicTile) -> Unit,
    onOpenApps: () -> Unit,
    onOpenBehaviour: () -> Unit,
    onOpenShowsWhenEmpty: () -> Unit,
    onOpenAnimation: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBackground: () -> Unit,
    onOpenActionButtons: () -> Unit,
    onOpenShizuku: () -> Unit,
    onOpenPermissionDot: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }

    // Routing (and back navigation, via the bottom bar) is owned by MainScreen.
    // Deeper routes slide in from the right; stepping back slides in from the left, so the
    // motion mirrors the predictive-back peek.
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val forward = targetState.depth >= initialState.depth
            val dir = if (forward) 1 else -1

            pageTransition(
                viewModel.appearance.value.pageTransitionStyle,
                dir,
            )
        },
        label = "settingsRoute",
    ) { current ->
        when (current) {
            SettingsRoute.List -> {
                val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
                SettingsList(
                    contentPadding = contentPadding,
                    cutoutEnabled = behaviour.cutoutEnabled,
                    onCutoutEnabledChange = viewModel::setCutoutEnabled,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onOpenSizePosition = onOpenSizePosition,
                    onOpenEventIcons = onOpenEventIcons,
                    onOpenDynamicTiles = onOpenDynamicTiles,
                    onOpenApps = onOpenApps,
                    onOpenBehaviour = onOpenBehaviour,
                    onOpenAnimation = onOpenAnimation,
                    onOpenAppearance = onOpenAppearance,
                    onOpenShizuku = onOpenShizuku
                )
            }

            SettingsRoute.SizePosition -> SizePositionScreen(viewModel, contentPadding)
            SettingsRoute.EventIcons -> EventIconsScreen(viewModel, contentPadding, onOpenEvent)
            SettingsRoute.EventDetail ->
                selectedEvent?.let { EventDetailScreen(it, viewModel, contentPadding) }

            SettingsRoute.DynamicTiles -> DynamicTilesScreen(viewModel, contentPadding, onOpenTile)
            SettingsRoute.Apps -> AppsScreen(viewModel, contentPadding)
            SettingsRoute.DynamicTileDetail ->
                selectedTile?.let { TileSettingsScreen(it, viewModel, contentPadding) }

            SettingsRoute.Behaviour -> BehaviourScreen(
                viewModel,
                contentPadding,
                onOpenShowsWhenEmpty
            )

            SettingsRoute.ShowsWhenEmpty -> ShowsWhenEmptyScreen(viewModel, contentPadding)
            SettingsRoute.Animation -> AnimationScreen(viewModel, contentPadding)
            SettingsRoute.Appearance -> AppearanceScreen(
                viewModel,
                contentPadding,
                onOpenBackground,
                onOpenActionButtons
            )

            SettingsRoute.Background -> BackgroundScreen(viewModel, contentPadding)
            SettingsRoute.ActionButtons -> ButtonScreen(viewModel, contentPadding)
            SettingsRoute.Shizuku -> ShizukuScreen(viewModel, contentPadding, onOpenPermissionDot)
            SettingsRoute.PermissionDot -> PermissionDotScreen(viewModel, contentPadding)
        }
    }
}

/** The screens reachable from the Settings tab. Hoisted to MainScreen so the bottom bar can
 *  switch to a back pill on the detail screens. */
enum class SettingsRoute { List, SizePosition, EventIcons, EventDetail, DynamicTiles, DynamicTileDetail, Apps, Behaviour, ShowsWhenEmpty, Animation, Appearance, Background, ActionButtons, Shizuku, PermissionDot }

/**
 * The screen that back navigation returns to. Most detail screens go straight back to the list,
 * but Background and ActionButtons are reached from Appearance, so they step back there first.
 */
val SettingsRoute.parent: SettingsRoute
    get() = when (this) {
        SettingsRoute.Background, SettingsRoute.ActionButtons -> SettingsRoute.Appearance
        SettingsRoute.DynamicTileDetail -> SettingsRoute.DynamicTiles
        SettingsRoute.EventDetail -> SettingsRoute.EventIcons
        SettingsRoute.ShowsWhenEmpty -> SettingsRoute.Behaviour
        else -> SettingsRoute.List
    }

/** How far down the navigation stack a route sits, used to pick the slide direction. */
val SettingsRoute.depth: Int
    get() = when (this) {
        SettingsRoute.List -> 0
        SettingsRoute.Background, SettingsRoute.ActionButtons, SettingsRoute.DynamicTileDetail,
        SettingsRoute.EventDetail, SettingsRoute.ShowsWhenEmpty,
            -> 2

        else -> 1
    }

@Composable
private fun SettingsList(
    contentPadding: PaddingValues,
    cutoutEnabled: Boolean,
    onCutoutEnabledChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSizePosition: () -> Unit,
    onOpenEventIcons: () -> Unit,
    onOpenDynamicTiles: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenBehaviour: () -> Unit,
    onOpenAnimation: () -> Unit,
    onOpenShizuku: () -> Unit,
    onOpenAppearance: () -> Unit,
) {
    val context = LocalContext.current
    // Re-reads on resume so returning from the system Accessibility settings updates immediately.
    val accessibilityAvailable = rememberAccessibilityGranted()
    // Granted is not the same as running: Android keeps the grant across an app update but often
    // leaves the service unbound, so the island is dead while the grant still reads green.
    val accessibilityRunning = rememberAccessibilityRunning()
    // The same stale-binding trap hits the notification listener, which feeds every dynamic tile
    // (music, phone, timer). MainActivity asks for a rebind on resume, but surface a card too in
    // case that best-effort request doesn't take on this OEM.
    val notificationsGranted = rememberNotificationAccessGranted()
    val notificationsRunning = rememberNotificationListenerRunning()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchBar(
            modifier = Modifier.fillMaxWidth(),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        searchExpanded = true
                    },
                    onSearch = { searchExpanded = true },
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                    placeholder = { Text("Search settings") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                searchExpanded = true
                            }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                )
            },
            expanded = searchExpanded,
            onExpandedChange = { searchExpanded = it },
        ) {
            if (searchQuery.isBlank()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Try searching for", style = MaterialTheme.typography.titleSmall)
                    listOf("Island size", "Auto collapse", "Lock screen", "Animation speed", "Music progress", "Shadow", "Notification icons").forEach { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { searchQuery = suggestion; searchExpanded = true }
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            val normalizedQuery = searchQuery.trim().lowercase()
            val matches = { title: String, subtitle: String, keywords: String ->
                val fields = listOf(title, subtitle, keywords)
                fields.any { it.contains(normalizedQuery, ignoreCase = true) } ||
                    normalizedQuery.split(Regex("\\s+")).filter(String::isNotBlank).all { token ->
                        fields.any { it.contains(token, ignoreCase = true) }
                    }
            }
            var resultCount = 0
            if (matches("Dynamic Cutout", "Show or hide the island", "island overlay enable disable accessibility service")) {
                resultCount++
                CutoutEnableCard(
                    enabled = cutoutEnabled,
                    onEnabledChange = onCutoutEnabledChange,
                    onOpenAccessibility = { Permissions.openAccessibilitySettings(context) },
                )
            }
            if (matches("Size & position", "Adjust the cutout size and position", "width height offset size position camera")) {
                resultCount++
                SettingsListItem(Icons.Rounded.Tune, "Size & position", "Adjust the cutout size and position", onOpenSizePosition)
            }
            if (matches(stringResource(R.string.appearance_section_title), stringResource(R.string.settings_appearance_subtitle), "appearance shadow stroke color theme background buttons")) {
                resultCount++
                SettingsListItem(Icons.Rounded.ColorLens, stringResource(R.string.appearance_section_title), stringResource(R.string.settings_appearance_subtitle), onOpenAppearance)
            }
            if (matches(stringResource(R.string.behaviour_title), stringResource(R.string.settings_behaviour_subtitle), "behaviour behavior duration timeout lockscreen auto collapse swipe landscape orientation dismiss empty")) {
                resultCount++
                SettingsListItem(Icons.Rounded.Timer, stringResource(R.string.behaviour_title), stringResource(R.string.settings_behaviour_subtitle), onOpenBehaviour)
            }
            if (matches(stringResource(R.string.animation_title), stringResource(R.string.settings_animation_subtitle), "animation transition motion haptic")) {
                resultCount++
                SettingsListItem(Icons.Rounded.Animation, stringResource(R.string.animation_title), stringResource(R.string.settings_animation_subtitle), onOpenAnimation)
            }
            if (matches(stringResource(R.string.shizuku_options_title), stringResource(R.string.settings_shizuku_subtitle), "shizuku permission dot status bar accessibility")) {
                resultCount++
                SettingsListItem(Icons.Rounded.Terminal, stringResource(R.string.shizuku_options_title), stringResource(R.string.settings_shizuku_subtitle), onOpenShizuku)
            }
            if (matches(stringResource(R.string.dynamic_tiles_title), stringResource(R.string.settings_dynamic_tiles_subtitle), "dynamic tiles music timer stopwatch phone call battery charging wifi download recording flashlight")) {
                resultCount++
                SettingsListItem(Icons.Rounded.GridView, stringResource(R.string.dynamic_tiles_title), stringResource(R.string.settings_dynamic_tiles_subtitle), onOpenDynamicTiles)
            }
            if (matches(stringResource(R.string.section_icons_title), stringResource(R.string.settings_icons_subtitle), "event notification icon icons call charging lock biometrics")) {
                resultCount++
                SettingsListItem(Icons.Rounded.Notifications, stringResource(R.string.section_icons_title), stringResource(R.string.settings_icons_subtitle), onOpenEventIcons)
            }
            if (matches(stringResource(R.string.apps_title), stringResource(R.string.settings_apps_subtitle), "apps application notifications app list")) {
                resultCount++
                SettingsListItem(Icons.Rounded.Apps, stringResource(R.string.apps_title), stringResource(R.string.settings_apps_subtitle), onOpenApps)
            }
            if (resultCount == 0) {
                Text(
                    text = "No matching settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
        // Accessibility access permission request if needed
        AnimatedVisibility(
            visible = !accessibilityAvailable,
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp))
        ) {
            SettingsListItem(
                icon = Icons.Rounded.ErrorOutline,
                subtitle = stringResource(R.string.settings_access_missing),
                title = stringResource(R.string.perm_accessibility_title),
                onClick = { Permissions.openAccessibilitySettings(context) },
                bgColor = MaterialTheme.colorScheme.primaryContainer,
                fgColor = MaterialTheme.colorScheme.onPrimaryContainer

            )
        }

        // Granted but not bound — the grant survived an update, the service did not. Distinct from
        // the card above: the fix is toggling the existing grant off and on, not granting it.
        AnimatedVisibility(
            visible = FORCE_STALLED_CARDS_FOR_TESTING || (accessibilityAvailable && !accessibilityRunning),
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp))
        ) {
            SettingsListItem(
                icon = Icons.Rounded.ErrorOutline,
                subtitle = stringResource(R.string.settings_access_stalled),
                title = stringResource(R.string.settings_access_stalled_title),
                onClick = { Permissions.openAccessibilitySettings(context) },
                bgColor = MaterialTheme.colorScheme.errorContainer,
                fgColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // Notification access granted but the listener isn't bound — dynamic tiles are starved
        // even though the grant reads green. Same off-and-on fix as the accessibility card above.
        AnimatedVisibility(
            visible = FORCE_STALLED_CARDS_FOR_TESTING || (notificationsGranted && !notificationsRunning),
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp))
        ) {
            SettingsListItem(
                icon = Icons.Rounded.ErrorOutline,
                subtitle = stringResource(R.string.settings_notif_stalled),
                title = stringResource(R.string.settings_notif_stalled_title),
                onClick = { Permissions.openNotificationAccessSettings(context) },
                bgColor = MaterialTheme.colorScheme.errorContainer,
                fgColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        CutoutEnableCard(
            enabled = cutoutEnabled,
            onEnabledChange = onCutoutEnabledChange,
            onOpenAccessibility = { Permissions.openAccessibilitySettings(context) },
        )

        // Customization of the cutout
        Column(
            modifier = Modifier.clip(RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsListItem(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.appearance_title),
                subtitle = stringResource(R.string.settings_size_subtitle),
                onClick = onOpenSizePosition,
            )
            SettingsListItem(
                icon = Icons.Rounded.ColorLens,
                title = stringResource(R.string.appearance_section_title),
                subtitle = stringResource(R.string.settings_appearance_subtitle),
                onClick = onOpenAppearance,
            )
            SettingsListItem(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.behaviour_title),
                subtitle = stringResource(R.string.settings_behaviour_subtitle),
                onClick = onOpenBehaviour,
            )
            SettingsListItem(
                icon = Icons.Rounded.Animation,
                title = stringResource(R.string.animation_title),
                subtitle = stringResource(R.string.settings_animation_subtitle),
                onClick = onOpenAnimation,
            )
            SettingsListItem(
                icon = Icons.Rounded.Terminal,
                title = stringResource(R.string.shizuku_options_title),
                subtitle = stringResource(R.string.settings_shizuku_subtitle),
                onClick = onOpenShizuku,
            )
        }

        // Events and tiles that trigger the cutout
        Column(
            modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsListItem(
                icon = Icons.Rounded.GridView,
                title = stringResource(R.string.dynamic_tiles_title),
                subtitle = stringResource(R.string.settings_dynamic_tiles_subtitle),
                onClick = onOpenDynamicTiles,
            )
            SettingsListItem(
                icon = Icons.Rounded.Notifications,
                title = stringResource(R.string.section_icons_title),
                subtitle = stringResource(R.string.settings_icons_subtitle),
                onClick = onOpenEventIcons,
            )
            SettingsListItem(
                icon = Icons.Rounded.Apps,
                title = stringResource(R.string.apps_title),
                subtitle = stringResource(R.string.settings_apps_subtitle),
                onClick = onOpenApps,
            )
        }
        }
    }
}

@Composable
private fun CutoutEnableCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenAccessibility)
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.cutout_enable_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.cutout_enable_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
fun SettingsListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    bgColor: Color = MaterialTheme.colorScheme.surface,
    fgColor: Color? = null,
    hapticsOnClick: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (hapticsOnClick) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }

                onClick()
            }),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = fgColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
