package com.vikram.expressiveisland.data

/**
 * The transition used when switching between in-app pages.
 * Names are persisted, so existing FADE and SLIDE preferences remain compatible.
 */
enum class PageTransitionStyle {
    FADE,
    SLIDE,
    SHARED_AXIS,
    FADE_THROUGH,
    SCALE_FADE,
    SLIDE_FADE;

    companion object {
        fun deserialize(value: String?): PageTransitionStyle? =
            entries.firstOrNull { it.name == value }
    }
}
