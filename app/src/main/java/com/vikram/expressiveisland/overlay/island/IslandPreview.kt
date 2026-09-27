package com.vikram.expressiveisland.overlay.island

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.data.IslandLayout
import com.vikram.expressiveisland.overlay.contents.CollapsedContent
import com.vikram.expressiveisland.overlay.contents.ExpandedContent
import com.vikram.expressiveisland.overlay.contents.IslandSurface
import com.vikram.expressiveisland.overlay.cornerShape

/**
 * A static, non-interactive pill used by the settings screen for previewing one state.
 */
@Composable
fun IslandPreview(
    event: IslandEvent,
    width: Dp,
    heightDp: Int,
    cornerTopLeftDp: Int,
    cornerTopRightDp: Int,
    cornerBottomLeftDp: Int,
    cornerBottomRightDp: Int,
    expanded: Boolean,
    appearance: AppearanceSettings = AppearanceSettings(),
    showActions: Boolean = true,
    collapsedHeightDp: Int = IslandLayout.DEFAULT_COLLAPSED.heightDp,
) {
    IslandSurface(
        modifier = Modifier.size(width, heightDp.dp),
        shape = cornerShape(
            topLeft = cornerTopLeftDp.dp,
            topRight = cornerTopRightDp.dp,
            bottomLeft = cornerBottomLeftDp.dp,
            bottomRight = cornerBottomRightDp.dp,
        ),
        appearance = appearance,
        progress = if (expanded) 1f else 0f,
    ) {
        if (expanded) {
            ExpandedContent(
                event = event,
                systemEventType = null,
                chargingState = null,
                showActions = showActions,
                appearance = appearance,
                collapsedHeightDp = collapsedHeightDp,
                replyingTo = null,
                replySent = false,
                progressData = event.progressData,
                onAction = {},
                onStartReply = {},
                onCancelReply = {},
                onSendReply = {},
                onDismiss = {},
                onHeightMeasured = {},
            )
        } else {
            CollapsedContent(
                event = event,
                heightDp = heightDp,
            )
        }
    }
}
