package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
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
import com.vikram.expressiveisland.data.DownloadPillProgressStyle
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
    val ringPreferences = remember(context) { SatelliteRingPreferences(context) }
    val ringSettings by ringPreferences.settings.collectAsStateWithLifecycle(initialValue = com.vikram.expressiveisland.data.SatelliteRingSettings())
    // A split bubble shortens the pill. If the camera activity ring is disabled, relocate
    // media/download progress feedback to the leading icon instead.
    val hideSatelliteIndicators = satelliteSharing
    val onCall by OnCallBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)
    val callPhoto = event.call?.takeIf { it.showPhoto }?.let { onCall?.photo }
    val badgeSize = (heightDp * 0.72f).dp
    val showIconProgressRing = satelliteSharing && !ringSettings.enabled &&
        (event.media != null || event.progressData != null) && event.download?.pillShowProgress != false
    val pillImmersive = event.download?.let { it.pillShowProgress && it.pillImmersiveProgress } == true &&
        event.progressData != null && !isStickToCamera && !satelliteSharing
    val pillProgress = event.progressData
    val pillFraction = pillProgress?.takeIf { !it.isIndeterminate && it.max > 0 }
        ?.let { (it.current.toFloat() / it.max).coerceIn(0f, 1f) } ?: 0f
    val pillIndeterminate = rememberInfiniteTransition(label = "pillImmersiveProgress")
    val pillIndeterminateFraction by pillIndeterminate.animateFloat(
        initialValue = 0.08f, targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1800), repeatMode = RepeatMode.Reverse),
        label = "pillImmersiveProgressFraction",
    )
    val animatedPillFraction by animateFloatAsState(pillFraction, label = "pillImmersiveProgressDeterminate")
    val pillDisplayFraction = if (pillProgress?.isIndeterminate == true || (pillProgress?.max ?: 0) <= 0) pillIndeterminateFraction else animatedPillFraction
    val pillProgressColor = event.download?.pillProgressColor?.resolve()
        ?: event.download?.progressColor?.resolve() ?: MaterialTheme.colorScheme.primary
    val pillTrackColor = event.download?.pillTrackColor?.resolve()
        ?: event.download?.trackColor?.resolve() ?: MaterialTheme.colorScheme.surfaceContainerHighest

    Box(modifier = Modifier.fillMaxSize()) {
        // Keep immersive progress in its own first-drawn layer. A sibling Canvas is
        // guaranteed to sit beneath every icon, label, timer and interactive overlay
        // composable instead of tinting foreground content.
        if (pillImmersive) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = pillTrackColor,
                    cornerRadius = CornerRadius(size.height / 2f),
                )
                drawRoundRect(
                    color = pillProgressColor.copy(alpha = 0.78f),
                    size = Size(size.width * pillDisplayFraction, size.height),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
        }

        val placement = Modifier
            .align(if (isStickToCamera) Alignment.BottomCenter else Alignment.CenterStart)
            .padding(
                start = if (isStickToCamera) 0.dp else (heightDp * 0.16f).dp,
                bottom = if (isStickToCamera) (heightDp * 0.14f).dp else 0.dp,
            )
        if (showIconProgressRing) {
            Box(modifier = placement.size(badgeSize), contentAlignment = Alignment.Center) {
                when {
                    albumArt != null -> AlbumArt(
                        bitmap = albumArt,
                        size = badgeSize - 6.dp,
                        rotate = event.media?.rotateAlbumArt == true,
                        playing = nowPlaying?.isPlaying == true,
                        strokeColor = albumArtStrokeFor(event),
                    )
                    callPhoto != null -> ContactPhoto(bitmap = callPhoto, size = badgeSize - 6.dp)
                    else -> IconBadge(
                        event = event,
                        badgeSize = badgeSize - 6.dp,
                        iconSize = (heightDp * 0.46f).dp,
                    )
                }
                CollapsedIconProgressRing(
                    event = event,
                    nowPlaying = nowPlaying,
                    diameterDp = badgeSize.value,
                    modifier = Modifier.fillMaxSize(),
                    ringSettings = ringSettings,
                )
            }
        } else {
            when {
                event.media?.previewPlaying == true && musicSettings.showAlbumArt -> PreviewAlbumArt(
                    modifier = placement.size(badgeSize),
                    rotate = event.media?.rotateAlbumArt == true,
                    playing = true,
                    strokeColor = albumArtStrokeFor(event),
                )
                albumArt != null -> AlbumArt(
                    bitmap = albumArt,
                    size = badgeSize,
                    modifier = placement,
                    rotate = event.media?.rotateAlbumArt == true,
                    playing = nowPlaying?.isPlaying == true,
                    strokeColor = albumArtStrokeFor(event),
                )
                callPhoto != null -> ContactPhoto(bitmap = callPhoto, size = badgeSize, modifier = placement)
                else -> IconBadge(
                    event = event,
                    badgeSize = badgeSize,
                    iconSize = (heightDp * 0.46f).dp,
                    modifier = placement,
                )
            }
        }

        if (
            event.media != null &&
            (nowPlaying?.isPlaying == true || event.media?.previewPlaying == true) &&
            musicSettings.showVisualizer &&
            !isStickToCamera &&
            !satelliteSharing
        ) {
            MusicVisualizer(
                isPlaying = true,
                style = musicSettings.visualizerStyle,
                color = musicSettings.visualizerColor?.resolve(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        end = (heightDp * 0.24f).dp + trailingInsetDp.dp,
                    ),
            )
        }

        // The timer tile shows the remaining time on the trailing edge, opposite its icon.
        if (event.timer != null && !isStickToCamera) {
            timerRemainingText(event.timer?.previewRemainingSeconds)?.let { remaining ->
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

        if (event.progressData != null && event.download?.pillShowProgress != false &&
            event.download?.pillImmersiveProgress != true &&
            !isStickToCamera && !hideSatelliteIndicators
        ) {
            val progress = event.progressData
            val style = event.download?.pillProgressStyle ?: DownloadPillProgressStyle.CIRCULAR
            val progressColor = event.download?.pillProgressColor?.resolve()
                ?: event.download?.progressColor?.resolve()
                ?: ringSettings.downloadProgressColor?.resolve()
                ?: MaterialTheme.colorScheme.primary
            val trackColor = event.download?.pillTrackColor?.resolve()
                ?: event.download?.trackColor?.resolve()
                ?: ringSettings.downloadTrackColor?.resolve()
                ?: MaterialTheme.colorScheme.primaryContainer
            val fraction = if (progress.isIndeterminate || progress.max <= 0) 0f
                else (progress.current.toFloat() / progress.max).coerceIn(0f, 1f)
            val animatedFraction by animateFloatAsState(fraction, label = "collapsedProgress")
            val trailing = Modifier.align(Alignment.CenterEnd)
                .padding(end = (heightDp * 0.24f).dp + trailingInsetDp.dp)
            when (style) {
                DownloadPillProgressStyle.CIRCULAR -> if (progress.isIndeterminate) {
                    CircularProgressIndicator(
                        modifier = trailing.size((heightDp * 0.5f).dp),
                        color = progressColor, trackColor = trackColor,
                        strokeWidth = (heightDp * 0.06f).dp,
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { animatedFraction }, modifier = trailing.size((heightDp * 0.5f).dp),
                        color = progressColor, trackColor = trackColor,
                        strokeWidth = (heightDp * 0.06f).dp,
                    )
                }
                DownloadPillProgressStyle.PERCENTAGE -> if (!progress.isIndeterminate && progress.max > 0) {
                    Text(text = "${(fraction * 100f).toInt().coerceIn(0, 100)}%",
                        color = LocalContentColor.current, fontSize = (heightDp * 0.27f).sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = trailing)
                }
                else -> Canvas(Modifier.align(Alignment.CenterEnd)
                    .padding(end = (heightDp * 0.24f).dp + trailingInsetDp.dp)
                    .size(width = 26.dp, height = 5.dp)) {
                    val h = size.height
                    val w = size.width
                    val radius = CornerRadius(h / 2f)
                    drawRoundRect(trackColor, size = size, cornerRadius = radius)
                    val fillWidth = w * (if (progress.isIndeterminate) 0.32f else animatedFraction)
                    when (style) {
                        DownloadPillProgressStyle.SEGMENTED -> {
                            val count = 8
                            val gap = 1.2.dp.toPx()
                            val segmentWidth = (w - gap * (count - 1)) / count
                            repeat(count) { index ->
                                val left = index * (segmentWidth + gap)
                                val fill = ((fillWidth - left) / segmentWidth).coerceIn(0f, 1f)
                                if (fill > 0f) drawRoundRect(progressColor, Offset(left, 0f),
                                    Size(segmentWidth * fill, h), radius)
                            }
                        }
                        DownloadPillProgressStyle.DOTTED -> repeat(12) { index ->
                            val x = (index + 0.5f) * w / 12f
                            val filled = (index + 1f) / 12f <= (if (progress.isIndeterminate) 0.35f else animatedFraction)
                            drawCircle(if (filled) progressColor else trackColor, h * 0.42f, Offset(x, h / 2f))
                        }
                        else -> drawRoundRect(progressColor, size = Size(fillWidth, h), cornerRadius = radius)
                    }
                }
            }
        }
    }
}

/**
 * Progress feedback relocated to the leading icon when a satellite bubble shortens the pill
 * and the camera activity ring is disabled.
 */
@Composable
private fun CollapsedIconProgressRing(
    event: IslandEvent,
    nowPlaying: com.vikram.expressiveisland.core.NowPlaying?,
    diameterDp: Float,
    modifier: Modifier,
    ringSettings: com.vikram.expressiveisland.data.SatelliteRingSettings,
) {
    val mediaProgress = nowPlaying?.progress
    val duration = mediaProgress?.durationMs ?: 0L
    val latestMediaProgress by rememberUpdatedState(mediaProgress)
    var mediaFraction by remember(event.id, duration) {
        mutableFloatStateOf(mediaProgress?.fractionAt(SystemClock.elapsedRealtime())?.coerceIn(0f, 1f) ?: 0f)
    }
    LaunchedEffect(event.id, duration, mediaProgress?.speed) {
        while (true) {
            val current = latestMediaProgress ?: break
            mediaFraction = current.fractionAt(SystemClock.elapsedRealtime())?.coerceIn(0f, 1f) ?: mediaFraction
            if (current.speed <= 0f) break
            delay(250L)
        }
    }
    val notificationProgress = event.progressData?.let { data ->
        if (data.isIndeterminate || data.max <= 0) null
        else (data.current.toFloat() / data.max).coerceIn(0f, 1f)
    }
    val hasNotificationProgress = event.progressData != null
    val hasMedia = event.media != null && nowPlaying != null
    if (!hasMedia && !hasNotificationProgress) return
    val mediaDeterminate = hasMedia && duration > 0L
    val notificationDeterminate = hasNotificationProgress && notificationProgress != null
    val determinate = mediaDeterminate || notificationDeterminate
    val fraction = when {
        notificationDeterminate -> notificationProgress!!
        mediaDeterminate -> mediaFraction
        else -> 0f
    }
    val shouldRotate = !determinate && (
        event.progressData?.isIndeterminate == true || (hasMedia && nowPlaying?.isPlaying == true)
    )
    val animatedFraction by animateFloatAsState(fraction, label = "iconActivityProgress")
    val transition = rememberInfiniteTransition(label = "iconActivityRing")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "iconActivityRingRotation",
    )
    val trackColor = event.download?.trackColor?.resolve()?.copy(alpha = ringSettings.opacity * 0.72f)
        ?: (
            if (hasNotificationProgress) ringSettings.downloadTrackColor else ringSettings.trackColor
        )?.resolve()?.copy(alpha = ringSettings.opacity * 0.72f)
        ?: MaterialTheme.colorScheme.primaryContainer.copy(alpha = ringSettings.opacity * 0.72f)
    val progressColor = event.download?.progressColor?.resolve()?.copy(alpha = ringSettings.opacity)
        ?: (
            if (hasNotificationProgress) ringSettings.downloadProgressColor else ringSettings.progressColor
        )?.resolve()?.copy(alpha = ringSettings.opacity)
        ?: MaterialTheme.colorScheme.primary.copy(alpha = ringSettings.opacity)
    val stroke = ringSettings.thicknessDp.coerceIn(1f, 6f).dp
    val diameter = (diameterDp * ringSettings.diameterScale).dp
    Canvas(modifier = modifier.size(diameter).rotate(if (shouldRotate) rotation else 0f)) {
        val strokePx = stroke.toPx()
        val inset = strokePx / 2f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx)
        drawArc(
            color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        if (shouldRotate || animatedFraction > 0f) {
            drawArc(
                color = progressColor,
                startAngle = if (shouldRotate) 15f else -90f,
                sweepAngle = if (shouldRotate) 105f else 360f * animatedFraction,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
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
    val ringDiameter = (diameterDp * ringSettings.diameterScale).dp
    val stroke = ringSettings.thicknessDp.dp
    val trackColor = (ringSettings.trackColor?.resolve() ?: MaterialTheme.colorScheme.primaryContainer).copy(alpha = ringSettings.opacity)
    val progressColor = (ringSettings.progressColor?.resolve() ?: MaterialTheme.colorScheme.primary).copy(alpha = ringSettings.opacity)
    val direction = if (ringSettings.clockwise) 1f else -1f
    Canvas(
        modifier = modifier
            .size(ringDiameter)
            .rotate(if (shouldRotate) rotation * direction else 0f),
    ) {
        val inset = stroke.toPx() / 2f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke.toPx(), size.height - stroke.toPx())
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = (size.minDimension - stroke.toPx()) / 2f
        fun drawDottedArc(color: Color, startAngle: Float, sweepAngle: Float, dotRadius: Float) {
            val circumference = 2f * Math.PI.toFloat() * radius
            val spacing = dotRadius * 3.2f
            val count = (circumference / spacing).toInt().coerceAtLeast(12)
            val steps = (count * kotlin.math.abs(sweepAngle) / 360f).toInt().coerceAtLeast(1)
            for (i in 0..steps) {
                val angle = Math.toRadians((startAngle + sweepAngle * i / steps).toDouble())
                drawCircle(color, dotRadius, androidx.compose.ui.geometry.Offset(
                    center.x + kotlin.math.cos(angle).toFloat() * radius,
                    center.y + kotlin.math.sin(angle).toFloat() * radius,
                ))
            }
        }
        if (ringSettings.style == SatelliteRingStyle.DOTTED) {
            drawDottedArc(trackColor, -90f, 360f, stroke.toPx() * 0.42f)
        } else {
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round))
        }
        if (shouldRotate || fraction > 0f) {
            val sweep = if (shouldRotate) 105f else 360f * fraction * direction
            val start = if (shouldRotate) 20f else -90f
            when (ringSettings.style) {
                SatelliteRingStyle.DOTTED -> drawDottedArc(progressColor, start, sweep, stroke.toPx() * 0.42f)
                SatelliteRingStyle.WAVY -> {
                    val path = androidx.compose.ui.graphics.Path()
                    val steps = (220 * (kotlin.math.abs(sweep) / 360f)).toInt().coerceAtLeast(2)
                    for (i in 0..steps) {
                        val angle = Math.toRadians((start + sweep * i / steps).toDouble())
                        val wave = kotlin.math.sin(i * 0.32f) * stroke.toPx() * 0.28f
                        val r = radius + wave
                        val point = androidx.compose.ui.geometry.Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * r,
                            center.y + kotlin.math.sin(angle).toFloat() * r
                        )
                        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, progressColor, style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round))
                }
                SatelliteRingStyle.EXPRESSIVE -> {
                    val expressiveEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(stroke.toPx() * 2.8f, stroke.toPx() * 0.75f, stroke.toPx() * 0.9f, stroke.toPx() * 0.75f), 0f
                    )
                    drawArc(color = progressColor, startAngle = start, sweepAngle = sweep, useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                        style = Stroke(width = stroke.toPx() * 1.18f, cap = StrokeCap.Round, pathEffect = expressiveEffect))
                }
                SatelliteRingStyle.SOLID -> drawArc(
                    color = progressColor, startAngle = start, sweepAngle = sweep, useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                    style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}
