package com.vikram.expressiveisland.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PhoneCallback
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.notifications.TestCaller
import com.vikram.expressiveisland.notifications.TestNotifier
import com.vikram.expressiveisland.permissions.Permissions
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.components.ExpressiveSegmentedRow
import com.vikram.expressiveisland.ui.pageTransition
import com.vikram.expressiveisland.ui.theme.AppTheme

/**
 * Screens reachable from the Profile tab.
 *
 * Testing remains as a route for compatibility with existing navigation.
 * The actual testing controls are now displayed directly on the Profile list.
 */
enum class ProfileRoute {
    List,
    Changelog,
    PermissionDetails,
    Testing,
}

@Composable
fun ProfileTab(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    route: ProfileRoute,
    onOpenChangelog: () -> Unit,
    onOpenPermissionDetails: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val dir = if (targetState != ProfileRoute.List) 1 else -1

            pageTransition(
                viewModel.appearance.value.pageTransitionStyle,
                dir,
            )
        },
        label = "profileRoute",
    ) { current ->

        when (current) {
            ProfileRoute.List -> {
                ProfileList(
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                    onOpenChangelog = onOpenChangelog,
                    onOpenPermissionDetails = onOpenPermissionDetails,
                    onExportSettings = onExportSettings,
                    onImportSettings = onImportSettings,
                )
            }

            ProfileRoute.Changelog -> {
                ChangelogScreen(contentPadding)
            }

            ProfileRoute.PermissionDetails -> {
                PermissionDetailsScreen(contentPadding)
            }

            ProfileRoute.Testing -> {
                /*
                 * Kept for navigation compatibility.
                 *
                 * Testing controls are now directly available on the
                 * Profile list, so this route does not need to be used
                 * by the bottom navigation.
                 */
                TestingScreen(contentPadding)
            }
        }
    }
}

@Composable
private fun ProfileList(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
    onOpenChangelog: () -> Unit,
    onOpenPermissionDetails: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    val context = LocalContext.current

    val theme by viewModel.theme.collectAsStateWithLifecycle()

    val permissionStatus = rememberPermissionStatus()

    val versionName = remember {
        runCatching {
            context.packageManager
                .getPackageInfo(
                    context.packageName,
                    0,
                )
                .versionName
        }.getOrNull() ?: "—"
    }

    val openUrl = remember(context) {
        { url: String ->
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(url),
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK,
                ),
            )
        }
    }

    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState(),
            )
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        // =====================================================================
        // HEADER
        // =====================================================================

        AppHeader()

        Spacer(
            modifier = Modifier.height(20.dp),
        )

        // =====================================================================
        // APPEARANCE
        // =====================================================================

        SectionHeader(
            title = "Appearance",
            description = "Customize how Expressive Island looks.",
        )

        ThemeCard(
            selected = theme,
            onSelect = viewModel::setTheme,
        )

        // =====================================================================
        // ACCESS
        // =====================================================================

        AccessSection(
            status = permissionStatus,
            context = context,
            onOpenPermissionDetails = {
                haptics.performHapticFeedback(
                    HapticFeedbackType.TextHandleMove,
                )
                onOpenPermissionDetails()
            },
        )

        // =====================================================================
        // TESTING
        // =====================================================================

        TestingSection(
            context = context,
        )

        // =====================================================================
        // APP
        // =====================================================================

        SectionHeader(
            title = "App",
            description = "Version information and configuration tools.",
        )

        VersionCard(
            versionName = versionName,
            onClick = {
                haptics.performHapticFeedback(
                    HapticFeedbackType.TextHandleMove,
                )
                onOpenChangelog()
            },
        )

        // =====================================================================
        // BACKUP & RESTORE
        // =====================================================================

        SectionHeader(
            title = "Backup & Restore",
            description = "Save your Expressive Island settings or restore them later.",
        )

        ExportSettingsCard(
            onExportSettings = onExportSettings,
            onImportSettings = onImportSettings,
        )

        // =====================================================================
        // PROJECT
        // =====================================================================

        SectionHeader(
            title = "Project",
            description = "Explore the project and its development.",
        )

        val githubProjectUrl =
            stringResource(
                R.string.profile_github_project_url,
            )

        val githubProfileUrl =
            stringResource(
                R.string.profile_github_url,
            )

        val coffeeUrl =
            stringResource(
                R.string.profile_coffee_url,
            )

        val linkedInUrl =
            stringResource(
                R.string.profile_linkedin_url,
            )

        GitHubCard(
            onClick = {
                haptics.performHapticFeedback(
                    HapticFeedbackType.TextHandleMove,
                )
                openUrl(githubProjectUrl)
            },
        )

        DevCard(
            onOpenGitHub = {
                openUrl(githubProfileUrl)
            },
            onOpenCoffee = {
                openUrl(coffeeUrl)
            },
            onOpenLinkedIn = {
                openUrl(linkedInUrl)
            },
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )
    }
}

// =============================================================================
// SECTION HEADER
// =============================================================================

@Composable
private fun SectionHeader(
    title: String,
    description: String? = null,
) {
    Column(
        modifier = Modifier.padding(
            start = 8.dp,
            top = 10.dp,
            bottom = 4.dp,
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (description != null) {
            Spacer(
                modifier = Modifier.height(2.dp),
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// =============================================================================
// GROUPED CARD SYSTEM
// =============================================================================

private enum class GroupPosition {
    Single,
    First,
    Middle,
    Last,
}

private fun groupedShape(
    position: GroupPosition,
    radius: Dp = 20.dp,
): RoundedCornerShape {
    val smallRadius = 5.dp

    return when (position) {
        GroupPosition.Single -> {
            RoundedCornerShape(radius)
        }

        GroupPosition.First -> {
            RoundedCornerShape(
                topStart = radius,
                topEnd = radius,
                bottomStart = smallRadius,
                bottomEnd = smallRadius,
            )
        }

        GroupPosition.Middle -> {
            RoundedCornerShape(
                topStart = smallRadius,
                topEnd = smallRadius,
                bottomStart = smallRadius,
                bottomEnd = smallRadius,
            )
        }

        GroupPosition.Last -> {
            RoundedCornerShape(
                topStart = smallRadius,
                topEnd = smallRadius,
                bottomStart = radius,
                bottomEnd = radius,
            )
        }
    }
}

@Composable
private fun GroupedCard(
    position: GroupPosition,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = groupedShape(position),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        },
    )
}

// =============================================================================
// PERMISSION STATE
// =============================================================================

private data class ProfilePermissionStatus(
    val notifications: Boolean,
    val accessibility: Boolean,
    val batteryIgnored: Boolean,
) {
    /*
     * Battery optimization is useful for reliability but isn't treated as
     * an essential permission.
     */
    val allEssentialGranted: Boolean
        get() = notifications && accessibility
}

@Composable
private fun rememberPermissionStatus(): ProfilePermissionStatus {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun read(): ProfilePermissionStatus {
        return ProfilePermissionStatus(
            notifications =
                Permissions.isNotificationAccessGranted(
                    context,
                ),

            accessibility =
                Permissions.isAccessibilityGranted(
                    context,
                ),

            batteryIgnored =
                Permissions.isBatteryOptimizationIgnored(
                    context,
                ),
        )
    }

    var status by remember {
        mutableStateOf(
            read(),
        )
    }

    DisposableEffect(
        lifecycleOwner,
    ) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    status = read()
                }
            }

        lifecycleOwner.lifecycle.addObserver(
            observer,
        )

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(
                observer,
            )
        }
    }

    return status
}

// =============================================================================
// ACCESS
// =============================================================================

@Composable
private fun AccessSection(
    status: ProfilePermissionStatus,
    context: Context,
    onOpenPermissionDetails: () -> Unit,
) {
    SectionHeader(
        title = "Access",
        description = "System access required for Expressive Island to work correctly.",
    )

    if (status.allEssentialGranted) {
        AllSetCard()

        Spacer(
            modifier = Modifier.height(8.dp),
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {

        PermissionGroupCard(
            position = GroupPosition.First,
            icon = Icons.Rounded.Notifications,
            title = stringResource(
                R.string.perm_notifications_title,
            ),
            description = stringResource(
                R.string.perm_notifications_desc,
            ),
            granted = status.notifications,
            onClick = {
                Permissions.openNotificationAccessSettings(
                    context,
                )
            },
        )

        PermissionGroupCard(
            position = GroupPosition.Middle,
            icon = Icons.Rounded.Shield,
            title = stringResource(
                R.string.perm_accessibility_title,
            ),
            description = stringResource(
                R.string.perm_accessibility_desc,
            ),
            granted = status.accessibility,
            onClick = {
                Permissions.openAccessibilitySettings(
                    context,
                )
            },
        )

        PermissionGroupCard(
            position = GroupPosition.Last,
            icon = Icons.Rounded.BatterySaver,
            title = stringResource(
                R.string.perm_battery_title,
            ),
            description = stringResource(
                R.string.perm_battery_desc,
            ),
            granted = status.batteryIgnored,
            onClick = {
                Permissions.requestIgnoreBatteryOptimization(
                    context,
                )
            },
        )
    }

    Spacer(
        modifier = Modifier.height(8.dp),
    )

    PermissionDetailsCard(
        onClick = onOpenPermissionDetails,
    )
}

@Composable
private fun PermissionGroupCard(
    position: GroupPosition,
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = groupedShape(position),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )

            Spacer(
                modifier = Modifier.width(14.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(
                    modifier = Modifier.height(2.dp),
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp),
            )

            // Explicit action/status affordance restored.
            Surface(
                modifier = Modifier.clickable(
                    onClick = onClick,
                ),
                shape = RoundedCornerShape(50),
                color = if (granted) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ) {
                Text(
                    text = if (granted) {
                        "Enabled"
                    } else {
                        "Tap to enable"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (granted) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier.padding(
                        horizontal = 11.dp,
                        vertical = 6.dp,
                    ),
                )
            }
        }
    }
}

// =============================================================================
// ALL SET
// =============================================================================

@Composable
private fun AllSetCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )

            Spacer(
                modifier = Modifier.width(14.dp),
            )

            Column {
                Text(
                    text = stringResource(
                        R.string.all_set_title,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Text(
                    text = stringResource(
                        R.string.all_set_desc,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// =============================================================================
// TESTING
// =============================================================================

@Composable
private fun TestingSection(
    context: Context,
) {
    var pendingPost by remember {
        mutableStateOf<(() -> Unit)?>(null)
    }

    val postPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->

            val action = pendingPost

            /*
             * Clear the pending action before executing it.
             * This prevents an accidental second execution if Compose
             * recomposes while the action is running.
             */
            pendingPost = null

            if (granted) {
                action?.invoke()
            }
        }

    fun postWithPermission(
        send: () -> Unit,
    ) {
        if (TestNotifier.canPost(context)) {
            send()
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            pendingPost = send

            postPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    SectionHeader(
        title = "Testing",
        description = "Test how Expressive Island responds to notifications and calls.",
    )

    Spacer(
        modifier = Modifier.height(2.dp),
    )

    // -------------------------------------------------------------------------
    // NOTIFICATIONS
    // -------------------------------------------------------------------------

    TestGroupHeader(
        title = "Notifications",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {

        TestCard(
            position = GroupPosition.First,
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(
                R.string.action_send_test,
            ),
            description = "Test a standard notification.",
            onClick = {
                postWithPermission {
                    TestNotifier.send(context)
                }
            },
        )

        TestCard(
            position = GroupPosition.Middle,
            icon = Icons.Rounded.NotificationsNone,
            title = stringResource(
                R.string.action_send_test_plain,
            ),
            description = "Test a simple notification.",
            onClick = {
                postWithPermission {
                    TestNotifier.sendPlain(context)
                }
            },
        )

        TestCard(
            position = GroupPosition.Last,
            icon = Icons.Rounded.Downloading,
            title = stringResource(
                R.string.action_send_test_progress,
            ),
            description = "Test a notification with progress.",
            onClick = {
                postWithPermission {
                    TestNotifier.sendProgress(context)
                }
            },
        )
    }

    Spacer(
        modifier = Modifier.height(12.dp),
    )

    // -------------------------------------------------------------------------
    // CALLS
    // -------------------------------------------------------------------------

    TestGroupHeader(
        title = "Calls",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {

        TestCard(
            position = GroupPosition.First,
            icon = Icons.Rounded.Call,
            title = stringResource(
                R.string.action_send_test_call,
            ),
            description = "Test the connected-call pill.",
            onClick = {
                TestCaller.toggle(
                    context,
                    TestCaller.Kind.CONNECTED,
                )
            },
        )

        TestCard(
            position = GroupPosition.Last,
            icon = Icons.AutoMirrored.Rounded.PhoneCallback,
            title = stringResource(
                R.string.action_send_test_incoming_call,
            ),
            description = "Test the incoming-call pill.",
            onClick = {
                TestCaller.toggle(
                    context,
                    TestCaller.Kind.INCOMING,
                )
            },
        )
    }

    Spacer(
        modifier = Modifier.height(12.dp),
    )

    // -------------------------------------------------------------------------
    // ISLAND / EDGE CASES
    // -------------------------------------------------------------------------

    TestGroupHeader(
        title = "Island",
    )

    TestCard(
        position = GroupPosition.Single,
        icon = Icons.Rounded.Layers,
        title = stringResource(
            R.string.action_send_test_double,
        ),
        description = "Test simultaneous notifications and duplicate handling.",
        onClick = {
            postWithPermission {
                TestNotifier.sendPair(context)
            }
        },
    )
}

@Composable
private fun TestGroupHeader(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = 8.dp,
            bottom = 6.dp,
        ),
    )
}

@Composable
private fun TestCard(
    position: GroupPosition,
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = groupedShape(position),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 13.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(25.dp),
            )

            Spacer(
                modifier = Modifier.width(14.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(
                    modifier = Modifier.height(2.dp),
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector =
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// =============================================================================
// APP HEADER
// =============================================================================

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Icon(
                painter =
                    painterResource(
                        R.drawable.ic_launcher_monochrome,
                    ),
                contentDescription =
                    stringResource(
                        R.string.app_icon_description,
                    ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Text(
            text = stringResource(
                R.string.app_name,
            ),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                top = 16.dp,
            ),
        )

        Text(
            text = stringResource(
                R.string.app_tagline,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                top = 4.dp,
            ),
        )
    }
}

// =============================================================================
// THEME
// =============================================================================

@Composable
private fun ThemeCard(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.profile_theme,
                ),
                style = MaterialTheme.typography.labelLarge,
            )

            ExpressiveSegmentedRow(
                options = AppTheme.entries.map {
                    stringResource(
                        it.labelRes,
                    )
                },
                selectedIndex = selected.ordinal,
                onSelect = {
                    onSelect(
                        AppTheme.entries[it],
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// =============================================================================
// VERSION
// =============================================================================

@Composable
private fun VersionCard(
    versionName: String,
    onClick: () -> Unit,
) {
    val number =
        versionName.substringBefore('-')

    val preRelease =
        versionName.contains('-')

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(
                        R.string.profile_version,
                    ),
                    style =
                        MaterialTheme.typography.labelLarge,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Text(
                        text = number,
                        style =
                            MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.primary,
                    )

                    if (preRelease) {
                        Spacer(
                            modifier = Modifier.width(12.dp),
                        )

                        Surface(
                            shape =
                                RoundedCornerShape(12.dp),
                            color =
                                MaterialTheme.colorScheme.primary,
                            contentColor =
                                MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.version_beta,
                                ),
                                style =
                                    MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp,
                                ),
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(
                        R.string.profile_version_subtitle,
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector =
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// =============================================================================
// PERMISSION DETAILS
// =============================================================================

@Composable
private fun PermissionDetailsCard(
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )

            Spacer(
                modifier = Modifier.width(20.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(
                        R.string.profile_permissions_title,
                    ),
                    style =
                        MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = stringResource(
                        R.string.profile_permissions_subtitle,
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector =
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// =============================================================================
// EXPORT / IMPORT
// =============================================================================

@Composable
private fun ExportSettingsCard(
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            Text(
                text = stringResource(
                    R.string.profile_export_title,
                ),
                style =
                    MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(
                    R.string.profile_export_description,
                ),
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.padding(
                    top = 8.dp,
                ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
            ) {

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onExportSettings,
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.Upload,
                        contentDescription = null,
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp),
                    )

                    Text(
                        text = stringResource(
                            R.string.profile_export_export,
                        ),
                    )
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onImportSettings,
                ) {
                    Icon(
                        imageVector =
                            Icons.Rounded.Download,
                        contentDescription = null,
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp),
                    )

                    Text(
                        text = stringResource(
                            R.string.profile_export_import,
                        ),
                    )
                }
            }
        }
    }
}

// =============================================================================
// GITHUB
// =============================================================================

@Composable
private fun GitHubCard(
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Icon(
                painter =
                    painterResource(
                        R.drawable.ic_github,
                    ),
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )

            Spacer(
                modifier = Modifier.width(20.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(
                        R.string.profile_github_title,
                    ),
                    style =
                        MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = stringResource(
                        R.string.profile_github_subtitle,
                    ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector =
                    Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// =============================================================================
// DEVELOPER
// =============================================================================

@Composable
private fun DevCard(
    onOpenGitHub: () -> Unit,
    onOpenCoffee: () -> Unit,
    onOpenLinkedIn: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {

            // -------------------------------------------------------------
            // Developer profile
            // -------------------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DevAvatar()

                Spacer(
                    modifier = Modifier.width(16.dp),
                )

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(
                            R.string.dev_card_author,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp),
                    )

                    Text(
                        text = "Developer of Expressive Island",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // -------------------------------------------------------------
            // Social / support actions
            // -------------------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                DeveloperAction(
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_github,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    label = stringResource(
                        R.string.dev_card_github,
                    ),
                    onClick = onOpenGitHub,
                )

                DeveloperAction(
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            painter = painterResource(
                                R.drawable.ic_linkedin,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                    },
                    label = stringResource(
                        R.string.dev_card_linkedin,
                    ),
                    onClick = onOpenLinkedIn,
                )
            }
        }
    }
}

@Composable
private fun DeveloperAction(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable(
                onClick = onClick,
            ),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()

            Spacer(
                modifier = Modifier.width(7.dp),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

// =============================================================================
// DEVELOPER AVATAR
// =============================================================================

@Composable
private fun DevAvatar() {
    Image(
        painter =
            painterResource(
                R.drawable.dev_avatar,
            ),
        contentDescription =
            stringResource(
                R.string.dev_card_avatar,
            ),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(112.dp)
            .clip(
                RoundedCornerShape(28.dp),
            ),
    )
}