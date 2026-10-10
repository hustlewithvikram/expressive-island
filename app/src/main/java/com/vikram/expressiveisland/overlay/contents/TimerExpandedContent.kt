package com.vikram.expressiveisland.overlay.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.core.RunningTimerBus
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.island.TimerTileOptions
import com.vikram.expressiveisland.overlay.resolve

/**
 * The timer tile's expanded layout: a timer icon + the ticking remaining time, and (when enabled)
 * the Reset / Add 1-min chips. The countdown is read live from [RunningTimerBus]; the chips fire the
 * clock app's own notification actions, coloured by [TimerTileOptions] (Reset apart from Add 1 min).
 */
@Composable
fun TimerExpandedContent(
    event: IslandEvent,
    appearance: AppearanceSettings,
    collapsedHeightDp: Int,
    onAction: (IslandAction) -> Unit,
) {
    val timer = event.timer ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, top = collapsedHeightDp.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ACTIONS_ROW_SPACING_DP.dp),
        ) {
            // Weighted so the Reset / Add 1 min chips keep their height and the text gives way.
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBadge(event = event, badgeSize = 44.dp, iconSize = 26.dp)
                // The remaining time is the headline; the timer name sits beneath.
                TileTitle(
                    title = timerRemainingText() ?: event.label,
                    subtitle = event.label,
                    modifier = Modifier.weight(1f),
                )
            }
            if (timer.showActions && event.actions.isNotEmpty()) {
                // A reset / stop button gets its own colour; every other button shares the second.
                val resetFill = timer.resetColor.resolve()
                val addFill = timer.addButtonColor.resolve()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    event.actions.take(3).forEach { action ->
                        ActionChip(
                            action = action,
                            style = appearance.actionButtonStyle,
                            fill = if (action.destructive) resetFill else addFill,
                            heightDp = appearance.actionButtonHeightDp,
                            onClick = { onAction(action) },
                        )
                    }
                }
            }
        }
    }
}