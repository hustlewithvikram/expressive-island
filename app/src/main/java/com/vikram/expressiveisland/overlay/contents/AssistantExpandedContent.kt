package com.vikram.expressiveisland.overlay.contents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.data.ActionButtonAlignment
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.resolve
import com.vikram.expressiveisland.overlay.toHorizontal

/**
 * The assistant tile's expanded layout: an assistant icon + the voice response text, wrapped in a
 * scrollable column constrained by max cutout height percentage.
 */
@Composable
fun AssistantExpandedContent(
    event: IslandEvent,
    showActions: Boolean,
    appearance: AppearanceSettings,
    collapsedHeightDp: Int,
    onDismiss: () -> Unit,
    onHeightMeasured: ((Int) -> Unit)? = null,
) {
    val assistant = event.assistant ?: return
    val contentColor = LocalContentColor.current
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val maxCutoutHeightDp =
        (configuration.screenHeightDp * assistant.maxCutoutHeightPercent / 100).dp
    val maxHeaderWidthDp = (configuration.screenWidthDp * 0.47f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxCutoutHeightDp)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp + collapsedHeightDp.dp,
                bottom = 14.dp
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Measure the scroll *content*, not the viewport. The scroll container above is clamped
                // to the island's live (animating) height, so measuring it would feed that height back
                // into its own fit-to-content target — the loop that made the cutout bob as the answer
                // streamed in. The content column is laid out with unbounded height, so its reported
                // height is the answer's true natural height, independent of the surrounding animation.
                // The reported height must cover the band as well as the 14dp padding above and
                // below, or the card fits itself to the answer alone and the band pushes the tail
                // of it out of view.
                .onGloballyPositioned { coordinates ->
                    val hDp = (coordinates.size.height / density).toInt()
                    if (hDp > 0) {
                        onHeightMeasured?.invoke(hDp + 28 + collapsedHeightDp)
                    }
                },
        ) {
            // Title header ("Assistant") constrained to max 47% screen width so it never goes behind camera hole
            Row(
                modifier = Modifier.widthIn(max = maxHeaderWidthDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(event = event, badgeSize = 36.dp, iconSize = 22.dp)
                Spacer(Modifier.width(10.dp))
                TileTitle(title = event.label)
            }

            // Answer content text displayed below title header
            if (assistant.displayAnswerInCutout) {
                Spacer(Modifier.height(8.dp))
                val textToDisplay =
                    assistant.answerText.takeIf { !it.isNullOrBlank() } ?: "Assistant active..."
                Text(
                    text = textToDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.88f),
                )
            }

            // Close action button at the end, obeying action button settings
            if (showActions) {
                Spacer(Modifier.height(14.dp))
                val chipFill = appearance.actionButtonColor?.resolve() ?: event.accent
                val full = appearance.actionButtonAlignment == ActionButtonAlignment.FULL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        appearance.actionButtonAlignment.toHorizontal()
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionChip(
                        action = IslandAction(label = "Close"),
                        style = appearance.actionButtonStyle,
                        fill = chipFill,
                        heightDp = appearance.actionButtonHeightDp,
                        onClick = onDismiss,
                        modifier = if (full) Modifier.weight(1f) else Modifier,
                    )
                }
            }
        }
    }
}