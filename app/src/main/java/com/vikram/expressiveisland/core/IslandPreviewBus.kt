package com.vikram.expressiveisland.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates the settings preview shown by the real system overlay.
 * A settings page selects its content and expanded state; the overlay remains the renderer.
 */
enum class IslandPreviewCategory {
    APPEARANCE,
    SIZE_POSITION,
    BACKGROUND,
    ACTION_BUTTONS,
    ANIMATION,
    BEHAVIOUR,
    APPS,
    ASSISTANT,
    CAMERA_HOLE,
    DYNAMIC_TILES,
    MUSIC_TILE,
    PHONE_TILE,
    TIMER_TILE,
    EVENT_DETAIL,
    EVENT_ICONS,
    INTEGRATION,
    PERMISSION_DOTS,
    EMPTY_STATE,
    SHIZUKU,
}

data class IslandPreviewContent(
    val label: String,
    val detail: String,
)

object IslandPreviewBus {
    private val mutableActive = MutableStateFlow(false)
    val active: StateFlow<Boolean> = mutableActive
    private var activeOwner: Any? = null

    private val mutablePreviewHeightDp = MutableStateFlow(0)
    val previewHeightDp: StateFlow<Int> = mutablePreviewHeightDp

    private val mutableExpandedPreview = MutableStateFlow(false)
    val expandedPreview: StateFlow<Boolean> = mutableExpandedPreview

    private val mutableCategory = MutableStateFlow(IslandPreviewCategory.APPEARANCE)
    val category: StateFlow<IslandPreviewCategory> = mutableCategory

    private val mutableSystemEventType = MutableStateFlow<SystemEventType?>(null)
    val systemEventType: StateFlow<SystemEventType?> = mutableSystemEventType

    private val mutableContent = MutableStateFlow(
        IslandPreviewContent("Preview", "Settings preview")
    )
    val content: StateFlow<IslandPreviewContent> = mutableContent

    /** A screen claims the preview; stale screens cannot deactivate a newer screen's preview. */
    @Synchronized
    fun activate(owner: Any, enabled: Boolean) {
        activeOwner = owner
        mutableActive.value = enabled
    }

    @Synchronized
    fun release(owner: Any) {
        if (activeOwner === owner) {
            activeOwner = null
            mutableActive.value = false
            mutableExpandedPreview.value = false
            mutablePreviewHeightDp.value = 0
            mutableSystemEventType.value = null
        }
    }

    fun setPreviewHeightDp(value: Int) {
        mutablePreviewHeightDp.value = value.coerceAtLeast(0)
    }

    fun setExpandedPreview(value: Boolean) {
        mutableExpandedPreview.value = value
    }

    fun setCategory(value: IslandPreviewCategory) {
        mutableCategory.value = value
    }

    fun setSystemEventType(value: SystemEventType?) {
        mutableSystemEventType.value = value
    }

    fun setContent(label: String, detail: String) {
        mutableContent.value = IslandPreviewContent(label, detail)
    }
}
