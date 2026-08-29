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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vikram.expressiveisland.data.CutoutColor
import com.vikram.expressiveisland.data.DynamicRole
import com.vikram.expressiveisland.data.PermissionDotColors
import com.vikram.expressiveisland.system.PermissionUsage
import kotlin.math.roundToInt

/** A dot's diameter and the gap between two of them, both as a fraction of the pill's height. */
private const val DOT_SIZE_FRACTION = 0.20f
private const val DOT_GAP_FRACTION = 0.14f

/** Smaller dots used by the stacked layout so all three fit comfortably inside the pill. */
private const val VERTICAL_DOT_SIZE_FRACTION = 0.15f
private const val VERTICAL_DOT_GAP_FRACTION = 0.06f

/** Gap between the pill icon and dots when they are placed on the leading edge. */
private const val DOT_LEADING_GAP_FRACTION = 0.25f

/** Inset from the pill trailing edge when dots are placed there. */
private const val DOT_TRAILING_INSET_FRACTION = 0.36f

/** Existing trailing content inset used by timer/progress content. */
internal const val COLLAPSED_TRAILING_INSET_FRACTION = 0.24f

internal fun permissionDotStartInsetDp(heightDp: Int): Float =
    heightDp * (0.16f + 0.72f + DOT_LEADING_GAP_FRACTION)

internal fun permissionDotEndInsetDp(heightDp: Int): Float =
    heightDp * DOT_TRAILING_INSET_FRACTION

/**
 * Extra width required when the dots share the trailing edge with timer/progress content.
 * The vertical form remains one dot wide regardless of how many resources are active.
 */
internal fun permissionDotTrailingInsetDp(
    usage: PermissionUsage,
    heightDp: Int,
    vertical: Boolean = false,
): Int {
    if (usage.count == 0) return 0
    val room = permissionDotEndInsetDp(heightDp) +
        permissionDotRowWidthDp(usage, heightDp, vertical) +
        heightDp * DOT_GAP_FRACTION
    return (room - heightDp * COLLAPSED_TRAILING_INSET_FRACTION)
        .coerceAtLeast(0f)
        .roundToInt()
}

/** Width consumed by the visible dot row. */
internal fun permissionDotRowWidthDp(
    usage: PermissionUsage,
    heightDp: Int,
    vertical: Boolean = false,
): Int {
    if (usage.count == 0) return 0
    val count = if (vertical) 1 else usage.count
    val sizeFraction = if (vertical) VERTICAL_DOT_SIZE_FRACTION else DOT_SIZE_FRACTION
    val gapFraction = if (vertical) VERTICAL_DOT_GAP_FRACTION else DOT_GAP_FRACTION
    val dots = count * heightDp * sizeFraction
    val gaps = count * heightDp * gapFraction
    return (dots + gaps).roundToInt()
}

/**
 * Microphone / camera / location usage indicators. Defaults remain backward-compatible with the
 * existing call site, while the optional colour and vertical arguments allow the controller to pass
 * the user's settings exactly like upstream.
 */
@Composable
internal fun PermissionDotRow(
    usage: PermissionUsage,
    heightDp: Int,
    modifier: Modifier = Modifier,
    colors: PermissionDotColors = PermissionDotColors(),
    vertical: Boolean = false,
) {
    val dots: @Composable () -> Unit = {
        PermissionDot(usage.microphone, colors.microphone.resolveForDot(), heightDp, vertical)
        PermissionDot(usage.camera, colors.camera.resolveForDot(), heightDp, vertical)
        PermissionDot(usage.location, colors.location.resolveForDot(), heightDp, vertical)
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

private fun CutoutColor.resolveForDot(): Color = when (this) {
    is CutoutColor.Solid -> Color(argb)
    is CutoutColor.Dynamic -> when (role) {
        DynamicRole.PRIMARY -> Color(0xFF6750A4)
        DynamicRole.SECONDARY -> Color(0xFF625B71)
        DynamicRole.TERTIARY -> Color(0xFF7D5260)
    }
    is CutoutColor.AppIcon -> Color(0xFF6750A4)
}

private fun PermissionDot(
    visible: Boolean,
    color: Color,
    heightDp: Int,
    vertical: Boolean,
) {
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
                    else Modifier.padding(horizontal = gap),
                )
                .size((heightDp * sizeFraction).dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
