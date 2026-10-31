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
import androidx.compose.ui.platform.LocalContext
import com.vikram.expressiveisland.overlay.island.IslandEvent

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
    val stroke = (diameterDp * 0.055f).dp
    val trackColor = MaterialTheme.colorScheme.primaryContainer
    val progressColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .size(ringDiameter)
            .rotate(if (shouldRotate) rotation else 0f),
    ) {
        val inset = stroke.toPx() / 2f
        val arcSize = androidx.compose.ui.geometry.Size(
            size.width - stroke.toPx(),
            size.height - stroke.toPx(),
        )
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
        )
        if (shouldRotate || fraction > 0f) {
            drawArc(
                color = progressColor,
                startAngle = if (shouldRotate) 20f else -90f,
                sweepAngle = if (shouldRotate) 105f else 360f * fraction,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
