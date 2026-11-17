package com.vikram.expressiveisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Signals that the settings screen wants the real overlay pinned open so size/position/radius
 * changes can be seen live on the actual cutout. [expandedPreview] mirrors which tab the user
 * is editing, so the pinned island shows the matching (collapsed or expanded) state.
 */
enum class IslandPreviewCategory { APPEARANCE, SIZE_POSITION }

object IslandPreviewBus {

    private val mutableActive = MutableStateFlow(false)
    val active: StateFlow<Boolean> = mutableActive

    private val mutableExpandedPreview = MutableStateFlow(false)
    val expandedPreview: StateFlow<Boolean> = mutableExpandedPreview

    private val mutableCategory = MutableStateFlow(IslandPreviewCategory.APPEARANCE)
    val category: StateFlow<IslandPreviewCategory> = mutableCategory

    fun setActive(value: Boolean) {
        mutableActive.value = value
    }

    fun setExpandedPreview(value: Boolean) {
        mutableExpandedPreview.value = value
    }

    fun setCategory(value: IslandPreviewCategory) {
        mutableCategory.value = value
    }
}
