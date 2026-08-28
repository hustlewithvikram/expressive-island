package com.vikram.expressiveisland.overlay.cards

/**
 * Identifies the type of expanded information card currently displayed
 * inside the Dynamic Island.
 *
 * Each card can have its own UI and its own height without affecting
 * ordinary notification cards.
 */
enum class ExpandedCardType {
    NOTIFICATION,
    MEDIA,
    CHARGING,
    WIFI,
    TIMER,
    ASSISTANT,
    CALL,
}

/**
 * Layout specification for an expanded event card.
 *
 * [heightDp] is nullable because some cards, such as the assistant,
 * calculate their height dynamically.
 */
data class ExpandedCardSpec(
    val type: ExpandedCardType,
    val heightDp: Int? = null,
)