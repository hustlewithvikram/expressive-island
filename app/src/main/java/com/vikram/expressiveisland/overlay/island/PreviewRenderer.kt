package com.vikram.expressiveisland.overlay.island

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.Color
import com.vikram.expressiveisland.core.IslandPreviewCategory
import com.vikram.expressiveisland.core.IslandPreviewContent
import com.vikram.expressiveisland.data.MusicTileSettings
import com.vikram.expressiveisland.data.PhoneTileSettings
import com.vikram.expressiveisland.data.TimerTileSettings

/**
 * Single source of truth for the real system-island preview shown by settings.
 *
 * Every settings category gets a semantic icon and a representative event. The controller only
 * owns lifecycle/overlay state; all preview presentation decisions live here.
 */
internal object PreviewRenderer {

    fun createEvent(
        context: Context,
        category: IslandPreviewCategory,
        content: IslandPreviewContent,
        musicSettings: MusicTileSettings = MusicTileSettings(),
        phoneSettings: PhoneTileSettings = PhoneTileSettings(),
        timerSettings: TimerTileSettings = TimerTileSettings(),
    ): IslandEvent {
        fun previewIntent(requestCode: Int) = PendingIntent.getActivity(
            context, requestCode, Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val actions = when (category) {
            IslandPreviewCategory.ACTION_BUTTONS -> {
            listOf(
                IslandAction(label = "Reply", intent = previewIntent(0)),
                IslandAction(label = "Archive", intent = previewIntent(1)),
            )
            }
            IslandPreviewCategory.PHONE_TILE -> listOf(
                IslandAction(label = "Hang up", intent = previewIntent(2), destructive = true),
                IslandAction(label = "Answer", intent = previewIntent(3), answer = true),
            )
            IslandPreviewCategory.TIMER_TILE -> listOf(
                IslandAction(label = "Reset", intent = previewIntent(4), destructive = true),
                IslandAction(label = "Add 1 min", intent = previewIntent(5)),
            )
            else -> emptyList()
        }

        return IslandEvent(
            id = -1L,
            icon = IslandIcon.Vector(iconFor(category)),
            label = content.label,
            detail = content.detail,
            accent = when (category) {
                IslandPreviewCategory.MUSIC_TILE -> Color(0xFFF472B6)
                IslandPreviewCategory.PHONE_TILE -> Color(0xFF22C55E)
                IslandPreviewCategory.TIMER_TILE -> Color(0xFFF59E0B)
                else -> Color(0xFF60A5FA)
            },
            actions = actions,
            media = if (category == IslandPreviewCategory.MUSIC_TILE) MediaTileOptions(
                showAlbumArt = musicSettings.showAlbumArt,
                rotateAlbumArt = musicSettings.rotateAlbumArt,
                albumArtStroke = musicSettings.albumArtStroke,
                albumArtStrokeColor = musicSettings.albumArtStrokeColor,
                showControls = musicSettings.showControls,
                showProgress = musicSettings.showProgress,
                skipStyle = musicSettings.skipButton,
                playPauseStyle = musicSettings.playPauseButton,
                previewPlaying = true,
            ) else null,
            call = if (category == IslandPreviewCategory.PHONE_TILE) CallTileOptions(
                showPhoto = phoneSettings.showPhoto,
                showDuration = phoneSettings.showDuration,
                showActions = phoneSettings.showActions,
                incomingExpandedLayout = phoneSettings.expandedIncomingLayout,
                hangUpColor = phoneSettings.hangUpColor,
                otherButtonColor = phoneSettings.otherButtonColor,
            ) else null,
            timer = if (category == IslandPreviewCategory.TIMER_TILE) TimerTileOptions(
                showActions = timerSettings.showActions,
                resetColor = timerSettings.resetColor,
                addButtonColor = timerSettings.addButtonColor,
            ) else null,
            normalOnly = category == IslandPreviewCategory.PHONE_TILE,
        )
    }

    private fun iconFor(category: IslandPreviewCategory) = when (category) {
        IslandPreviewCategory.SIZE_POSITION -> Icons.Rounded.Straighten
        IslandPreviewCategory.BACKGROUND -> Icons.Rounded.FormatColorFill
        IslandPreviewCategory.ACTION_BUTTONS -> Icons.Rounded.TouchApp
        IslandPreviewCategory.ANIMATION -> Icons.Rounded.Animation
        IslandPreviewCategory.BEHAVIOUR -> Icons.Rounded.Settings
        IslandPreviewCategory.APPS -> Icons.Rounded.Apps
        IslandPreviewCategory.ASSISTANT -> Icons.Rounded.SmartToy
        IslandPreviewCategory.CAMERA_HOLE -> Icons.Rounded.CameraAlt
        IslandPreviewCategory.DYNAMIC_TILES -> Icons.Rounded.Widgets
        IslandPreviewCategory.MUSIC_TILE -> Icons.Rounded.MusicNote
        IslandPreviewCategory.PHONE_TILE -> Icons.Rounded.Call
        IslandPreviewCategory.TIMER_TILE -> Icons.Rounded.Timer
        IslandPreviewCategory.EVENT_DETAIL -> Icons.Rounded.Info
        IslandPreviewCategory.EVENT_ICONS -> Icons.Rounded.AutoAwesome
        IslandPreviewCategory.INTEGRATION -> Icons.Rounded.Link
        IslandPreviewCategory.PERMISSION_DOTS -> Icons.Rounded.NotificationsActive
        IslandPreviewCategory.EMPTY_STATE -> Icons.Rounded.VisibilityOff
        IslandPreviewCategory.SHIZUKU -> Icons.Rounded.Security
        IslandPreviewCategory.APPEARANCE -> Icons.Rounded.Palette
    }
}
