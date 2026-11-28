package com.vikram.expressiveisland.overlay.island

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.vikram.expressiveisland.core.IslandPreviewCategory
import com.vikram.expressiveisland.core.IslandPreviewContent
import com.vikram.expressiveisland.overlay.MaterialIconCatalog
import com.vikram.expressiveisland.overlay.toImageBitmap
import com.vikram.expressiveisland.overlay.island.IslandReply
import com.vikram.expressiveisland.overlay.island.DownloadTileOptions
import com.vikram.expressiveisland.data.MusicTileSettings
import com.vikram.expressiveisland.data.PhoneTileSettings
import com.vikram.expressiveisland.data.TimerTileSettings
import com.vikram.expressiveisland.data.DownloadTileSettings
import com.vikram.expressiveisland.data.IconSource
import com.vikram.expressiveisland.service.ProgressData

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
        downloadSettings: DownloadTileSettings = DownloadTileSettings(),
    ): IslandEvent {
        fun previewIntent(requestCode: Int) = PendingIntent.getActivity(
            context, requestCode, Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val actions = when (category) {
            IslandPreviewCategory.ACTION_BUTTONS -> {
            listOf(
                IslandAction(
                    label = "Reply",
                    intent = previewIntent(0),
                    reply = IslandReply(
                        resultKey = "preview_reply",
                        remoteInputs = listOf(RemoteInput.Builder("preview_reply").setLabel("Reply to message").build()),
                        hint = "Reply to message",
                    ),
                ),
                IslandAction(label = "Archive", intent = previewIntent(1)),
            )
            }
            IslandPreviewCategory.PHONE_TILE -> when {
                content.detail.contains("Ongoing", ignoreCase = true) ->
                    listOf(IslandAction(label = "Hang up", intent = previewIntent(2), destructive = true))
                content.detail.contains("Outgoing", ignoreCase = true) ->
                    listOf(IslandAction(label = "Cancel call", intent = previewIntent(2), destructive = true))
                else -> listOf(
                    IslandAction(label = "Decline", intent = previewIntent(2), destructive = true),
                    IslandAction(label = "Answer", intent = previewIntent(3), answer = true),
                )
            }
            IslandPreviewCategory.TIMER_TILE -> listOf(
                IslandAction(label = "Reset", intent = previewIntent(4), destructive = true),
                IslandAction(label = "Add 1 min", intent = previewIntent(5)),
            )
            IslandPreviewCategory.DOWNLOAD_TILE -> buildList {
                if (downloadSettings.showPauseResume) {
                    add(IslandAction(label = "Pause", intent = previewIntent(6)))
                }
                if (downloadSettings.showCancel) {
                    add(IslandAction(label = "Cancel", intent = previewIntent(7), destructive = true))
                }
            }
            else -> emptyList()
        }

        return IslandEvent(
            id = -1L,
            icon = if (category == IslandPreviewCategory.DOWNLOAD_TILE) {
                val appIcon = if (downloadSettings.showAppIcon) runCatching {
                    IslandIcon.Raster(context.packageManager.getApplicationIcon(context.packageName).toImageBitmap())
                }.getOrNull() else null
                appIcon
                    ?: (downloadSettings.icon as? IconSource.Material)?.let { MaterialIconCatalog.iconFor(it.iconName) }
                        ?.let(IslandIcon::Vector)
                    ?: IslandIcon.Vector(iconFor(category))
            } else IslandIcon.Vector(iconFor(category)),
            label = content.label,
            detail = content.detail,
            accent = when (category) {
                IslandPreviewCategory.MUSIC_TILE -> Color(0xFFF472B6)
                IslandPreviewCategory.PHONE_TILE -> Color(0xFF22C55E)
                IslandPreviewCategory.TIMER_TILE -> Color(0xFFF59E0B)
                IslandPreviewCategory.DOWNLOAD_TILE -> Color(0xFF38BDF8)
                else -> Color(0xFF60A5FA)
            },
            actions = actions,
            iconContainerColor = when (category) {
                IslandPreviewCategory.PHONE_TILE -> phoneSettings.iconContainerColor
                IslandPreviewCategory.TIMER_TILE -> timerSettings.iconContainerColor
                IslandPreviewCategory.DOWNLOAD_TILE -> if (downloadSettings.showAppIcon) null else downloadSettings.iconContainerColor
                else -> null
            },
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
                showButtonLabels = phoneSettings.showButtonLabels,
                incomingAnswerColor = phoneSettings.incomingAnswerColor,
                incomingHangUpColor = phoneSettings.incomingHangUpColor,
                expandedHangUpColor = phoneSettings.expandedHangUpColor,
                miniCall = phoneSettings.miniCall,
                previewOutgoing = content.detail.contains("Outgoing", ignoreCase = true),
                previewPhoto = if (phoneSettings.showPhoto) createPreviewContactPhoto() else null,
            ) else null,
            timer = if (category == IslandPreviewCategory.TIMER_TILE) TimerTileOptions(
                showActions = timerSettings.showActions,
                resetColor = timerSettings.resetColor,
                addButtonColor = timerSettings.addButtonColor,
                previewRemainingSeconds = if (category == IslandPreviewCategory.TIMER_TILE) 15 * 60 else null,
            ) else null,
            download = if (category == IslandPreviewCategory.DOWNLOAD_TILE) DownloadTileOptions(
                showText = downloadSettings.showText,
                showPercentage = downloadSettings.showPercentage,
                showAppIcon = downloadSettings.showAppIcon,
                showProgress = downloadSettings.showProgress,
                progressStyle = downloadSettings.progressStyle,
                progressColor = downloadSettings.progressColor,
                trackColor = downloadSettings.trackColor,
                immersiveProgress = downloadSettings.immersiveProgress,
                showPauseResume = downloadSettings.showPauseResume,
                showCancel = downloadSettings.showCancel,
                pillShowProgress = downloadSettings.pillShowProgress,
                pillImmersiveProgress = downloadSettings.pillImmersiveProgress,
                pillProgressColor = downloadSettings.pillProgressColor,
                pillTrackColor = downloadSettings.pillTrackColor,
                actionButtonColor = downloadSettings.actionButtonColor,
                pillProgressStyle = downloadSettings.pillProgressStyle,
            ) else null,
            progressData = if (category == IslandPreviewCategory.DOWNLOAD_TILE) {
                ProgressData(max = 100, current = 64, title = "Downloading update.zip")
            } else null,
            normalOnly = false,
        )
    }


    /** Synthetic portrait used only by the settings preview to verify photo visibility and sizing. */
    private fun createPreviewContactPhoto() = runCatching {
        val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = android.graphics.Color.rgb(22, 92, 65)
        canvas.drawCircle(64f, 64f, 64f, paint)
        paint.color = android.graphics.Color.rgb(242, 190, 151)
        canvas.drawCircle(64f, 47f, 25f, paint)
        paint.color = android.graphics.Color.rgb(42, 48, 62)
        canvas.drawOval(RectF(22f, 76f, 106f, 150f), paint)
        bitmap.asImageBitmap()
    }.getOrNull()

    private fun iconFor(category: IslandPreviewCategory) = when (category) {
        IslandPreviewCategory.NOTIFICATION_CONTENT -> Icons.Rounded.NotificationsActive
        IslandPreviewCategory.CAMERA_RING -> Icons.Rounded.CameraAlt
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
        IslandPreviewCategory.PHONE_TILE -> Icons.Rounded.Person
        IslandPreviewCategory.TIMER_TILE -> Icons.Rounded.Timer
        IslandPreviewCategory.DOWNLOAD_TILE -> Icons.Rounded.FileDownload
        IslandPreviewCategory.EVENT_DETAIL -> Icons.Rounded.Info
        IslandPreviewCategory.EVENT_ICONS -> Icons.Rounded.AutoAwesome
        IslandPreviewCategory.INTEGRATION -> Icons.Rounded.Link
        IslandPreviewCategory.PERMISSION_DOTS -> Icons.Rounded.NotificationsActive
        IslandPreviewCategory.EMPTY_STATE -> Icons.Rounded.VisibilityOff
        IslandPreviewCategory.SHIZUKU -> Icons.Rounded.Security
        IslandPreviewCategory.APPEARANCE -> Icons.Rounded.Palette
    }
}
