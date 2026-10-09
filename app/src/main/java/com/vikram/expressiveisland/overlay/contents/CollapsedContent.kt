package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            !isStickToCamera
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

        event.progressData?.takeIf { !isStickToCamera }?.let { progress ->
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