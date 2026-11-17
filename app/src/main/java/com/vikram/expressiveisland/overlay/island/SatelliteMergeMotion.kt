package com.vikram.expressiveisland.overlay.island

import kotlin.math.sin

/**
 * Small, allocation-free motion curves for the secondary activity joining the main cutout.
 * The curves intentionally peak around contact and settle to zero so the pill never stays warped.
 */
internal fun satelliteMergeSwell(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    return sin(Math.PI.toFloat() * t)
}

/** A restrained stretch peaks just before contact and relaxes as the bubble is absorbed. */
internal fun satelliteMergeStretch(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    val rise = (t / 0.72f).coerceIn(0f, 1f)
    val fall = (1f - ((t - 0.72f) / 0.28f).coerceIn(0f, 1f))
    val easedRise = rise * rise * (3f - 2f * rise)
    return 0.32f * easedRise * fall
}

/** Keep the bubble visible during its approach; fade it only in the final part of the merge. */
internal fun satelliteMergeAlpha(progress: Float): Float {
    val t = ((progress.coerceIn(0f, 1f) - 0.78f) / 0.22f).coerceIn(0f, 1f)
    return 1f - t * t * (3f - 2f * t)
}
