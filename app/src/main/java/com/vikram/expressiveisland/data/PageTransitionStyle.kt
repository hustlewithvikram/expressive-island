package com.vikram.expressiveisland.data

/**
 * The transition used when switching between in-app pages.
 */
enum class PageTransitionStyle {
    FADE,
    SLIDE;

    companion object {
        fun deserialize(value: String?): PageTransitionStyle? =
            entries.firstOrNull { it.name == value }
    }
}