package com.vikram.expressiveisland.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.IslandPreviewBus
import com.vikram.expressiveisland.data.IslandDimensions
import com.vikram.expressiveisland.data.IslandLayout
import com.vikram.expressiveisland.permissions.Permissions
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.components.ExpressiveSegmentedRow
import kotlin.math.roundToInt

@Composable
internal fun SizePositionScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val layout by viewModel.layout.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    /*
     * The real overlay is pinned while this screen is visible so changes can be
     * previewed directly on the device cutout.
     *
     * The overlay is made non-touchable while pinned by IslandOverlayController.
     * This means a large expanded preview can never steal interaction from this
     * editor.
     */
    DisposableEffect(lifecycleOwner) {
        fun refresh() {
            IslandPreviewBus.setActive(
                Permissions.isAccessibilityGranted(context)
            )
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> refresh()
                Lifecycle.Event.ON_PAUSE -> {
                    IslandPreviewBus.setActive(false)
                }

                else -> Unit
            }
        }

        refresh()
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            IslandPreviewBus.setActive(false)
            IslandPreviewBus.setExpandedPreview(false)
        }
    }

    LaunchedEffect(tab) {
        IslandPreviewBus.setExpandedPreview(tab == 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CardSectionHeader(
            text = "Size & Position",
            padding = PaddingValues(
                start = 6.dp,
            )
        )

        /*
         * Mode selector
         */
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Edit layout",
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = if (tab == 0) {
                        "Adjust the normal pill appearance."
                    } else {
                        "Adjust the expanded pill appearance."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ExpressiveSegmentedRow(
                    options = listOf(
                        stringResource(R.string.tab_normal),
                        stringResource(R.string.tab_expanded),
                    ),
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        /*
         * Active editor
         */
        when (tab) {
            0 -> DimensionsEditor(
                dimensions = layout.collapsed,
                defaults = IslandLayout.DEFAULT_COLLAPSED,
                expandedPreview = false,
                onChange = viewModel::setCollapsedDimensions,
            )

            else -> DimensionsEditor(
                dimensions = layout.expanded,
                defaults = IslandLayout.DEFAULT_EXPANDED,
                expandedPreview = true,
                onChange = viewModel::setExpandedDimensions,
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DimensionsEditor(
    dimensions: IslandDimensions,
    defaults: IslandDimensions,
    expandedPreview: Boolean,
    onChange: (IslandDimensions) -> Unit,
) {
    var width by remember(dimensions.widthPercent) {
        mutableStateOf(dimensions.widthPercent.toFloat())
    }

    var height by remember(dimensions.heightDp) {
        mutableStateOf(dimensions.heightDp.toFloat())
    }

    var offsetX by remember(dimensions.offsetXDp) {
        mutableStateOf(dimensions.offsetXDp.toFloat())
    }

    var offsetY by remember(dimensions.offsetYDp) {
        mutableStateOf(dimensions.offsetYDp.toFloat())
    }

    var cornerTl by remember(dimensions.cornerTopLeftDp) {
        mutableStateOf(dimensions.cornerTopLeftDp.toFloat())
    }

    var cornerTr by remember(dimensions.cornerTopRightDp) {
        mutableStateOf(dimensions.cornerTopRightDp.toFloat())
    }

    var cornerBl by remember(dimensions.cornerBottomLeftDp) {
        mutableStateOf(dimensions.cornerBottomLeftDp.toFloat())
    }

    var cornerBr by remember(dimensions.cornerBottomRightDp) {
        mutableStateOf(dimensions.cornerBottomRightDp.toFloat())
    }

    var cornerMode by remember(dimensions) {
        mutableStateOf(cornerModeFor(dimensions))
    }

    fun commit() {
        onChange(
            IslandDimensions.of(
                widthPercent = width.roundToInt(),
                heightDp = height.roundToInt(),
                offsetXDp = offsetX.roundToInt(),
                offsetYDp = offsetY.roundToInt(),
                cornerTopLeftDp = cornerTl.roundToInt(),
                cornerTopRightDp = cornerTr.roundToInt(),
                cornerBottomLeftDp = cornerBl.roundToInt(),
                cornerBottomRightDp = cornerBr.roundToInt(),
            )
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        /*
         * Size
         */
        EditorSection(
            title = "Size",
            description = if (expandedPreview) {
                "Control the dimensions of the expanded island."
            } else {
                "Control the dimensions of the normal island."
            },
        ) {
            AdjustableSlider(
                label = stringResource(R.string.appearance_width),
                valueText = "${width.roundToInt()}%",
                value = width,
                valueRange =
                    IslandDimensions.MIN_WIDTH_PERCENT.toFloat()..
                            IslandDimensions.MAX_WIDTH_PERCENT.toFloat(),
                step = 1f,
                onValueChange = { width = it },
                onCommit = { commit() },
            )

            AdjustableSlider(
                label = stringResource(R.string.appearance_height),
                valueText = "${height.roundToInt()} dp",
                value = height,
                valueRange =
                    IslandDimensions.MIN_HEIGHT_DP.toFloat()..
                            IslandDimensions.MAX_HEIGHT_DP.toFloat(),
                step = 2f,
                onValueChange = { height = it },
                onCommit = { commit() },
            )
        }

        /*
         * Corners
         */
        EditorSection(
            title = "Corners",
            description = "Customize each corner independently or as groups.",
        ) {
            CornerRadiusControls(
                cornerTl = cornerTl,
                cornerTr = cornerTr,
                cornerBl = cornerBl,
                cornerBr = cornerBr,
                mode = cornerMode,
                onModeChange = { cornerMode = it },
                onTlChange = { cornerTl = it },
                onTrChange = { cornerTr = it },
                onBlChange = { cornerBl = it },
                onBrChange = { cornerBr = it },
                onCommit = { commit() },
            )
        }

        /*
         * Position
         */
        EditorSection(
            title = "Position",
            description = "Fine-tune where the island sits on the screen.",
        ) {
            AdjustableSlider(
                label = stringResource(R.string.appearance_vertical),
                valueText = "${offsetY.roundToInt()} dp",
                value = offsetY,
                valueRange =
                    IslandDimensions.MIN_OFFSET_Y_DP.toFloat()..
                            IslandDimensions.MAX_OFFSET_Y_DP.toFloat(),
                step = 2f,
                onValueChange = { offsetY = it },
                onCommit = { commit() },
            )

            AdjustableSlider(
                label = stringResource(R.string.appearance_horizontal),
                valueText = "${offsetX.roundToInt()} dp",
                value = offsetX,
                valueRange =
                    IslandDimensions.MIN_OFFSET_X_DP.toFloat()..
                            IslandDimensions.MAX_OFFSET_X_DP.toFloat(),
                step = 2f,
                onValueChange = { offsetX = it },
                onCommit = { commit() },
            )
        }

        /*
         * Reset
         */
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Reset layout",
                    style = MaterialTheme.typography.titleSmall,
                )

                Text(
                    text = "Restore all dimensions and corner values to their defaults.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = {
                        width = defaults.widthPercent.toFloat()
                        height = defaults.heightDp.toFloat()
                        offsetX = defaults.offsetXDp.toFloat()
                        offsetY = defaults.offsetYDp.toFloat()
                        cornerTl = defaults.cornerTopLeftDp.toFloat()
                        cornerTr = defaults.cornerTopRightDp.toFloat()
                        cornerBl = defaults.cornerBottomLeftDp.toFloat()
                        cornerBr = defaults.cornerBottomRightDp.toFloat()

                        cornerMode = cornerModeFor(defaults)

                        onChange(defaults)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.action_reset_layout),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            content()
        }
    }
}

private enum class CornerMode {
    All,
    TopBottom,
    Each,
}

/**
 * The narrowest [CornerMode] that can represent [d]'s radii.
 */
private fun cornerModeFor(
    d: IslandDimensions,
): CornerMode = when {
    d.cornerTopLeftDp == d.cornerTopRightDp &&
            d.cornerBottomLeftDp == d.cornerBottomRightDp &&
            d.cornerTopLeftDp == d.cornerBottomLeftDp -> {
        CornerMode.All
    }

    d.cornerTopLeftDp == d.cornerTopRightDp &&
            d.cornerBottomLeftDp == d.cornerBottomRightDp -> {
        CornerMode.TopBottom
    }

    else -> CornerMode.Each
}

@Composable
private fun CornerRadiusControls(
    cornerTl: Float,
    cornerTr: Float,
    cornerBl: Float,
    cornerBr: Float,
    mode: CornerMode,
    onModeChange: (CornerMode) -> Unit,
    onTlChange: (Float) -> Unit,
    onTrChange: (Float) -> Unit,
    onBlChange: (Float) -> Unit,
    onBrChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    val range =
        IslandDimensions.MIN_CORNER_DP.toFloat()..
                IslandDimensions.MAX_CORNER_DP.toFloat()

    val modes = CornerMode.entries

    fun onCornerChanged(
        callback: (Float) -> Unit,
        newCorner: Float,
    ) {
        if (newCorner >= 1f) {
            callback(newCorner)
        }
    }

    fun onAllChanged(newCorner: Float) {
        if (newCorner >= 1f) {
            onTlChange(newCorner)
            onTrChange(newCorner)
            onBlChange(newCorner)
            onBrChange(newCorner)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExpressiveSegmentedRow(
            options = modes.map { cornerMode ->
                when (cornerMode) {
                    CornerMode.All ->
                        stringResource(R.string.corner_mode_all)

                    CornerMode.TopBottom ->
                        stringResource(R.string.corner_mode_split)

                    CornerMode.Each ->
                        stringResource(R.string.corner_mode_each)
                }
            },
            selectedIndex = mode.ordinal,
            onSelect = { onModeChange(modes[it]) },
            modifier = Modifier.fillMaxWidth(),
        )

        when (mode) {
            CornerMode.All -> {
                CornerSlider(
                    label = stringResource(R.string.appearance_corner_all),
                    value = cornerTl,
                    range = range,
                    onValueChange = { onAllChanged(it) },
                    onCommit = onCommit,
                )
            }

            CornerMode.TopBottom -> {
                CornerSlider(
                    label = stringResource(R.string.appearance_corner_top),
                    value = cornerTl,
                    range = range,
                    onValueChange = {
                        onCornerChanged(onTlChange, it)
                        onCornerChanged(onTrChange, it)
                    },
                    onCommit = onCommit,
                )

                CornerSlider(
                    label = stringResource(R.string.appearance_corner_bottom),
                    value = cornerBl,
                    range = range,
                    onValueChange = {
                        onCornerChanged(onBlChange, it)
                        onCornerChanged(onBrChange, it)
                    },
                    onCommit = onCommit,
                )
            }

            CornerMode.Each -> {
                CornerSlider(
                    label = stringResource(R.string.appearance_corner_tl),
                    value = cornerTl,
                    range = range,
                    onValueChange = {
                        onCornerChanged(onTlChange, it)
                    },
                    onCommit = onCommit,
                )

                CornerSlider(
                    label = stringResource(R.string.appearance_corner_tr),
                    value = cornerTr,
                    range = range,
                    onValueChange = {
                        onCornerChanged(onTrChange, it)
                    },
                    onCommit = onCommit,
                )

                CornerSlider(
                    label = stringResource(R.string.appearance_corner_bl),
                    value = cornerBl,
                    range = range,
                    onValueChange = {
                        onCornerChanged(onBlChange, it)
                    },
                    onCommit = onCommit,
                )

                CornerSlider(
                    label = stringResource(R.string.appearance_corner_br),
                    value = cornerBr,
                    range = range,
                    onValueChange = {
                        onCornerChanged(onBrChange, it)
                    },
                    onCommit = onCommit,
                )
            }
        }
    }
}

@Composable
private fun CornerSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onCommit: () -> Unit,
) {
    AdjustableSlider(
        label = label,
        valueText = "${value.roundToInt()} dp",
        value = value,
        valueRange = range,
        step = 1f,
        onValueChange = onValueChange,
        onCommit = onCommit,
    )
}