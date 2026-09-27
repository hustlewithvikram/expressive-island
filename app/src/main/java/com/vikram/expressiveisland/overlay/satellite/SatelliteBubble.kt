package com.vikram.expressiveisland.overlay.satellite

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.core.NowPlayingBus
import com.vikram.expressiveisland.core.OnCallBus
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.overlay.contents.AlbumArt
import com.vikram.expressiveisland.overlay.contents.ContactPhoto
import com.vikram.expressiveisland.overlay.contents.IconBadge
import com.vikram.expressiveisland.overlay.contents.IslandSurface
import com.vikram.expressiveisland.overlay.contents.albumArtFor
import com.vikram.expressiveisland.overlay.contents.albumArtStrokeFor
import com.vikram.expressiveisland.overlay.island.IslandEvent

/**
 * Small secondary event bubble used by the split-island feature. It reuses the same surface and
 * badge rendering as the normal pill so themes, icons, album art and call photos stay consistent.
 */
@Composable
fun SatelliteBubble(
    event: IslandEvent,
    diameterDp: Int,
    appearance: AppearanceSettings,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val nowPlaying by NowPlayingBus.state.collectAsStateWithLifecycle()
    val onCall by OnCallBus.state.collectAsStateWithLifecycle()
    val albumArt = albumArtFor(event, nowPlaying)
    val callPhoto = event.call?.takeIf { it.showPhoto }?.let { onCall?.photo }
    val badgeSize = (diameterDp * 0.72f).dp

    IslandSurface(
        modifier = modifier
            .size(diameterDp.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        shape = CircleShape,
        appearance = appearance,
        progress = 0f,
        appColor = event.appColor,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                albumArt != null -> AlbumArt(
                    bitmap = albumArt,
                    size = badgeSize,
                    rotate = event.media?.rotateAlbumArt == true,
                    playing = nowPlaying?.isPlaying == true,
                    strokeColor = albumArtStrokeFor(event),
                )

                callPhoto != null -> ContactPhoto(
                    bitmap = callPhoto,
                    size = badgeSize,
                )

                else -> IconBadge(
                    event = event,
                    badgeSize = badgeSize,
                    iconSize = (diameterDp * 0.46f).dp,
                )
            }
        }
    }
}
