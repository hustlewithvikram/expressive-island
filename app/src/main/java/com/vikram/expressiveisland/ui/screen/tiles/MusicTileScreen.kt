package com.vikram.expressiveisland.ui.screen.tiles

import android.R.attr.checked
import android.R.attr.description
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.MusicButtonStyle
import com.vikram.expressiveisland.overlay.resolve
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.screen.AdjustableSlider
import com.vikram.expressiveisland.ui.screen.CardSectionHeader
import com.vikram.expressiveisland.ui.screen.ColorPickerCard
import com.vikram.expressiveisland.ui.screen.SettingsToggleCard
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private val MusicAccent = Color(0xFFF472B6)

private val MusicButtonFilledDefault = Color(0xFFE0E0E0)

private const val PREVIEW_BUTTON_HEIGHT_DP = 48

/**
 * Main outer radius used by the settings cards.
 */
private val GROUP_CORNER_RADIUS = 24.dp

/**
 * Small radius used on the inner corners of separated cards.
 *
 * Cards in a group have a small visual gap, so completely square inner
 * corners would look too harsh.
 */
private val GROUP_INNER_RADIUS = 6.dp

/**
 * Space between cards that belong to the same logical group.
 */
private val GROUP_CARD_SPACING = 4.dp

/**
 * Extra padding used around reset actions.
 */
private val RESET_HORIZONTAL_PADDING = 14.dp
private val RESET_VERTICAL_PADDING = 8.dp

/**
 * Music tile settings UI.
 *
 * This file intentionally contains UI refactoring only.
 * Existing state and ViewModel callbacks are preserved.
 */
@Composable
internal fun MusicTileScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.musicTile.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // =====================================================================
        // MUSIC TILE
        // =====================================================================

        CardSectionHeader(
            text = "Music Tile",
            padding = PaddingValues(
                start = 6.dp,
                top = 4.dp,
                bottom = 0.dp,
            ),
        )

        // =====================================================================
        // ALBUM ART
        // =====================================================================

        SectionLabel("Album Art")

        SettingsGroup {

            SettingsToggleCard(
                shape = groupShape(
                    if (settings.showAlbumArt) {
                        GroupPosition.FIRST
                    } else {
                        GroupPosition.ONLY
                    },
                ),
                title = stringResource(R.string.music_show_art_title),
                description = stringResource(R.string.music_show_art_desc),
                checked = settings.showAlbumArt,
                onCheckedChange = viewModel::setMusicShowAlbumArt,
            )

            AnimatedVisibility(
                visible = settings.showAlbumArt,
            ) {
                SettingsGroup {

                    /*
                     * Spin album art.
                     *
                     * It is the first child after the main album-art toggle,
                     * therefore it gets the top corners for its own separated
                     * card but no artificial bottom-heavy rounding.
                     */
                    SettingsToggleCard(
                        shape = groupShape(GroupPosition.MIDDLE),
                        title = stringResource(R.string.music_rotate_art_title),
                        description = stringResource(R.string.music_rotate_art_desc),
                        checked = settings.rotateAlbumArt,
                        onCheckedChange = viewModel::setMusicRotateAlbumArt,
                    )

                    SettingsToggleCard(
                        shape = groupShape(
                            if (settings.albumArtStroke) {
                                GroupPosition.MIDDLE
                            } else {
                                GroupPosition.LAST
                            },
                        ),
                        title = stringResource(R.string.music_art_stroke_title),
                        description = stringResource(R.string.music_art_stroke_desc),
                        checked = settings.albumArtStroke,
                        onCheckedChange = viewModel::setMusicAlbumArtStroke,
                    )

                    AnimatedVisibility(
                        visible = settings.albumArtStroke,
                    ) {
                        ColorPickerCard(
                            label = stringResource(R.string.music_art_stroke_color),
                            selected = settings.albumArtStrokeColor,
                            onSelect = viewModel::setMusicAlbumArtStrokeColor,
                            defaultLabel = stringResource(R.string.music_default_accent),
                            defaultColor = MusicAccent,
                            shape = groupShape(GroupPosition.LAST),
                        )
                    }
                }
            }
        }

        // =====================================================================
        // ALBUM COVER BACKGROUND
        // =====================================================================

        SectionLabel("Album Cover Background")

        SettingsGroup {

            SettingsToggleCard(
                shape = groupShape(
                    if (settings.expandedBackground) {
                        GroupPosition.FIRST
                    } else {
                        GroupPosition.ONLY
                    },
                ),
                title = stringResource(
                    R.string.music_expanded_background_title,
                ),
                description = stringResource(
                    R.string.music_expanded_background_desc,
                ),
                checked = settings.expandedBackground,
                onCheckedChange = viewModel::setMusicExpandedBackground,
            )

            AnimatedVisibility(
                visible = settings.expandedBackground,
            ) {
                AlbumBackgroundBlurCard(
                    blur = settings.expandedBackgroundBlur,
                    shape = groupShape(GroupPosition.LAST),
                    onCommit = viewModel::setMusicExpandedBackgroundBlur,
                )
            }
        }

        // =====================================================================
        // BEHAVIOR
        // =====================================================================

        SectionLabel("Behavior")

        SettingsGroup {

            SettingsToggleCard(
                shape = groupShape(GroupPosition.FIRST),
                title = stringResource(
                    R.string.music_expand_on_play_title,
                ),
                description = stringResource(
                    R.string.music_expand_on_play_desc,
                ),
                checked = settings.expandOnPlay,
                onCheckedChange = viewModel::setMusicExpandOnPlay,
            )

            SettingsToggleCard(
                shape = groupShape(GroupPosition.MIDDLE),
                title = stringResource(
                    R.string.music_visible_in_player_title,
                ),
                description = stringResource(
                    R.string.music_visible_in_player_desc,
                ),
                checked = settings.visibleInPlayerApp,
                onCheckedChange = viewModel::setMusicVisibleInPlayerApp,
            )

            SettingsToggleCard(
                shape = groupShape(GroupPosition.LAST),
                title = stringResource(
                    R.string.music_show_controls_title,
                ),
                description = stringResource(
                    R.string.music_show_controls_desc,
                ),
                checked = settings.showControls,
                onCheckedChange = viewModel::setMusicShowControls,
            )
        }

        // =====================================================================
        // PLAYBACK CONTROLS
        // =====================================================================

        AnimatedVisibility(
            visible = settings.showControls,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                SectionLabel(
                    stringResource(R.string.music_buttons_title),
                )

                MusicButtonsPreview(
                    skipStyle = settings.skipButton,
                    playPauseStyle = settings.playPauseButton,
                )

                // -------------------------------------------------------------
                // SKIP BUTTONS
                // -------------------------------------------------------------

                SectionLabel(
                    stringResource(R.string.music_skip_buttons_title),
                )

                SettingsGroup {

                    ButtonPresetRow(
                        current = settings.skipButton,
                        sampleFill = settings.skipButton.previewFill(
                            fallback = null,
                        ) ?: MusicButtonFilledDefault,
                        onApply = viewModel::applyMusicSkipPreset,
                    )

                    ColorPickerCard(
                        label = stringResource(
                            R.string.music_button_color,
                        ),
                        selected = settings.skipButton.color,
                        onSelect = viewModel::setMusicSkipColor,
                        defaultLabel = stringResource(
                            R.string.cd_color_default_plain,
                        ),
                        defaultColor = MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                        shape = groupShape(GroupPosition.MIDDLE),
                    )

                    ButtonShapeCard(
                        style = settings.skipButton,
                        shape = groupShape(GroupPosition.LAST),
                        onOpacityCommit = viewModel::setMusicSkipOpacity,
                        onCornerCommit = viewModel::setMusicSkipCornerPercent,
                    )
                }

                // -------------------------------------------------------------
                // PLAY / PAUSE
                // -------------------------------------------------------------

                SectionLabel(
                    stringResource(
                        R.string.music_playpause_button_title,
                    ),
                )

                SettingsGroup {

                    ButtonPresetRow(
                        current = settings.playPauseButton,
                        sampleFill = settings.playPauseButton.previewFill(
                            fallback = MusicAccent,
                        ) ?: MusicAccent,
                        onApply = viewModel::applyMusicPlayPausePreset,
                    )

                    ColorPickerCard(
                        label = stringResource(
                            R.string.music_button_color,
                        ),
                        selected = settings.playPauseButton.color,
                        onSelect = viewModel::setMusicPlayPauseColor,
                        defaultLabel = stringResource(
                            R.string.music_default_accent,
                        ),
                        defaultColor = MusicAccent,
                        shape = groupShape(GroupPosition.MIDDLE),
                    )

                    ButtonShapeCard(
                        style = settings.playPauseButton,
                        shape = groupShape(GroupPosition.LAST),
                        onOpacityCommit = viewModel::setMusicPlayPauseOpacity,
                        onCornerCommit = viewModel::setMusicPlayPauseCornerPercent,
                    )
                }
            }
        }

        // =====================================================================
        // PLAYBACK DISPLAY
        // =====================================================================

        SectionLabel("Playback Display")

        SettingsGroup {

            SettingsToggleCard(
                shape = groupShape(GroupPosition.ONLY),
                title = stringResource(
                    R.string.music_progress_title,
                ),
                description = stringResource(
                    R.string.music_progress_description,
                ),
                checked = settings.showProgress,
                onCheckedChange = viewModel::setMusicShowProgress,
            )

            /*
             * Keep Expanded Background here as well as in the dedicated
             * Album Cover Background section? No.
             *
             * The actual setting belongs to Album Cover Background.
             * Therefore this section only contains the progress setting.
             */
        }
    }
}

// =============================================================================
// GROUPED CARD SYSTEM
// =============================================================================

private enum class GroupPosition {
    ONLY,
    FIRST,
    MIDDLE,
    LAST,
}

/**
 * Creates the shape for a card inside a visually grouped settings section.
 *
 * Unlike the previous implementation, every card retains a small amount of
 * rounding because there is a 4.dp gap between cards.
 */
private fun groupShape(
    position: GroupPosition,
): RoundedCornerShape {
    return when (position) {

        GroupPosition.ONLY -> {
            RoundedCornerShape(
                GROUP_CORNER_RADIUS,
            )
        }

        GroupPosition.FIRST -> {
            RoundedCornerShape(
                topStart = GROUP_CORNER_RADIUS,
                topEnd = GROUP_CORNER_RADIUS,
                bottomStart = GROUP_INNER_RADIUS,
                bottomEnd = GROUP_INNER_RADIUS,
            )
        }

        GroupPosition.MIDDLE -> {
            RoundedCornerShape(
                GROUP_INNER_RADIUS,
            )
        }

        GroupPosition.LAST -> {
            RoundedCornerShape(
                topStart = GROUP_INNER_RADIUS,
                topEnd = GROUP_INNER_RADIUS,
                bottomStart = GROUP_CORNER_RADIUS,
                bottomEnd = GROUP_CORNER_RADIUS,
            )
        }
    }
}

/**
 * Group container with the small visual separation used by the previous UI.
 */
@Composable
private fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            GROUP_CARD_SPACING,
        ),
        content = content,
    )
}

// =============================================================================
// SECTION LABEL
// =============================================================================

@Composable
private fun SectionLabel(
    text: String,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = 8.dp,
            top = 0.dp,
            bottom = 4.dp,
        ),
    )
}

// =============================================================================
// ALBUM BACKGROUND BLUR
// =============================================================================

@Composable
private fun AlbumBackgroundBlurCard(
    blur: Float,
    shape: RoundedCornerShape,
    onCommit: (Float) -> Unit,
) {
    var localBlur by remember(blur) {
        mutableFloatStateOf(blur)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 18.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(
                12.dp,
            ),
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(
                            R.string.music_background_blur,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = "Adjust the blur applied to the album artwork background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = 3.dp,
                        ),
                    )
                }

                Text(
                    text = "${localBlur.roundToInt()} dp",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            AdjustableSlider(
                label = "",
                valueText = "",
                value = localBlur,
                valueRange = 0f..40f,
                step = 1f,
                onValueChange = {
                    localBlur = it
                },
                onCommit = {
                    onCommit(localBlur)
                },
            )

            ResetToDefaultButton(
                visible = localBlur != 0f,
                onClick = {
                    localBlur = 0f
                    onCommit(0f)
                },
            )
        }
    }
}

// =============================================================================
// BUTTON PRESETS
// =============================================================================

@Composable
private fun ButtonPresetRow(
    current: MusicButtonStyle,
    sampleFill: Color,
    onApply: (MusicButtonStyle) -> Unit,
) {
    val labels = mapOf(
        MusicButtonStyle.Companion.ROUNDED to stringResource(
            R.string.music_preset_rounded,
        ),
        MusicButtonStyle.Companion.PILL to stringResource(
            R.string.music_preset_pill,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(
            10.dp,
        ),
    ) {

        Text(
            text = "Button shape",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                10.dp,
            ),
        ) {
            MusicButtonStyle.Companion.PRESETS.forEach { preset ->

                val selected =
                    current.filled == preset.filled &&
                            current.cornerPercent == preset.cornerPercent

                PresetChip(
                    label = labels[preset].orEmpty(),
                    fill = sampleFill,
                    cornerPercent = preset.cornerPercent,
                    selected = selected,
                    onClick = {
                        onApply(preset)
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    fill: Color,
    cornerPercent: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(
            onClick = onClick,
        ),
        shape = RoundedCornerShape(18.dp),
        border = if (selected) {
            BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary,
            )
        } else {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            )
        },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                8.dp,
            ),
        ) {

            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(30.dp)
                    .clip(
                        RoundedCornerShape(
                            percent = cornerPercent,
                        ),
                    )
                    .background(fill),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// =============================================================================
// BUTTON SHAPE SETTINGS
// =============================================================================

@Composable
private fun ButtonShapeCard(
    style: MusicButtonStyle,
    shape: RoundedCornerShape,
    onOpacityCommit: (Float) -> Unit,
    onCornerCommit: (Int) -> Unit,
) {
    var opacity by remember(style.opacity) {
        mutableFloatStateOf(style.opacity)
    }

    var corner by remember(style.cornerPercent) {
        mutableFloatStateOf(
            style.cornerPercent.toFloat(),
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(
                14.dp,
            ),
        ) {

            AdjustableSlider(
                label = stringResource(
                    R.string.opacity,
                ),
                valueText = "${(opacity * 100).roundToInt()}%",
                value = opacity,
                valueRange = 0f..1f,
                step = 0.05f,
                onValueChange = {
                    opacity = it
                },
                onCommit = {
                    onOpacityCommit(opacity)
                },
            )

            AdjustableSlider(
                label = stringResource(
                    R.string.music_button_corners,
                ),
                valueText = "${corner.roundToInt()}%",
                value = corner,
                valueRange =
                    MusicButtonStyle.Companion.MIN_CORNER_PERCENT.toFloat()..
                            MusicButtonStyle.Companion.MAX_CORNER_PERCENT.toFloat(),
                step = 5f,
                onValueChange = {
                    corner = it
                },
                onCommit = {
                    onCornerCommit(
                        corner.roundToInt(),
                    )
                },
            )

            /*
             * The reset action is intentionally not wired to guessed defaults.
             *
             * MusicButtonStyle's actual persisted defaults are defined outside
             * this UI file. The existing preset mechanism only controls the
             * preset shape/fill combination and is not guaranteed to represent
             * every persisted default value.
             */
        }
    }
}

// =============================================================================
// MUSIC BUTTON PREVIEW
// =============================================================================

@Composable
private fun MusicButtonsPreview(
    skipStyle: MusicButtonStyle,
    playPauseStyle: MusicButtonStyle,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            GROUP_CORNER_RADIUS,
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme
                .colorScheme
                .surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 18.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(
                14.dp,
            ),
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Live preview",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "Expanded music controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        top = 2.dp,
                    ),
                )
            }

            MusicControlsPreviewSurface(
                skipStyle = skipStyle,
                playPauseStyle = playPauseStyle,
            )
        }
    }
}

@Composable
private fun MusicControlsPreviewSurface(
    skipStyle: MusicButtonStyle,
    playPauseStyle: MusicButtonStyle,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(22.dp),
            )
            .background(
                Color(0xFF18181B),
            )
            .padding(
                horizontal = 14.dp,
                vertical = 14.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            PreviewButton(
                icon = Icons.Rounded.SkipPrevious,
                fill = skipStyle.previewFill(
                    fallback = null,
                ),
                cornerPercent = skipStyle.cornerPercent,
                modifier = Modifier.weight(1f),
            )

            PreviewButton(
                icon = Icons.Rounded.PlayArrow,
                fill = playPauseStyle.previewFill(
                    fallback = MusicAccent,
                ),
                cornerPercent = playPauseStyle.cornerPercent,
                widthDp = PREVIEW_BUTTON_HEIGHT_DP * 16 / 9,
            )

            PreviewButton(
                icon = Icons.Rounded.SkipNext,
                fill = skipStyle.previewFill(
                    fallback = null,
                ),
                cornerPercent = skipStyle.cornerPercent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MusicButtonStyle.previewFill(
    fallback: Color?,
): Color? {
    val base =
        color?.resolve()
            ?: fallback
            ?: if (filled) {
                MusicButtonFilledDefault
            } else {
                return null
            }

    return base.copy(
        alpha = opacity,
    )
}

@Composable
private fun PreviewButton(
    icon: ImageVector,
    fill: Color?,
    cornerPercent: Int,
    modifier: Modifier = Modifier,
    widthDp: Int = PREVIEW_BUTTON_HEIGHT_DP,
) {
    var pressed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "previewButtonBounce",
    )

    Box(
        modifier = modifier
            .then(
                if (modifier == Modifier) {
                    Modifier.width(widthDp.dp)
                } else {
                    Modifier
                },
            )
            .height(
                PREVIEW_BUTTON_HEIGHT_DP.dp,
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(
                RoundedCornerShape(
                    (PREVIEW_BUTTON_HEIGHT_DP * cornerPercent / 100f).dp,
                ),
            )
            .background(
                fill ?: Color.White.copy(
                    alpha = 0.08f,
                ),
            )
            .clickable(
                indication = null,
                interactionSource = remember {
                    MutableInteractionSource()
                },
            ) {
                pressed = true
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                fill == null -> {
                    Color.White.copy(
                        alpha = 0.90f,
                    )
                }

                fill.luminance() > 0.5f -> {
                    Color.Black
                }

                else -> {
                    Color.White
                }
            },
            modifier = Modifier.size(
                26.dp,
            ),
        )
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(
                120.milliseconds,
            )
            pressed = false
        }
    }
}

// =============================================================================
// RESET ACTION
// =============================================================================

@Composable
private fun ResetToDefaultButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "Reset to default",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(14.dp),
                    )
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.08f,
                        ),
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember {
                            MutableInteractionSource()
                        },
                        onClick = onClick,
                    )
                    .padding(
                        horizontal = RESET_HORIZONTAL_PADDING,
                        vertical = RESET_VERTICAL_PADDING,
                    ),
            )
        }
    }
}