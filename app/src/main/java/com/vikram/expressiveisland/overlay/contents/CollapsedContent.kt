package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.core.NowPlayingBus
import com.vikram.expressiveisland.core.OnCallBus
import com.vikram.expressiveisland.data.MusicTilePreferences
import com.vikram.expressiveisland.data.MusicTileSettings
import com.vikram.expressiveisland.data.SatelliteRingPreferences
import com.vikram.expressiveisland.data.SatelliteRingStyle
import androidx.compose.ui.platform.LocalContext
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.resolve

@Composable
fun CollapsedContent(
    event: IslandEvent,
    heightDp: Int,
    isStickToCamera: Boolean = false,
    trailingInsetDp: Int = 0,
    satelliteSharing: Boolean = false,
) {
    // The music tile shows album art, the phone tile the caller's photo, on the normal cutout.
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val musicPreferences = remember(context) { MusicTilePreferences(context) }
    val musicSettings by musicPreferences.settings.collectAsState(initial = MusicTileSettings())
    val onCall by OnCallBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)
    val callPhoto = event.call?.takeIf { it.showPhoto }?.let { onCall?.photo }
    val badgeSize = (heightDp * 0.72f).dp

    Box(modifier = Modifier.fillMaxSize()) {
        val placement = Modifier
            .align(if (isStickToCamera) Alignment.BottomCenter else Alignment.CenterStart)
            .padding(
                start = if (isStickToCamera) 0.dp else (heightDp * 0.16f).dp,
                bottom = if (isStickToCamera) (heightDp * 0.14f).dp else 0.dp,
            )
        when {
            albumArt != null -> AlbumArt(
                bitmap = albumArt,
                size = badgeSize,
                modifier = placement,
                rotate = event.media?.rotateAlbumArt == true,
                playing = nowPlaying?.isPlaying == true,
                strokeColor = albumArtStrokeFor(event),
            )

            callPhoto != null -> ContactPhoto(
                bitmap = callPhoto,
                size = badgeSize,
                modifier = placement
            )

            else -> IconBadge(
                event = event,
                badgeSize = badgeSize,
                iconSize = (heightDp * 0.46f).dp,
                modifier = placement,
            )
        }

        if (
            event.media != null &&
            nowPlaying?.isPlaying == true &&
            musicSettings.showVisualizer &&
            !isStickToCamera &&
            !satelliteSharing
        ) {
            MusicVisualizer(
                isPlaying = true,
                style = musicSettings.visualizerStyle,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        end = (heightDp * 0.24f).dp + trailingInsetDp.dp,
                    ),
            )
        }

        // The timer tile shows the remaining time on the trailing edge, opposite its icon.
        if (event.timer != null && !isStickToCamera) {
            timerRemainingText()?.let { remaining ->
                Text(
                    text = remaining,
                    color = LocalContentColor.current,
                    fontSize = (heightDp * 0.34f).sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = (heightDp * 0.24f).dp + trailingInsetDp.dp),
                )
            }
        }

        event.progressData?.takeIf { !isStickToCamera && !satelliteSharing }?.let { progress ->
            val indicatorModifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = (heightDp * 0.24f).dp + trailingInsetDp.dp)
                .size((heightDp * 0.5f).dp)
            val strokeWidth = (heightDp * 0.06f).dp
            if (progress.isIndeterminate) {
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeWidth = strokeWidth,
                )
            } else {
                val fraction = if (progress.max <= 0) 0f
                else (progress.current.toFloat() / progress.max).coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    label = "collapsedProgress",
                )
                CircularProgressIndicator(
                    progress = { animatedFraction },
                    modifier = indicatorModifier,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}

/**
 * Activity ring aligned to the physical camera cutout. Determinate media and notification
 * progress fill clockwise from 12 o'clock; only streams / indeterminate work rotate.
 */
@Composable
internal fun SatelliteActivityRing(
    event: IslandEvent,
    diameterDp: Int,
    modifier: Modifier = Modifier,
) {
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ringPreferences = remember(context) { SatelliteRingPreferences(context) }
    val ringSettings by ringPreferences.settings.collectAsStateWithLifecycle(initialValue = com.vikram.expressiveisland.data.SatelliteRingSettings())
    if (!ringSettings.enabled) return
    val mediaProgress = nowPlaying?.progress
    val latestMediaProgress by rememberUpdatedState(mediaProgress)
    val duration = mediaProgress?.durationMs ?: 0L
    var mediaFraction by androidx.compose.runtime.remember(event.id, duration) {
        mutableFloatStateOf(
            mediaProgress?.fractionAt(SystemClock.elapsedRealtime())?.coerceIn(0f, 1f) ?: 0f
        )
    }

    LaunchedEffect(event.id, duration, mediaProgress?.speed) {
        while (true) {
            val current = latestMediaProgress ?: break
            mediaFraction = current.fractionAt(SystemClock.elapsedRealtime())
                ?.coerceIn(0f, 1f) ?: mediaFraction
            if (current.speed <= 0f) break
            delay(250)
        }
    }

    val notificationProgress = event.progressData?.let { data ->
        if (data.isIndeterminate || data.max <= 0) null
        else (data.current.toFloat() / data.max).coerceIn(0f, 1f)
    }
    val hasMedia = event.media != null && nowPlaying != null
    val hasNotificationProgress = event.progressData != null
    if (!hasMedia && !hasNotificationProgress) return

    val mediaIsDeterminate = hasMedia && duration > 0L
    val notificationIsDeterminate = hasNotificationProgress && notificationProgress != null
    val isDeterminate = mediaIsDeterminate || notificationIsDeterminate
    val fraction = when {
        notificationIsDeterminate -> notificationProgress!!
        mediaIsDeterminate -> mediaFraction
        else -> 0f
    }
    val shouldRotate = !isDeterminate &&
        (event.progressData?.isIndeterminate == true || (hasMedia && nowPlaying?.isPlaying == true))

    val transition = rememberInfiniteTransition(label = "satelliteActivityRing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "satelliteActivityRingRotation",
    )
    val ringDiameter = (diameterDp * 0.94f).dp
    val stroke = ringSettings.thicknessDp.dp
    val trackColor = ringSettings.trackColor?.resolve() ?: MaterialTheme.colorScheme.primaryContainer
    val progressColor = ringSettings.progressColor?.resolve() ?: MaterialTheme.colorScheme.primary
    val direction = if (ringSettings.clockwise) 1f else -1f
    Canvas(
        modifier = modifier
            .size(ringDiameter)
            .rotate(if (shouldRotate) rotation * direction else 0f),
    ) {
        val inset = stroke.toPx() / 2f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke.toPx(), size.height - stroke.toPx())
        val cap = if (ringSettings.style == SatelliteRingStyle.DOTTED) StrokeCap.Butt else StrokeCap.Round
        val pathEffect = if (ringSettings.style == SatelliteRingStyle.DOTTED) {
            androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(stroke.toPx() * 0.7f, stroke.toPx() * 1.35f), 0f)
        } else null
        val trackStyle = Stroke(width = stroke.toPx(), cap = cap, pathEffect = pathEffect)
        drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = trackStyle)
        if (shouldRotate || fraction > 0f) {
            val sweep = if (shouldRotate) 105f else 360f * fraction * direction
            val start = if (shouldRotate) 20f else -90f
            if (ringSettings.style == SatelliteRingStyle.WAVY && !shouldRotate && fraction > 0f) {
                val path = androidx.compose.ui.graphics.Path()
                val radius = (size.minDimension - stroke.toPx()) / 2f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                val steps = (180 * fraction).toInt().coerceAtLeast(2)
                for (i in 0..steps) {
                    val angle = Math.toRadians((-90.0 + direction * 360.0 * fraction * i / steps).toDouble())
                    val wave = kotlin.math.sin(i * 0.42f) * stroke.toPx() * 0.18f
                    val r = radius + wave
                    val point = androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * r, center.y + kotlin.math.sin(angle).toFloat() * r)
                    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path, progressColor, style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round))
            } else {
                val expressiveWidth = if (ringSettings.style == SatelliteRingStyle.EXPRESSIVE) stroke.toPx() * 1.12f else stroke.toPx()
                drawArc(color = progressColor, startAngle = start, sweepAngle = sweep, useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                    style = Stroke(width = expressiveWidth, cap = if (ringSettings.style == SatelliteRingStyle.DOTTED) StrokeCap.Butt else StrokeCap.Round, pathEffect = pathEffect))
            }
        }
    }
}
