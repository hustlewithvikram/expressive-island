package com.vikram.expressiveisland.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Lists every launchable app on the device with a switch per app. Turning an app off mutes it on
 * the cutout entirely: neither its notifications nor its media reach the island. Enabled is the
 * default, so only the opt-outs are stored (see [com.vikram.expressiveisland.data.AppPreferences]).
 *
 * The list is read off the main thread once; each row loads its own launcher icon lazily, so
 * scrolling a few hundred apps never holds every icon bitmap in memory at once.
 */
@Composable
internal fun AppsScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val disabled by viewModel.disabledApps.collectAsStateWithLifecycle()
    val normalOnly by viewModel.normalOnlyApps.collectAsStateWithLifecycle()
    val apps by produceState<List<InstalledApp>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    var query by rememberSaveable { mutableStateOf("") }
    // Only one row's options are open at a time, so the list never turns into a wall of controls.
    var expandedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }
    var sortOrder by rememberSaveable { mutableStateOf(AppSort.A_TO_Z) }
    var sortDialogVisible by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val loaded = apps
    if (loaded == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val filtered = remember(loaded, query, filter, sortOrder, disabled, normalOnly) {
        val needle = query.trim()

        val matching = loaded.filter { app ->
            val matchesQuery = needle.isEmpty() ||
                    app.label.contains(needle, ignoreCase = true) ||
                    app.packageName.contains(needle, ignoreCase = true)

            val isEnabled = app.packageName !in disabled
            val isNormalOnly = app.packageName in normalOnly

            val matchesFilter = when (filter) {
                AppFilter.ALL -> true
                AppFilter.ENABLED -> isEnabled
                AppFilter.DISABLED -> !isEnabled
                AppFilter.NORMAL_ONLY -> isNormalOnly
            }

            matchesQuery && matchesFilter
        }

        when (sortOrder) {
            AppSort.A_TO_Z -> matching.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            AppSort.Z_TO_A -> matching.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.label })
        }
    }
    val lastIndex = filtered.lastIndex

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            focusManager.clearFocus()
                        }
                    },
            ) {
                CardSectionHeader(
                    text = "Apps",
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Text(
                    text = stringResource(R.string.apps_screen_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(percent = 50),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { query = "" }
                                        .padding(3.dp),
                                )
                            }
                        },
                        placeholder = {
                            Text(stringResource(R.string.apps_search_hint))
                        },
                    )

                    IconButton(
                        onClick = { sortDialogVisible = true },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SortByAlpha,
                            contentDescription = "Sort apps",
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(
                        items = AppFilter.entries.toList(),
                        key = { it.name },
                    ) { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) },
                            shape = CircleShape,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (query.isNotBlank() || filter != AppFilter.ALL) {
                        "${filtered.size} of ${loaded.size} apps"
                    } else {
                        "${loaded.size} apps"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Spacer(Modifier.height(8.dp))
            }
        }

        if (filtered.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.apps_none_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        }

        itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                app = app,
                shape = appGroupShape(index = index, lastIndex = lastIndex),
                enabled = app.packageName !in disabled,
                normalOnly = app.packageName in normalOnly,
                expanded = expandedPackage == app.packageName,
                onExpandToggle = {
                    expandedPackage =
                        if (expandedPackage == app.packageName) null else app.packageName
                },
                onEnabledChange = { viewModel.setAppEnabled(app.packageName, it) },
                onNormalOnlyChange = { viewModel.setAppNormalOnly(app.packageName, it) },
            )
        }
    }

    if (sortDialogVisible) {
        AlertDialog(
            onDismissRequest = { sortDialogVisible = false },
            title = { Text("Sort apps") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AppSort.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    sortOrder = option
                                    sortDialogVisible = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )

                            if (sortOrder == option) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

private enum class AppFilter(val label: String) {
    ALL("All"),
    ENABLED("Enabled"),
    DISABLED("Disabled"),
    NORMAL_ONLY("Normal only")
}

private enum class AppSort(val label: String) {
    A_TO_Z("A–Z"),
    Z_TO_A("Z–A"),
}

// Grouped-list corners: the group's outer corners (first top, last bottom) are 32dp, rest 4dp.
// Hoisted to constants rather than built per item — with a few hundred rows the allocation churn
// during a fling is pure waste.
private val GroupShapeFirst =
    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val GroupShapeMiddle = RoundedCornerShape(4.dp)
private val GroupShapeLast =
    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 32.dp, bottomEnd = 32.dp)
private val GroupShapeOnly = RoundedCornerShape(32.dp)

private fun appGroupShape(index: Int, lastIndex: Int): Shape = when {
    index == 0 && index == lastIndex -> GroupShapeOnly
    index == 0 -> GroupShapeFirst
    index == lastIndex -> GroupShapeLast
    else -> GroupShapeMiddle
}

/**
 * One app: identity and the allow switch on the collapsed row, with the per-app options revealed
 * underneath when the row is tapped. The chevron rotates to advertise that there is more here —
 * without it the extra settings would be invisible.
 */
@Composable
private fun AppCard(
    app: InstalledApp,
    shape: Shape,
    enabled: Boolean,
    normalOnly: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onNormalOnlyChange: (Boolean) -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "appChevron",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(packageName = app.packageName)

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(3.dp))

                    AppStatusBadge(
                        enabled = enabled,
                        normalOnly = normalOnly,
                    )
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse options" else "Expand options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(chevronRotation),
                )

                Spacer(Modifier.width(8.dp))

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 16.dp,
                                top = 12.dp,
                                bottom = 12.dp,
                                end = 16.dp,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.apps_normal_only_title),
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Text(
                                text = stringResource(R.string.apps_normal_only_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Switch(
                            checked = normalOnly,
                            onCheckedChange = onNormalOnlyChange,
                            enabled = enabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppStatusBadge(
    enabled: Boolean,
    normalOnly: Boolean,
) {
    val label = when {
        !enabled -> "Disabled"
        normalOnly -> "Normal only"
        else -> "Enabled"
    }

    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        normalOnly -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        normalOnly -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

/**
 * The app's launcher icon, served from [iconCache] when we already have it and rasterised off the
 * main thread otherwise. A neutral placeholder stands in while it loads, or for good if the icon
 * can't be read.
 */
@Composable
internal fun AppIcon(packageName: String) {
    val context = LocalContext.current
    // Seeding from the cache means a row scrolled back into view paints its icon on the very first
    // frame — no null pass, no second composition, no package-manager round trip.
    val icon by produceState(initialValue = iconCache.get(packageName), packageName) {
        if (value != null) return@produceState
        val loaded = withContext(Dispatchers.IO) {
            iconLoadLimit.withPermit { loadAppIcon(context, packageName) }
        }
        if (loaded != null) iconCache.put(packageName, loaded)
        value = loaded
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** An installed app the user can allow or mute. Icons are loaded per row, not held here. */
internal data class InstalledApp(
    val packageName: String,
    val label: String,
)

/**
 * Every app with a launcher entry, minus this one, sorted by name. Resolving the launcher intent
 * (rather than asking for every installed package) keeps the list to apps the user recognises and
 * avoids the restricted QUERY_ALL_PACKAGES permission — the manifest's <queries> element is what
 * makes these visible on Android 11+.
 */
internal fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }.getOrDefault(emptyList())

    return resolved.asSequence()
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
}

/** The app's launcher icon at list size, or null if the package went away while we scrolled. */
private fun loadAppIcon(context: Context, packageName: String): ImageBitmap? = runCatching {
    context.packageManager.getApplicationIcon(packageName).toListIconBitmap()
}.getOrNull()

/**
 * Rasterises a launcher icon at a fixed small size. Adaptive icons report an intrinsic size well
 * above what a 44dp badge needs, so drawing them at their natural size would waste several
 * megabytes across a long list.
 */
private fun Drawable.toListIconBitmap(): ImageBitmap {
    val bitmap = createBitmap(ICON_PX, ICON_PX)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, ICON_PX, ICON_PX)
    draw(canvas)
    return bitmap.asImageBitmap()
}

private const val ICON_PX = 96

/**
 * Decoded launcher icons, held across scrolls and across visits to the screen. Without this every
 * row that came back into view re-hit the package manager and re-rasterised its adaptive icon,
 * which is what made a fling stutter. At 96px an entry is ~36 KB, so the cap is a few MB.
 */
private val iconCache = LruCache<String, ImageBitmap>(128)

/**
 * Caps how many icons decode at once. A fast fling composes rows faster than they load, and
 * Dispatchers.IO would happily run dozens of package-manager binder calls in parallel — which
 * contend with each other and starve the main thread instead of finishing sooner.
 */
private val iconLoadLimit = Semaphore(4)