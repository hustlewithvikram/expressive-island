package com.vikram.expressiveisland.overlay.contents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.core.ChargingState
import com.vikram.expressiveisland.core.HeadphonesBus
import com.vikram.expressiveisland.core.SystemEventType
import com.vikram.expressiveisland.core.WifiBus
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.service.ProgressData

@Composable
fun ExpandedContent(
    event: IslandEvent,
    systemEventType: SystemEventType?,
    chargingState: ChargingState?,
    showActions: Boolean,
    appearance: AppearanceSettings,
    collapsedHeightDp: Int,
    notchBottomInsetDp: Int = collapsedHeightDp,
    replyingTo: IslandAction?,
    replySent: Boolean,
    progressData: ProgressData? = null,
    onAction: (IslandAction) -> Unit,
    onStartReply: (IslandAction) -> Unit,
    onCancelReply: () -> Unit,
    onSendReply: (String) -> Unit,
    onDismiss: () -> Unit = {},
    onHeightMeasured: ((Int) -> Unit)? = null,
    onAssistantInputActiveChange: (Boolean) -> Unit = {},
    onMediaInteraction: () -> Unit = {},
    notificationTextExpanded: Boolean = false,
    onNotificationTextExpandedChange: (Boolean, Int) -> Unit = { _, _ -> },
    onAssistantPrompt: (String, (String) -> Unit) -> Unit = { _, complete -> complete("Assistant generation is unavailable.") },
    onNewAssistantChat: () -> Unit = {},
    onSelectAssistantChat: (String) -> Unit = {},
) {
    val wifiState by WifiBus.state.collectAsStateWithLifecycle()
    val headphonesState by HeadphonesBus.state.collectAsStateWithLifecycle()

    val currentWifiState = wifiState
    val currentHeadphonesState = headphonesState

    // The music tile has its own expanded layout (album art + playback controls).
    if (event.media != null) {
        MediaExpandedContent(
            event = event,
            buttonHeightDp = appearance.actionButtonHeightDp,
            collapsedHeightDp = collapsedHeightDp,
            onMediaInteraction = onMediaInteraction,
        )

        return
    }
    // The timer tile: icon + ticking remaining time, and its Reset / Add 1-min chips.
    if (event.timer != null) {
        TimerExpandedContent(
            event = event,
            appearance = appearance,
            collapsedHeightDp = collapsedHeightDp,
            onAction = onAction,
        )

        return
    }

    if (
        systemEventType == SystemEventType.CHARGING_STARTED &&
        chargingState != null
    ) {
        ChargingExpandedContent(
            charging = chargingState,
            appearance = appearance,
        )
        return
    }

    if (systemEventType == SystemEventType.BATTERY_LOW) {
        BatteryLowExpandedContent(
            appearance = appearance,
        )
        return
    }

    if (
        systemEventType == SystemEventType.WIFI_CONNECTED &&
        currentWifiState != null
    ) {
        WifiConnectedExpandedContent(
            wifi = currentWifiState,
            notchBottomInsetDp = notchBottomInsetDp,
        )
        return
    }

    if (
        systemEventType == SystemEventType.HEADPHONES_CONNECTED &&
        currentHeadphonesState != null
    ) {
        HeadphonesConnectedExpandedContent(
            headphones = currentHeadphonesState,
        )
        return
    }

    // All remaining device-level events receive a purpose-built, theme-aware detail card.
    if (systemEventType != null) {
        SystemEventExpandedContent(type = systemEventType)
        return
    }

    // The assistant tile: icon + text response with vertical scrolling.
    if (event.assistant != null) {
        AssistantExpandedContent(
            event = event,
            showActions = showActions,
            appearance = appearance,
            collapsedHeightDp = collapsedHeightDp,
            onDismiss = onDismiss,
            onHeightMeasured = onHeightMeasured,
            onInputFocusChange = onAssistantInputActiveChange,
            onAssistantPrompt = onAssistantPrompt,
            onNewAssistantChat = onNewAssistantChat,
            onSelectAssistantChat = onSelectAssistantChat,
        )
        return
    }

    // The generic notification expanded content ui
    NotificationExpandedContent(
        event = event,
        appearance = appearance,
        collapsedHeightDp = collapsedHeightDp,
        replyingTo = replyingTo,
        replySent = replySent,
        showActions = showActions,
        progressData = progressData,
        notificationTextExpanded = notificationTextExpanded,
        onNotificationTextExpandedChange = onNotificationTextExpandedChange,
        onAction = onAction,
        onStartReply = onStartReply,
        onCancelReply = onCancelReply,
        onSendReply = onSendReply,
    )

    return
}