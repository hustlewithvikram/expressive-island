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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
@OptIn(ExperimentalMaterial3Api::class)
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
                    viewModel = viewModel,
                    cutoutEnabled = behaviour.cutoutEnabled,
                    onCutoutEnabledChange = viewModel::setCutoutEnabled,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    searchExpanded = searchExpanded,
                    onSearchExpandedChange = { searchExpanded = it },
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
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsList(
    contentPadding: PaddingValues,
    viewModel: AppViewModel,
    cutoutEnabled: Boolean,
    onCutoutEnabledChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
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
    val focusManager = LocalFocusManager.current
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
    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val musicTile by viewModel.musicTile.collectAsStateWithLifecycle()
    val hideNotificationIcons by viewModel.hideNotificationIcons.collectAsStateWithLifecycle()
    val hideSystemInfo by viewModel.hideSystemInfo.collectAsStateWithLifecycle()
    val hideClock by viewModel.hideClock.collectAsStateWithLifecycle()
    val silenceSystemAlerts by viewModel.silenceSystemAlerts.collectAsStateWithLifecycle()
    val permissionDotEnabled by viewModel.permissionDotEnabled.collectAsStateWithLifecycle()
    val permissionDotVertical by viewModel.permissionDotVertical.collectAsStateWithLifecycle()

    // The entire settings page scrolls as one surface: the search field moves with the
    // cards below it. Put the top inset inside the scroll content so it scrolls away naturally;
    // omit the bottom inset to avoid the extra blank space at the bottom.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                // Keep the final settings cards clear of the bottom navigation bar, with
                // a little extra breathing room after the last item.
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchBar(
            // SearchBar expands into a large surface. The parent is vertically scrollable,
            // so it otherwise receives unbounded height during expansion on some Compose builds.
            // Keep the official Material 3 surface bounded to the available screen height.
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (LocalConfiguration.current.screenHeightDp - 120).coerceAtLeast(320).dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = {
                        onSearchQueryChange(it)
                        onSearchExpandedChange(true)
                    },
                    onSearch = { onSearchExpandedChange(true) },
                    expanded = searchExpanded,
                    onExpandedChange = onSearchExpandedChange,
                    placeholder = { Text("Search settings") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                )
            },
            expanded = searchExpanded,
            onExpandedChange = onSearchExpandedChange,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            // Search results stay inside the official Material 3 expanded surface.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                .clickable { onSearchQueryChange(suggestion); onSearchExpandedChange(true) }
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                val normalizedQuery = searchQuery.trim().lowercase()
                val matches = { title: String, subtitle: String, keywords: String ->
                    val fields = listOf(title, subtitle, keywords)
                    fields.any { it.contains(normalizedQuery, ignoreCase = true) } ||
                        normalizedQuery.split(Regex("\\s+")).filter(String::isNotBlank).all { token ->
                            fields.any { it.contains(token, ignoreCase = true) }
                        }
                }
                var resultCount = 0
                if (matches("Hide on lock screen", "Hide the island while the device is locked", "lockscreen lock screen lock")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Hide on lock screen", "Hide the island while the device is locked", behaviour.hideOnLockscreen, viewModel::setHideOnLockscreen)
                }
                if (matches("Hide in landscape", "Hide the island in landscape orientation", "landscape horizontal rotation")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Hide in landscape", "Hide the island in landscape orientation", behaviour.hideInLandscape, { viewModel.setHorizontalCutoutMode(if (it) com.vikram.expressiveisland.data.HorizontalCutoutMode.HIDDEN else com.vikram.expressiveisland.data.HorizontalCutoutMode.CENTER) })
                }
                if (matches("Auto collapse", "Collapse the expanded island automatically", "collapse shrink timeout delay")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Auto collapse", "Collapse the expanded island automatically", behaviour.expandedAutoCollapse, viewModel::setExpandedAutoCollapse)
                }
                if (matches("Disappear on shrink", "Hide instead of returning to the normal pill", "shrink disappear")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Disappear on shrink", "Hide instead of returning to the normal pill", behaviour.expandedDisappearOnShrink, viewModel::setExpandedDisappearOnShrink)
                }
                if (matches("Notifications auto expand", "Expand for incoming notifications", "notification expansion")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Notifications auto expand", "Expand for incoming notifications", behaviour.notificationsAutoExpand, viewModel::setNotificationsAutoExpand)
                }
                if (matches("Ignore silent notifications", "Do not react to silent notifications", "silent notification mute")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Ignore silent notifications", "Do not react to silent notifications", behaviour.ignoreSilentNotifications, viewModel::setIgnoreSilentNotifications)
                }
                if (matches("Show action buttons", "Show actions inside expanded notifications", "reply buttons actions")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Show action buttons", "Show actions inside expanded notifications", behaviour.showActionButtons, viewModel::setShowActionButtons)
                }
                if (matches("Toast on action", "Show a confirmation after an action", "toast feedback confirmation")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Toast on action", "Show a confirmation after an action", behaviour.toastOnAction, viewModel::setToastOnAction)
                }
                if (matches("Shrink on swipe up", "Swipe upward to shrink the island", "gesture swipe up")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Shrink on swipe up", "Swipe upward to shrink the island", behaviour.shrinkOnSwipeUp, viewModel::setShrinkOnSwipeUp)
                }
                if (matches("Swipe to dismiss", "Allow horizontal swipe dismissal", "gesture swipe left right")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Swipe to dismiss", "Allow horizontal swipe dismissal", behaviour.swipeToDismiss, viewModel::setSwipeToDismiss)
                }
                if (matches("Vibrate on tap", "Use haptic feedback when the island is tapped", "vibration haptics touch")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Vibrate on tap", "Use haptic feedback when the island is tapped", behaviour.vibrateOnTap, viewModel::setVibrateOnTap)
                }
                if (matches("Haptics on pop", "Use haptics when the island appears", "vibration haptic appearance")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Haptics on pop", "Use haptics when the island appears", behaviour.hapticsOnPop, viewModel::setHapticsOnPop)
                }
                if (matches("Dismiss notifications", "Dismiss notifications when handled", "clear notification remove")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Dismiss notifications", "Dismiss notifications when handled", behaviour.dismissNotifications, viewModel::setDismissNotifications)
                }
                if (matches("Display while DND", "Show content while Do Not Disturb is enabled", "do not disturb quiet mode")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Display while DND", "Show content while Do Not Disturb is enabled", behaviour.displayWhileDnd, viewModel::setDisplayWhileDnd)
                }
                if (matches("Alert on notification", "Show alert feedback for notifications", "notification alert")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Alert on notification", "Show alert feedback for notifications", behaviour.alertOnNotification, viewModel::setAlertOnNotification)
                }
                if (matches("Show when empty", "Keep the island visible when idle", "idle empty bubble")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Show when empty", "Keep the island visible when idle", behaviour.showsWhenEmpty, viewModel::setShowsWhenEmpty)
                }
                if (matches("Shadow", "Show a shadow around the island", "appearance depth elevation")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Shadow", "Show a shadow around the island", appearance.shadowEnabled, viewModel::setShadowEnabled)
                }
                if (matches("Stroke outline", "Draw an outline around the island", "border outline width")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Stroke outline", "Draw an outline around the island", appearance.strokeEnabled, viewModel::setStrokeEnabled)
                }
                if (matches("Show source app name", "Show the originating app on notifications", "application notification source")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Show source app name", "Show the originating app on notifications", appearance.showSourceAppName, viewModel::setShowSourceAppName)
                }
                if (matches("Show timestamp", "Show notification time information", "time date clock")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Show timestamp", "Show notification time information", appearance.showTimestamp, viewModel::setShowTimestamp)
                }
                if (matches("Show full notification text", "Show the complete notification message", "message text content")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Show full notification text", "Show the complete notification message", appearance.showFullNotificationText, viewModel::setShowFullNotificationText)
                }
                if (matches("Prefer dynamic icon color", "Use app-provided dynamic icon colors", "icon color app accent")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Prefer dynamic icon color", "Use app-provided dynamic icon colors", appearance.preferDynamicIconColor, viewModel::setPreferDynamicIconColor)
                }
                if (matches("Hide notification icons", "Hide app notification icons from the status bar", "status bar app icons")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Hide notification icons", "Hide app notification icons from the status bar", hideNotificationIcons, viewModel::setHideNotificationIcons)
                }
                if (matches("Hide system info icons", "Hide clock, battery, Wi-Fi and signal icons", "status bar clock battery wifi signal")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Hide system info icons", "Hide clock, battery, Wi-Fi and signal icons", hideSystemInfo, viewModel::setHideSystemInfo)
                }
                if (matches("Hide status bar clock", "Hide the system clock", "time status bar")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Hide status bar clock", "Hide the system clock", hideClock, viewModel::setHideClock)
                }
                if (matches("Silence system alerts", "Silence system alerts while the island handles notifications", "sound vibration heads up")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Silence system alerts", "Silence system alerts while the island handles notifications", silenceSystemAlerts, viewModel::setSilenceSystemAlerts)
                }
                if (matches("Permission dots", "Show camera, microphone and location activity", "privacy camera microphone location")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Permission dots", "Show camera, microphone and location activity", permissionDotEnabled, viewModel::setPermissionDotEnabled)
                }
                if (matches("Vertical permission dots", "Stack permission indicators vertically", "privacy dots orientation")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Vertical permission dots", "Stack permission indicators vertically", permissionDotVertical, viewModel::setPermissionDotVertical)
                }
                if (matches("Show album art", "Show album artwork in the music tile", "music album cover artwork")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Show album art", "Show album artwork in the music tile", musicTile.showAlbumArt, viewModel::setMusicShowAlbumArt)
                }
                if (matches("Rotate album art", "Rotate the album artwork while music plays", "music album rotation spinning")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Rotate album art", "Rotate the album artwork while music plays", musicTile.rotateAlbumArt, viewModel::setMusicRotateAlbumArt)
                }
                if (matches("Album art stroke", "Outline the album artwork", "music border outline")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Album art stroke", "Outline the album artwork", musicTile.albumArtStroke, viewModel::setMusicAlbumArtStroke)
                }
                if (matches("Expand on play", "Expand the island when music starts", "music auto expand playback")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Expand on play", "Expand the island when music starts", musicTile.expandOnPlay, viewModel::setMusicExpandOnPlay)
                }
                if (matches("Visible in player app", "Keep the music tile visible in the player app", "music player application")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Visible in player app", "Keep the music tile visible in the player app", musicTile.visibleInPlayerApp, viewModel::setMusicVisibleInPlayerApp)
                }
                if (matches("Music controls", "Show playback controls in the music tile", "music play pause next previous buttons")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Music controls", "Show playback controls in the music tile", musicTile.showControls, viewModel::setMusicShowControls)
                }
                if (matches("Music progress", "Show playback progress", "music seek timeline progress bar")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Music progress", "Show playback progress", musicTile.showProgress, viewModel::setMusicShowProgress)
                }
                if (matches("Music visualizer", "Show audio visualizer in the expanded tile", "music bars waveform pulse audio")) {
                    resultCount++
                    SettingsToggleCard(RoundedCornerShape(24.dp), "Music visualizer", "Show audio visualizer in the expanded tile", musicTile.showVisualizer, viewModel::setMusicShowVisualizer)
                }
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
                if (matches("Normal duration", "How long event content stays visible", "seconds event duration timeout")) {
                    resultCount++
                    SearchSliderResult("Normal duration", "How long event content stays visible", behaviour.normalDurationSeconds.toFloat(), 1f..10f, 1f, { "${it.toInt()} sec" }) { viewModel.setNormalDurationSeconds(it.toInt()) }
                }
                if (matches("Collapse delay", "Time before the expanded island collapses", "seconds delay timeout")) {
                    resultCount++
                    SearchSliderResult("Collapse delay", "Time before the expanded island collapses", behaviour.expandedCollapseSeconds.toFloat(), 1f..15f, 1f, { "${it.toInt()} sec" }) { viewModel.setExpandedCollapseSeconds(it.toInt()) }
                }
                if (matches("Animation duration", "Duration for non-expressive animations", "motion speed milliseconds ms")) {
                    resultCount++
                    SearchSliderResult("Animation duration", "Duration for non-expressive animations", behaviour.animationDurationMs.toFloat(), 0f..1000f, 20f, { "${it.toInt()} ms" }) { viewModel.setAnimationDurationMs(it.toInt()) }
                }
                if (matches("Stroke width", "Thickness of the island outline", "border outline thickness dp")) {
                    resultCount++
                    SearchSliderResult("Stroke width", "Thickness of the island outline", appearance.strokeWidthDp.toFloat(), 1f..8f, 1f, { "${it.toInt()} dp" }) { viewModel.setStrokeWidth(it.toInt()) }
                }
                if (matches("Action button height", "Height of expanded action buttons", "reply button size dp")) {
                    resultCount++
                    SearchSliderResult("Action button height", "Height of expanded action buttons", appearance.actionButtonHeightDp.toFloat(), 36f..56f, 1f, { "${it.toInt()} dp" }) { viewModel.setActionButtonHeight(it.toInt()) }
                }
                if (resultCount == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                            Text("Nothing matched your search", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text(
                                "Try another word, or search for “duration”, “shadow” or “notifications”.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            }
        }

        if (searchQuery.isBlank() && !searchExpanded) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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


@Composable
private fun SearchSliderResult(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    formatValue: (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableStateOf(value) }
    SettingsSliderCard(
        shape = RoundedCornerShape(24.dp),
        title = title,
        description = description,
        valueText = formatValue(sliderValue),
        value = sliderValue,
        valueRange = valueRange,
        step = step,
        onValueChange = { sliderValue = it },
        onCommit = { onCommit(sliderValue) },
    )
}
