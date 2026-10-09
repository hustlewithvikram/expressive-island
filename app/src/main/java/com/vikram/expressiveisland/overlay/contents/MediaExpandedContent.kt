package com.vikram.expressiveisland.overlay.contents

import android.R.style.MediaButton
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.core.MediaProgress
import com.vikram.expressiveisland.core.NowPlayingBus
import com.vikram.expressiveisland.data.MusicButtonStyle
import com.vikram.expressiveisland.data.MusicTilePreferences
import com.vikram.expressiveisland.data.MusicProgressStyle
import com.vikram.expressiveisland.data.SeekButtonMode
import com.vikram.expressiveisland.data.MusicTileSettings
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.formatMediaTime
import com.vikram.expressiveisland.overlay.resolve
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

/**
 * The music tile's expanded layout: album art + track/artist, and (when enabled) a row of
 * previous / play‑pause / next controls. Live state — art, the play vs pause icon and the
 * transport handle — is read from [NowPlayingBus] so the controls stay in sync as playback changes.
 */
@Composable
fun MediaExpandedContent(
    event: IslandEvent,
    buttonHeightDp: Int,
    collapsedHeightDp: Int,
) {
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)
    val context = LocalContext.current

    val musicPreferences = remember(context) {
        MusicTilePreferences(context)
    }

    val musicSettings by musicPreferences.settings.collectAsState(
        initial = MusicTileSettings(),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (musicSettings.expandedBackground && albumArt != null) {
            Image(
                bitmap = albumArt,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(musicSettings.expandedBackgroundBlur.dp)
                    .graphicsLayer { alpha = 0.72f },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = collapsedHeightDp.dp)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ACTIONS_ROW_SPACING_DP.dp),
        ) {
            // Weighted so the transport controls keep their height and the track text gives way.
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (albumArt != null) {
                    AlbumArt(
                        bitmap = albumArt,
                        size = 44.dp,
                        rotate = event.media?.rotateAlbumArt == true,
                        playing = nowPlaying?.isPlaying == true,
                        strokeColor = albumArtStrokeFor(event),
                    )
                } else {
                    IconBadge(event = event, badgeSize = 44.dp, iconSize = 26.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.label,
                        color = LocalContentColor.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    event.detail?.let { detail ->
                        Text(
                            text = detail,
                            color = LocalContentColor.current.copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            event.media?.takeIf { it.showProgress }?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                ) {
                    nowPlaying?.progress?.let { progress ->
                        MediaProgressBar(progress = progress, style = musicSettings.progressStyle)
                    }
                }
            }

            event.media?.takeIf { it.showControls }?.let { media ->
                MediaControls(
                    isPlaying = nowPlaying?.isPlaying == true,
                    accent = event.accent,
                    enabled = nowPlaying != null,
                    heightDp = buttonHeightDp,
                    skipStyle = media.skipStyle,
                    playPauseStyle = media.playPauseStyle,
                    showPlayPauseIcon = musicSettings.showPlayPauseIcon,
                    showPlayPauseText = musicSettings.showPlayPauseText,
                    leftSeekEnabled = musicSettings.leftSeekEnabled,
                    rightSeekEnabled = musicSettings.rightSeekEnabled,
                    leftSeekSeconds = musicSettings.leftSeekSeconds,
                    rightSeekSeconds = musicSettings.rightSeekSeconds,
                    onPrevious = { nowPlaying?.transport?.previous() },
                    onSeekBackward = { nowPlaying?.transport?.seekBackward(musicSettings.leftSeekSeconds) },
                    onPlayPause = { nowPlaying?.transport?.playPause() },
                    onSeekForward = { nowPlaying?.transport?.seekForward(musicSettings.rightSeekSeconds) },
                    onNext = { nowPlaying?.transport?.next() },
                )

            }
        }
    }
}

/**
 * The music tile's playback bar. [MediaProgress] is an anchor rather than a live position — the
 * media session only republishes on a real change, never on a tick — so this drives its own clock
 * while playback runs and extrapolates from that anchor. A session that publishes no track length
 * (a live stream) gets the indeterminate bar instead, matching the notification tile's.
 */
@Composable
fun MediaProgressBar(progress: MediaProgress?, style: MusicProgressStyle = MusicProgressStyle.WAVY) {
    val duration = progress?.durationMs ?: return

    var fraction by remember(progress) {
        mutableFloatStateOf(
            progress.fractionAt(SystemClock.elapsedRealtime())
                ?.coerceIn(0f, 1f)
                ?: 0f
        )
    }

    var positionMs by remember(progress) {
        mutableLongStateOf(
            progress.positionAt(SystemClock.elapsedRealtime())
                .coerceIn(0L, duration)
        )
    }

    LaunchedEffect(progress) {
        while (progress.speed > 0f && positionMs < duration) {
            delay(PROGRESS_TICK_MS.milliseconds)

            val now = SystemClock.elapsedRealtime()

            positionMs = progress
                .positionAt(now)
                .coerceIn(0L, duration)

            fraction = progress
                .fractionAt(now)
                ?.coerceIn(0f, 1f)
                ?: fraction
        }
    }

    // Keep the displayed position correct even while paused.
    LaunchedEffect(progress.positionMs, progress.speed) {
        val now = SystemClock.elapsedRealtime()

        positionMs = progress
            .positionAt(now)
            .coerceIn(0L, duration)

        fraction = progress
            .fractionAt(now)
            ?.coerceIn(0f, 1f)
            ?: fraction
    }

    if (style == MusicProgressStyle.CIRCULAR || style == MusicProgressStyle.CIRCULAR_WAVY) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = formatMediaTime(positionMs),
                color = LocalContentColor.current.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                strokeWidth = if (style == MusicProgressStyle.CIRCULAR_WAVY) 2.dp else 3.dp,
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Text(
                text = formatMediaTime(duration),
                color = LocalContentColor.current.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = formatMediaTime(positionMs),
                color = LocalContentColor.current.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            if (style == MusicProgressStyle.LINEAR) {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                )
            } else {
                WavyProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.weight(1f).height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                )
            }
            Text(
                text = formatMediaTime(duration),
                color = LocalContentColor.current.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val amplitude = height * 0.28f
        val wavelength = 32.dp.toPx()
        val centerY = height / 2f

        fun wavePath(endX: Float): Path {
            val path = Path()
            val steps = 120

            for (i in 0..steps) {
                val x = endX * i / steps
                val y = centerY +
                        amplitude * sin(
                    (x / wavelength) * (2f * PI).toFloat()
                )

                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            return path
        }

        // Track
        drawPath(
            path = wavePath(width),
            color = trackColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
            ),
        )

        // Active progress
        val progressWidth = width * progress.coerceIn(0f, 1f)

        if (progressWidth > 0f) {
            drawPath(
                path = wavePath(progressWidth),
                color = color,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}

/**
 * Previous / play‑pause / next. Each button's fill colour, opacity and corner rounding come from the
 * music tile's settings: the skip buttons share [skipStyle] (plain over the pill by default), the
 * centre button uses [playPauseStyle] (the tile accent by default).
 */
@Composable
fun MediaControls(
    isPlaying: Boolean,
    accent: Color,
    enabled: Boolean,
    heightDp: Int,
    skipStyle: MusicButtonStyle,
    playPauseStyle: MusicButtonStyle,
    showPlayPauseIcon: Boolean = true,
    showPlayPauseText: Boolean = true,
    leftSeekEnabled: Boolean = true,
    rightSeekEnabled: Boolean = true,
    leftSeekSeconds: Int = 5,
    rightSeekSeconds: Int = 5,
    onPrevious: () -> Unit,
    onSeekBackward: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
) {
    val showLeft = leftSeekEnabled
    val showRight = rightSeekEnabled
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous track",
            enabled = enabled,
            heightDp = heightDp,
            iconSize = 22.dp,
            fill = skipStyle.resolveFill(fallback = null),
            cornerPercent = skipStyle.cornerPercent,
            onClick = onPrevious,
            weight = 1f,
        )
        if (showLeft) {
            MediaButton(
                icon = Icons.Rounded.FastRewind,
                badgeText = "${leftSeekSeconds}s",
                contentDescription = "Seek backward ${leftSeekSeconds} seconds",
                enabled = enabled,
                heightDp = heightDp,
                iconSize = 20.dp,
                fill = (skipStyle.color?.resolve() ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)).copy(alpha = skipStyle.opacity),
                cornerPercent = skipStyle.cornerPercent,
                onClick = onSeekBackward,
                weight = 0.85f,
            )
        }
        MediaButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            showIcon = showPlayPauseIcon,
            label = if (showPlayPauseText) (if (isPlaying) "Pause" else "Play") else null,
            contentDescription = if (isPlaying) "Pause" else "Play",
            enabled = enabled,
            heightDp = heightDp,
            iconSize = 22.dp,
            fill = playPauseStyle.resolveFill(fallback = accent),
            cornerPercent = playPauseStyle.cornerPercent,
            onClick = onPlayPause,
            weight = 1.25f,
        )
        if (showRight) {
            MediaButton(
                icon = Icons.Rounded.FastForward,
                badgeText = "${rightSeekSeconds}s",
                contentDescription = "Seek forward ${rightSeekSeconds} seconds",
                enabled = enabled,
                heightDp = heightDp,
                iconSize = 20.dp,
                fill = (skipStyle.color?.resolve() ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f)).copy(alpha = skipStyle.opacity),
                cornerPercent = skipStyle.cornerPercent,
                onClick = onSeekForward,
                weight = 0.85f,
            )
        }
        MediaButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next track",
            enabled = enabled,
            heightDp = heightDp,
            iconSize = 22.dp,
            fill = skipStyle.resolveFill(fallback = null),
            cornerPercent = skipStyle.cornerPercent,
            onClick = onNext,
            weight = 1f,
        )
    }
}

/** The concrete fill for a transport button, or null (a plain, unfilled button) when neither the
 *  style nor the [fallback] supplies a colour and the style isn't [filled].
 *  A filled style with no colour falls back to [MusicButtonFilledDefault]. Opacity folds into alpha. */
@Composable
fun MusicButtonStyle.resolveFill(fallback: Color?): Color? {
    val base = color?.resolve() ?: fallback ?: if (filled) MusicButtonFilledDefault else return null
    return base.copy(alpha = opacity)
}

/**
 * A transport button with the shared press "squish". A null [fill] renders a plain (unfilled) button
 * tinted with the content colour; a non-null [fill] renders a filled button whose corners are rounded
 * by [cornerPercent] relative to its height (50 = a pill / stadium, 0 = a square) with an
 * auto-contrasting icon. [widthDp] defaults to [heightDp] (a square); a larger value makes a
 * rectangle — e.g. the 16:9 play/pause button.
 */
@Composable
fun RowScope.MediaButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    heightDp: Int,
    iconSize: Dp,
    fill: Color?,
    cornerPercent: Int,
    onClick: () -> Unit,
    label: String? = null,
    showIcon: Boolean = true,
    badgeText: String? = null,
    widthDp: Int = heightDp,
    maxWidth: Boolean = false,
    weight: Float? = null,
) {
    val interaction = remember { MutableInteractionSource() }

    val modifier = Modifier
        .then(
            when {
                weight != null -> {
                    Modifier
                        .weight(weight)
                        .height(heightDp.dp)
                }

                maxWidth -> {
                    Modifier
                        .weight(1f)
                        .height(heightDp.dp)
                }

                else -> {
                    Modifier.size(
                        width = widthDp.dp,
                        height = heightDp.dp,
                    )
                }
            }
        )
        .pressScale(interaction)

    if (fill == null) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            modifier = modifier,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = LocalContentColor.current,
                modifier = Modifier.size(iconSize),
            )
        }
    } else {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            shape = RoundedCornerShape(
                (heightDp * cornerPercent / 100f).dp
            ),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = fill,
                contentColor = if (fill.luminance() > 0.5f) {
                    PillTextColorDark
                } else {
                    PillTextColor
                },
                disabledContainerColor = LocalContentColor.current.copy(alpha = 0.12f),
                disabledContentColor = LocalContentColor.current.copy(alpha = 0.4f),
            ),
            modifier = modifier,
        ) {
            if (label != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showIcon) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                        )
                    }

                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            } else if (badgeText != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(iconSize + 6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = badgeText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .background(fill, shape = RoundedCornerShape(3.dp))
                            .padding(horizontal = 1.dp),
                    )
                }
            } else if (showIcon) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                Text(
                    text = if (contentDescription == "Pause") "Pause" else "Play",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}
