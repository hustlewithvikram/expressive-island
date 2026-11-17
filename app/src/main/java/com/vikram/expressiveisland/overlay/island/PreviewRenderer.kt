package com.vikram.expressiveisland.overlay.island

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
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
    ): IslandEvent {
        val actions = if (category == IslandPreviewCategory.ACTION_BUTTONS) {
            val noOp = PendingIntent.getActivity(
                context,
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            listOf(
                IslandAction(label = "Reply", intent = noOp),
                IslandAction(label = "Archive", intent = noOp),
            )
        } else {
            emptyList()
        }

        return IslandEvent(
            id = -1L,
            icon = IslandIcon.Vector(iconFor(category)),
            label = content.label,
            detail = content.detail,
            accent = Color(0xFF60A5FA),
            actions = actions,
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
        IslandPreviewCategory.EVENT_DETAIL -> Icons.Rounded.Info
        IslandPreviewCategory.EVENT_ICONS -> Icons.Rounded.AutoAwesome
        IslandPreviewCategory.INTEGRATION -> Icons.Rounded.Link
        IslandPreviewCategory.PERMISSION_DOTS -> Icons.Rounded.NotificationsActive
        IslandPreviewCategory.EMPTY_STATE -> Icons.Rounded.VisibilityOff
        IslandPreviewCategory.SHIZUKU -> Icons.Rounded.Security
        IslandPreviewCategory.APPEARANCE -> Icons.Rounded.Palette
    }
}
