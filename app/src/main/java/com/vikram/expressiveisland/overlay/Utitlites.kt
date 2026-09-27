package com.vikram.expressiveisland.overlay

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Formats elapsed call seconds as m:ss, or h:mm:ss once the call passes an hour. */
fun formatCallDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatMediaTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1_000L)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatChargingTime(minutes: Long): String {
    if (minutes <= 0L) return ""

    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return when {
        hours > 0L ->
            "${hours}h ${remainingMinutes}m to full"

        else ->
            "${remainingMinutes}m to full"
    }
}

/** Circle for a spinning cover, else a rounded square whose radius scales with [size]. */
fun albumArtShape(rotate: Boolean, size: Dp) =
//    if (rotate) CircleShape else RoundedCornerShape(size * 0.24f)
    if (rotate) CircleShape else RoundedCornerShape(100.dp)

/**
 * Builds a rounded shape with each corner independently sized (LTR-mapped). Corners are clamped to
 * be non-negative: the spring animation driving the radii overshoots below its target, so easing a
 * corner down to 0 dp momentarily produces a negative value, which Compose refuses to render
 * ("RoundRect with negative corners could not be rendered"). Clamping yields a plain rectangle at 0.
 */
fun cornerShape(topLeft: Dp, topRight: Dp, bottomLeft: Dp, bottomRight: Dp) =
    RoundedCornerShape(
        topStart = topLeft.coerceAtLeast(0.dp),
        topEnd = topRight.coerceAtLeast(0.dp),
        bottomStart = bottomLeft.coerceAtLeast(0.dp),
        bottomEnd = bottomRight.coerceAtLeast(0.dp),
    )