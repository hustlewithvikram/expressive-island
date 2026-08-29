package com.vikram.expressiveisland.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.system.PermissionUsage
import com.vikram.expressiveisland.data.PermissionDotColors
import kotlin.math.roundToInt

/**
 * The dot colours, one per watched resource. Fixed rather than themed for the same reason the pill's
 * own text is: these read against the user's chosen island background, not against the app's
 * surfaces, and their meaning is the colour — the system's own indicator trains people to read green
 * as the microphone.
 */
private val MICROPHONE_DOT_COLOR = Color(0xFF19C337)
private val CAMERA_DOT_COLOR = Color(0xFFE5484D)
private val LOCATION_DOT_COLOR = Color(0xFF3B82F6)

/** A dot's diameter and the gap between two of them, both as a fraction of the pill's height. */
private const val DOT_SIZE_FRACTION = 0.20f
private const val DOT_GAP_FRACTION = 0.14f

/**
 * The same two, for the stacked layout: three dots and their gaps have to share the pill's height
 * rather than its width, so both shrink to fit (3 × 0.15 + 3 × 0.06 = 0.63 of the height).
 */
private const val VERTICAL_DOT_SIZE_FRACTION = 0.15f
private const val VERTICAL_DOT_GAP_FRACTION = 0.06f

/**
 * The gap between the pill's icon and the first dot, as a fraction of the pill's height. Sized so
 * the dots sit clear of the icon but still well inside the camera hole when placed on the left.
 */
private const val DOT_LEADING_GAP_FRACTION = 0.25f

/**
 * Inset from the pill's trailing edge, as a fraction of its height. Matches the inset the timer's
 * remaining-time text and the progress ring already use on that edge.
 */
private const val DOT_TRAILING_INSET_FRACTION = 0.24f

/**
 * How far from the pill's leading edge the dots start when placed on the left: past the icon badge
 * ([CollapsedContent] insets it by 0.16 and sizes it at 0.72 of the height) plus a gap.
 */
internal fun permissionDotStartInsetDp(heightDp: Int): Float =
    heightDp * (0.16f + 0.72f + DOT_LEADING_GAP_FRACTION)

/** How far from the pill's trailing edge the dots start when placed on the right. */
internal fun permissionDotEndInsetDp(heightDp: Int): Float = heightDp * DOT_TRAILING_INSET_FRACTION

/**
 * The room [PermissionDotRow] needs on the trailing edge, so collapsed content that also sits there
 * can be shifted clear of it. Zero when there is nothing to draw.
 */

/**
 * The room [PermissionDotRow] needs on the trailing edge, so collapsed content that also sits there
 * can be shifted clear of it. Zero when there is nothing to draw.
 */
internal fun permissionDotRowWidthDp(usage: PermissionUsage, heightDp: Int, vertical: Boolean = false): Int {
    if (usage.count == 0) return 0
    // Stacked, the dots are only ever one wide however many are lit — and a smaller one at that.
    val count = if (vertical) 1 else usage.count
    val sizeFraction = if (vertical) VERTICAL_DOT_SIZE_FRACTION else DOT_SIZE_FRACTION
    val gapFraction = if (vertical) VERTICAL_DOT_GAP_FRACTION else DOT_GAP_FRACTION
    val dots = count * heightDp * sizeFraction
    // Each dot carries half a gap on each side, so one gap per dot.
    val gaps = count * heightDp * gapFraction
    return (dots + gaps).roundToInt()
}

/**
 * The microphone / camera / location dots, drawn on the collapsed pill while an app is using that
 * resource. Sized to [heightDp] so they follow the user's own geometry, and each fades and scales in
 * on its own so a second resource lighting up doesn't restart the first dot's animation.
 */
@Composable
internal fun PermissionDotRow(
    usage: PermissionUsage,
    colors: PermissionDotColors,
    heightDp: Int,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    // Microphone, camera, location — declared once so both directions draw the same three dots.
    val dots: @Composable () -> Unit = {
        PermissionDot(usage.microphone, colors.microphone.resolve(), heightDp, vertical)
        PermissionDot(usage.camera, colors.camera.resolve(), heightDp, vertical)
        PermissionDot(usage.location, colors.location.resolve(), heightDp, vertical)
    }

    if (vertical) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            dots()
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dots()
        }
    }
}

/** One dot, popping in and out with the resource it stands for. */
@Composable
private fun PermissionDot(visible: Boolean, color: Color, heightDp: Int, vertical: Boolean) {
    val sizeFraction = if (vertical) VERTICAL_DOT_SIZE_FRACTION else DOT_SIZE_FRACTION
    val gapFraction = if (vertical) VERTICAL_DOT_GAP_FRACTION else DOT_GAP_FRACTION
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.4f) +
            if (vertical) expandVertically(clip = false) else expandHorizontally(clip = false),
        exit = fadeOut() + scaleOut(targetScale = 0.4f) +
            if (vertical) shrinkVertically(clip = false) else shrinkHorizontally(clip = false),
    ) {
        val gap = (heightDp * gapFraction / 2f).dp
        Box(
            modifier = Modifier
                .then(
                    if (vertical) Modifier.padding(vertical = gap)
                    else Modifier.padding(horizontal = gap)
                )
                .size((heightDp * sizeFraction).dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}