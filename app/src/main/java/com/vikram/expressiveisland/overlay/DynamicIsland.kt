package com.vikram.expressiveisland.overlay

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.core.ChargingBus
import com.vikram.expressiveisland.core.HeadphonesBus
import com.vikram.expressiveisland.core.OnCallBus
import com.vikram.expressiveisland.core.SystemEventType
import com.vikram.expressiveisland.data.ActionButtonAlignment
import com.vikram.expressiveisland.data.ActionButtonAnimation
import com.vikram.expressiveisland.data.AnimationBounce
import com.vikram.expressiveisland.data.AnimationSpeed
import com.vikram.expressiveisland.data.AnimationStyle
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.data.CALL_MIN_WIDTH_PERCENT
import com.vikram.expressiveisland.data.CenterShortcut
import com.vikram.expressiveisland.data.CutoutColor
import com.vikram.expressiveisland.data.IconSource
import com.vikram.expressiveisland.data.PillWidgetType
import com.vikram.expressiveisland.data.IslandDimensions
import com.vikram.expressiveisland.data.PermissionDotColors
import com.vikram.expressiveisland.data.PermissionDotPosition
import com.vikram.expressiveisland.data.SentAlignment
import com.vikram.expressiveisland.data.SwipeDismissDirection
import com.vikram.expressiveisland.data.SwipeDismissTarget
import com.vikram.expressiveisland.data.asCallCutout
import com.vikram.expressiveisland.overlay.contents.ACTIONS_ROW_SPACING_DP
import com.vikram.expressiveisland.overlay.contents.CallNormalContent
import com.vikram.expressiveisland.overlay.contents.CenterContent
import com.vikram.expressiveisland.overlay.contents.CollapsedContent
import com.vikram.expressiveisland.overlay.contents.SatelliteActivityRing
import com.vikram.expressiveisland.overlay.contents.EmptyPillContent
import com.vikram.expressiveisland.overlay.contents.ExpandedContent
import com.vikram.expressiveisland.overlay.contents.IslandSurface
import com.vikram.expressiveisland.overlay.contents.LocalActionButtonAnimation
import com.vikram.expressiveisland.overlay.contents.PressExpandDp
import com.vikram.expressiveisland.overlay.satellite.SatelliteBubble
import com.vikram.expressiveisland.overlay.contents.callCutoutWidthPercent
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.island.AnimationEngine
import com.vikram.expressiveisland.overlay.island.satelliteMergeAlpha
import com.vikram.expressiveisland.overlay.island.satelliteMergeStretch
import com.vikram.expressiveisland.overlay.island.satelliteMergeSwell
import com.vikram.expressiveisland.overlay.island.SATELLITE_GAP_DP
import com.vikram.expressiveisland.overlay.satellite.SatellitePosition
import com.vikram.expressiveisland.system.PermissionUsage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.unit.lerp as lerpDp


// How far the island must be dragged upward before a swipe-up collapses it.
private const val SWIPE_UP_SHRINK_THRESHOLD_DP = 24

// How far the island must be dragged sideways before releasing dismisses it.
private const val SWIPE_DISMISS_THRESHOLD_DP = 90

// How long the "reply sent" confirmation stays on screen before the reply is dispatched.
private const val REPLY_SENT_FEEDBACK_MS = 900L

/**
 * Extra height added to the expanded island when it shows action buttons, so the added row grows
 * downward instead of pushing the content up into the camera cutout: one chip row (at its configured
 * height) plus its spacing. The controller grows the host window by the same amount so it never clips.
 */
internal fun expandedActionsExtraDp(buttonHeightDp: Int): Int =
    buttonHeightDp + ACTIONS_ROW_SPACING_DP

/**
 * A safe upper bound on the height the expanded "center" claims below the base expanded cutout. The
 * visible island fits its measured content exactly (see the height-bonus logic in [DynamicIsland]);
 * the controller reserves this for the host window and touchable region so they never clip the
 * tallest (labels-on) layout — the window being a touch taller than the content is invisible.
 */
internal const val CENTER_SHORTCUTS_EXTRA_DP = 135

// Gap between the camera cutout (cleared by a collapsed-pill-height band at the top) and the center's
// content, used when fitting the island height to its measured shortcut row.
private const val CENTER_TOP_GAP_DP = 8

/**
 * Maps the configured chip placement onto the [Row] arrangement that positions the chip row.
 * [ActionButtonAlignment.FULL] stretches the chips with weight rather than positioning them, so it
 * falls back to leading here (the arrangement is irrelevant once the chips fill the whole width).
 */
internal fun ActionButtonAlignment.toHorizontal(): Alignment.Horizontal = when (this) {
    ActionButtonAlignment.LEFT, ActionButtonAlignment.FULL -> Alignment.Start
    ActionButtonAlignment.CENTER -> Alignment.CenterHorizontally
    ActionButtonAlignment.RIGHT -> Alignment.End
}

/** Maps the configured "Sent" confirmation placement onto its [Row] arrangement. */
internal fun SentAlignment.toHorizontal(): Alignment.Horizontal = when (this) {
    SentAlignment.LEFT -> Alignment.Start
    SentAlignment.CENTER -> Alignment.CenterHorizontally
    SentAlignment.RIGHT -> Alignment.End
}

/**
 * The interactive overlay island. The hosting window is a fixed size; the island's size,
 * position and corners are all animated here in Compose, so expand/collapse never resizes the
 * window (which caused per-frame relayout jank). Tapping toggles expanded; [forcedExpanded]
 * locks the state (used by the settings preview).
 */
@Composable
internal fun DynamicIsland(
    event: IslandEvent?,
    systemEventType: SystemEventType? = null,
    collapsed: IslandDimensions,
    expanded: IslandDimensions,
    displayWidthDp: Int,
    forcedExpanded: Boolean?,
    isStickToCamera: Boolean = false,
    isRotation270: Boolean = false,
    offsetYDp: Int = 6,
    animationStyle: AnimationStyle,
    animationSpeed: AnimationSpeed,
    animationBounce: AnimationBounce,
    actionButtonAnimation: ActionButtonAnimation,
    animationDurationMs: Int,
    autoCollapse: Boolean,
    autoCollapseMs: Long,
    appearance: AppearanceSettings,
    showActions: Boolean,
    shrinkOnSwipeUp: Boolean,
    swipeToDismiss: Boolean,
    swipeDismissDirection: SwipeDismissDirection,
    swipeDismissTarget: SwipeDismissTarget,
    showsWhenEmpty: Boolean,
    emptyIcon: IconSource? = null,
    emptyIconColor: CutoutColor? = null,
    persistentPillLeftWidget: PillWidgetType = PillWidgetType.NONE,
    persistentPillRightWidget: PillWidgetType = PillWidgetType.NONE,
    emptyOpensCenter: Boolean = false,
    assistantTapEnabled: Boolean = false,
    onAssistantTap: () -> Unit = {},
    onAssistantPrompt: (String, (String) -> Unit) -> Unit = { _, complete -> complete("Assistant generation is unavailable.") },
    onNewAssistantChat: () -> Unit = {},
    onSelectAssistantChat: (String) -> Unit = {},
    cameraHoleTapEnabled: Boolean = false,
    cameraHoleTapHaptics: Boolean = true,
    cameraHoleQuickActions: Boolean = false,
    onCameraHoleQuickActionsDismiss: () -> Unit = {},
    onCameraHoleTap: () -> Unit = {},
    centerShortcuts: List<CenterShortcut> = emptyList(),
    centerShowLabels: Boolean = true,
    centerFillContainers: Boolean = false,
    centerThemedIcons: Boolean = false,
    vibrateOnTap: Boolean = true,
    hapticsOnPop: Boolean = false,
    permissionDotsEnabled: Boolean = false,
    permissionUsage: PermissionUsage = PermissionUsage(),
    permissionDotPosition: PermissionDotPosition = PermissionDotPosition.RIGHT,
    permissionDotColors: PermissionDotColors = PermissionDotColors(),
    permissionDotsVertical: Boolean = false,
    onEmptyClick: () -> Unit = {},
    onCenterShortcut: (CenterShortcut) -> Unit = {},
    onExpandedChange: (Boolean) -> Unit,
    onActivate: () -> Unit,
    onAction: (IslandAction) -> Unit,
    onReply: (IslandAction, String) -> Unit,
    onReplyActiveChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onAssistantContentHeightChanged: (Long?, Int) -> Unit = { _, _ -> },
    collapseRequest: Int = 0,
    notificationTextExpanded: Boolean = false,
    notificationTextExtraHeightDp: Int = 0,
    onNotificationTextExpandedChange: (Boolean, Int) -> Unit = { _, _ -> },
    satellite: IslandEvent? = null,
    satellitePosition: SatellitePosition = SatellitePosition.RIGHT,
    onSatelliteClick: () -> Unit = {},
) {
    var lastEvent by remember { mutableStateOf<IslandEvent?>(null) }
    if (event != null) {
        lastEvent = event
    }

    val shownEvent = lastEvent
    // Keep the system-event renderer selected for the entire exit transition, even after
    // the controller clears its active event type as part of dismissing the pill.
    val displayedSystemEventType = systemEventType ?: shownEvent?.systemEventType
    val chargingState by ChargingBus.state.collectAsStateWithLifecycle()
    val chargingPreviewState by ChargingBus.previewState.collectAsStateWithLifecycle()
    val displayedChargingState = chargingPreviewState ?: chargingState
    val headphonesState by HeadphonesBus.state.collectAsStateWithLifecycle()
    val mediaActive = shownEvent?.media != null
    val emptyPill = event == null && (showsWhenEmpty || cameraHoleQuickActions)

    val initialExpandedState =
        if (forcedExpanded == false) false else (shownEvent?.initiallyExpanded ?: false)
    //var tapExpanded by remember(shownEvent?.id, forcedExpanded) { mutableStateOf(initialExpandedState) }

    val expansionKey = if (shownEvent?.media != null) {
        "media"
    } else {
        shownEvent?.id
    }

    var tapExpanded by remember(expansionKey, forcedExpanded) {
        mutableStateOf(initialExpandedState)
    }

    LaunchedEffect(cameraHoleQuickActions) {
        if (cameraHoleQuickActions) tapExpanded = true
    }

    var centerInteraction by remember { mutableIntStateOf(0) }
    var mediaInteraction by remember { mutableIntStateOf(0) }
    var replyingTo by remember(shownEvent?.id) { mutableStateOf<IslandAction?>(null) }
    val replying = replyingTo != null
    var assistantInputActive by remember(shownEvent?.id) { mutableStateOf(false) }
    var sentReply by remember(shownEvent?.id) { mutableStateOf<Pair<IslandAction, String>?>(null) }
    val confirmingSent = sentReply != null
    val isCall = shownEvent?.call != null
    val isAssistantNormalOnly =
        shownEvent?.assistant != null && !shownEvent.assistant.displayAnswerInCutout
    val isNormalOnly = isCall || isAssistantNormalOnly || shownEvent?.normalOnly == true
    val centerExpanded = emptyPill && (emptyOpensCenter || cameraHoleQuickActions) && tapExpanded

    val isExpanded = when {
        forcedExpanded == false -> false
        emptyPill && !mediaActive -> centerExpanded
        isNormalOnly -> false
        else -> forcedExpanded ?: tapExpanded
    }

    val boopScale = remember { Animatable(1f) }
    val pressExpand = remember { Animatable(0f) }
    val pressWidens = actionButtonAnimation == ActionButtonAnimation.EXPAND
    val dismissOffsetX = remember(shownEvent?.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val motion = remember(animationStyle, animationSpeed, animationBounce, animationDurationMs) {
        AnimationEngine(animationStyle, animationSpeed, animationBounce, animationDurationMs)
    }

    LaunchedEffect(replying) { onReplyActiveChange(replying) }
    LaunchedEffect(isExpanded, event != null, emptyPill, emptyOpensCenter) {
        if (event != null || (emptyPill && emptyOpensCenter)) onExpandedChange(isExpanded)
    }

    LaunchedEffect(collapseRequest) {
        if (collapseRequest > 0 && forcedExpanded == null && !replying) {
            tapExpanded = false
        }
    }

    LaunchedEffect(
        tapExpanded,
        forcedExpanded,
        autoCollapse,
        autoCollapseMs,
        replying,
        confirmingSent,
        centerInteraction,
        mediaInteraction,
        cameraHoleQuickActions,
        assistantInputActive,
    ) {
        if (forcedExpanded == null && tapExpanded && autoCollapse && !replying && !confirmingSent && !assistantInputActive) {
            delay(autoCollapseMs.milliseconds)
            tapExpanded = false
            if (cameraHoleQuickActions) onCameraHoleQuickActionsDismiss()
        }
    }

    val hasActions = showActions && (shownEvent?.actions?.isNotEmpty() == true)
    val hasMediaControls = shownEvent?.media?.showControls == true
    val hasCallActions = shownEvent?.call?.showActions == true && shownEvent.actions.isNotEmpty()
    val hasTimerActions = shownEvent?.timer?.showActions == true && shownEvent.actions.isNotEmpty()
    val liveCall by OnCallBus.state.collectAsStateWithLifecycle()
    val callIncoming = isCall && liveCall?.ongoing == false
    val callTwoRow = false
    val callTrailingButtons = when {
        !isCall || !hasCallActions -> 0
        callTwoRow -> 0
        callIncoming -> 2
        else -> 1
    }

    val density = LocalDensity.current.density
    val callWidthPercent = remember(
        isCall,
        shownEvent?.label,
        callTrailingButtons,
        callIncoming,
        displayWidthDp,
        density
    ) {
        if (isCall) {
            callCutoutWidthPercent(
                shownEvent.label,
                callTrailingButtons,
                callIncoming,
                displayWidthDp,
                density
            )
        } else {
            CALL_MIN_WIDTH_PERCENT
        }
    }

    val dims = when {
        emptyPill && !isExpanded -> collapsed
        callTwoRow -> expanded
        isCall -> collapsed.asCallCutout(callWidthPercent)
        isExpanded -> expanded
        else -> collapsed
    }

    var assistantContentHeightDp by remember(shownEvent?.id) { mutableIntStateOf(0) }
    var centerContentHeightDp by remember { mutableIntStateOf(0) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    // System events always use the exact expanded height configured in Size & Position.
    // Their content must adapt inside that fixed shell instead of growing it per event type.
    val heightBonus = if (displayedSystemEventType != null) {
        0
    } else {
        when {
            emptyPill && isExpanded -> {
                if (centerContentHeightDp > 0) {
                    collapsed.heightDp + CENTER_TOP_GAP_DP + centerContentHeightDp - dims.heightDp
                } else {
                    CENTER_SHORTCUTS_EXTRA_DP
                }
            }

            emptyPill -> 0

            isCall && shownEvent?.call?.incomingExpandedLayout == true &&
                    (liveCall?.ongoing == false || shownEvent.id == -1L) &&
                    shownEvent.call.showActions && shownEvent.actions.isNotEmpty() ->
                com.vikram.expressiveisland.overlay.contents.callIncomingExtraDp()

            isExpanded && shownEvent?.assistant != null && shownEvent.assistant.displayAnswerInCutout -> {
                val maxCutoutHeightDp =
                    (screenHeightDp * shownEvent.assistant.maxCutoutHeightPercent / 100)
                val fitHeightDp = if (assistantContentHeightDp > 0) assistantContentHeightDp else maxCutoutHeightDp
                val targetHeightDp = fitHeightDp.coerceIn(110, maxCutoutHeightDp)
                (targetHeightDp - dims.heightDp)
            }

            isExpanded && (hasActions || hasMediaControls || hasCallActions || hasTimerActions) ->
                expandedActionsExtraDp(appearance.actionButtonHeightDp)

            else -> 0
        } + if (isExpanded && notificationTextExpanded) notificationTextExtraHeightDp else 0
    }

    val present = event != null || showsWhenEmpty || cameraHoleQuickActions
    // Keep the shell mounted while it travels from its expanded geometry back into the
    // camera-notch footprint. This is separate from reveal: reveal controls visibility,
    // while shellProgress controls the actual size/position/corner morph.
    val shellProgress by animateFloatAsState(
        targetValue = if (present) 1f else 0f,
        animationSpec = motion.shrink(),
        label = "islandExitShrink",
    )
    val exitProgress = 1f - shellProgress
    val reveal = remember { Animatable(0f) }
    val arrivalIconAlpha = remember { Animatable(1f) }
    val arrivalScaleX = remember { Animatable(1f) }

    // Start the horizontal overshoot at the same time as the shell reveal. The pill reaches
    // 110% around the end of its medium-speed expansion, then eases gently back to 100%.
    LaunchedEffect(event?.id, event?.notificationKey, isExpanded) {
        arrivalIconAlpha.snapTo(if (event?.notificationKey == null || isExpanded) 1f else 0f)
        arrivalScaleX.snapTo(1f)
        if (event?.notificationKey != null && !isExpanded) {
            launch {
                arrivalScaleX.animateTo(
                    targetValue = 1.10f,
                    animationSpec = tween(
                        durationMillis = 260,
                        easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
                    ),
                )
                arrivalScaleX.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 468,
                        easing = CubicBezierEasing(0f, 0f, 0.2f, 1f),
                    ),
                )
            }
            // Keep the icon hidden until the pill itself has completed its centre-out reveal.
            snapshotFlow { reveal.value }.first { it >= 0.98f }
            arrivalIconAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 240,
                    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
                ),
            )
        }
    }

    LaunchedEffect(present) {
        reveal.animateTo(
            targetValue = if (present) 1f else 0f,
            animationSpec = motion.reveal(present),
        )
    }

    LaunchedEffect(emptyPill) {
        if (emptyPill && !cameraHoleQuickActions) {
            tapExpanded = false
            if (dismissOffsetX.value != 0f) {
                reveal.snapTo(0f)
                dismissOffsetX.snapTo(0f)
                reveal.animateTo(1f, animationSpec = motion.reveal(entering = true))
            }
        }
    }

    val spec: AnimationSpec<Dp> = if (reveal.value == 0f) snap() else motion.dp()
    val isAssistantAnswer = isExpanded && shownEvent?.assistant?.displayAnswerInCutout == true
    // Assistant responses can grow repeatedly while streaming. Use a critically damped height
    // spring so each incremental measurement is followed smoothly without a visible bob.
    val heightSpec: AnimationSpec<Dp> = if (isAssistantAnswer) motion.dpSmooth() else spec

    // The bubble shares the normal cutout's total width instead of making the island wider.
    val satelliteCandidate = satellite != null && !isExpanded && !isCall && !isStickToCamera
    val requestedSatelliteSplitDp = collapsed.heightDp + SATELLITE_GAP_DP
    val remainingSatelliteWidthDp =
        displayWidthDp * (collapsed.widthPercent / 100f) - requestedSatelliteSplitDp
    val satelliteSharing = satelliteCandidate &&
            remainingSatelliteWidthDp >= requestedSatelliteSplitDp &&
            remainingSatelliteWidthDp >= collapsed.heightDp * 2
    val satelliteSplitDp = if (satelliteSharing) requestedSatelliteSplitDp else 0
    val satelliteShiftDp = when {
        !satelliteSharing -> 0f
        satellitePosition == SatellitePosition.LEFT -> satelliteSplitDp / 2f
        else -> -satelliteSplitDp / 2f
    }

    val width by animateDpAsState(
        if (isStickToCamera) collapsed.heightDp.dp
        else (displayWidthDp * dims.widthPercent / 100f).dp - satelliteSplitDp.dp,
        spec, label = "islandWidth"
    )

    val height by animateDpAsState(
        if (isStickToCamera) {
            (displayWidthDp * dims.widthPercent / 100f).dp
        } else {
            (dims.heightDp + heightBonus + if (isExpanded && shownEvent?.media != null) 20 else 0).dp
        },
        heightSpec,
        label = "islandHeight"
    )

    val cornerRadius = (collapsed.heightDp / 2f).dp
    val offsetX by animateDpAsState(
        if (isStickToCamera) 0.dp else dims.offsetXDp.dp + satelliteShiftDp.dp,
        spec,
        label = "islandOffsetX",
    )
    val offsetY by animateDpAsState(
        if (isStickToCamera) 0.dp else dims.offsetYDp.dp,
        spec,
        label = "islandOffsetY"
    )
    val topLeft by animateDpAsState(
        if (isStickToCamera) cornerRadius else dims.cornerTopLeftDp.dp,
        spec,
        label = "cornerTL"
    )
    val topRight by animateDpAsState(
        if (isStickToCamera) cornerRadius else dims.cornerTopRightDp.dp,
        spec,
        label = "cornerTR"
    )
    val bottomLeft by animateDpAsState(
        if (isStickToCamera) cornerRadius else dims.cornerBottomLeftDp.dp,
        spec,
        label = "cornerBL"
    )
    val bottomRight by animateDpAsState(
        if (isStickToCamera) cornerRadius else dims.cornerBottomRightDp.dp,
        spec,
        label = "cornerBR"
    )
    val expandProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = motion.fade(),
        label = "islandBackgroundFade",
    )
    // Empty-pill center expansion has no event arrival/removal to drive the reveal animation.
    // Give it the same centre-out shell morph used by notification expansion.
    val centerMorph by animateFloatAsState(
        targetValue = if (emptyPill && isExpanded) 1f else 0f,
        animationSpec = motion.float(),
        label = "persistentPillCenterMorph",
    )

    val dotDp = collapsed.heightDp.dp
    val shellMorph = when {
        isStickToCamera -> 1f
        emptyPill && isExpanded -> centerMorph
        else -> shellProgress
    }
    val revealWidth = lerpDp(dotDp, width, shellMorph)
    val revealHeight = lerpDp(dotDp, height, shellMorph)
    // During exit, the expanded shell returns to the configured collapsed anchor as well as
    // shrinking. Without this interpolation it can shrink in place, away from the camera notch.
    val shellOffsetX = if (isStickToCamera) 0.dp
        else lerpDp(collapsed.offsetXDp.dp, offsetX, shellMorph)
    val shellOffsetY = if (isStickToCamera) 0.dp
        else lerpDp(collapsed.offsetYDp.dp, offsetY, shellMorph)
    val exitBlur = (exitProgress.coerceIn(0f, 1f) * 2f).dp
    val dotCorner = dotDp / 2
    val revealTopLeft = lerpDp(dotCorner, topLeft, shellMorph)
    val revealTopRight = lerpDp(dotCorner, topRight, shellMorph)
    val revealBottomLeft = lerpDp(dotCorner, bottomLeft, shellMorph)
    val revealBottomRight = lerpDp(dotCorner, bottomRight, shellMorph)

    var lastSatellite by remember { mutableStateOf<IslandEvent?>(null) }
    if (satellite != null) lastSatellite = satellite
    val satelliteShown = satellite != null && present && !isCall && !isStickToCamera
    val satelliteReveal = remember { Animatable(0f) }
    val satelliteMerge = remember { Animatable(0f) }
    LaunchedEffect(satelliteShown, satellite?.id, isExpanded, isCall, isStickToCamera, event?.id) {
        if (satelliteShown) {
            satelliteMerge.snapTo(0f)
            satelliteReveal.animateTo(1f, animationSpec = motion.reveal(entering = true))
        } else {
            // When the secondary activity is promoted into the main pill, pull it into the pill
            // before fading it. Hiding it immediately makes the transition look like a pop-out.
            val canMerge = satellite == null && event != null && present &&
                !isExpanded && !isCall && !isStickToCamera && lastSatellite != null
            if (canMerge) {
                satelliteMerge.animateTo(1f, animationSpec = motion.merge())
                satelliteReveal.animateTo(0f, animationSpec = motion.reveal(entering = false))
                satelliteMerge.snapTo(0f)
                lastSatellite = null
            } else {
                satelliteMerge.snapTo(0f)
                satelliteReveal.animateTo(0f, animationSpec = motion.reveal(entering = false))
                lastSatellite = null
            }
        }
    }

    val haptic = LocalHapticFeedback.current

    // The dots belong on the collapsed pill only: the expanded card keeps its top clear for the
    // camera, the call cutout already fills its trailing edge with the hang-up button, and the
    // stuck-to-camera pill is barely wider than its own icon.
    // Mounted for as long as the feature is on rather than only while something is in use, so each
    // dot fades in and out with its own resource instead of appearing the instant the row exists.
    val showPermissionDots = permissionDotsEnabled && !isExpanded && !isCall && !isStickToCamera
    val permissionDotsOnLeft = permissionDotPosition == PermissionDotPosition.LEFT
    val collapsedTrailingInsetDp = if (showPermissionDots && !permissionDotsOnLeft) {
        permissionDotRowWidthDp(permissionUsage, collapsed.heightDp, permissionDotsVertical)
    } else {
        0
    }

    val hasEvent = event != null
    var lastHasEvent by remember { mutableStateOf(hasEvent) }
    LaunchedEffect(hasEvent) {
        if (hapticsOnPop && hasEvent != lastHasEvent) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastHasEvent = hasEvent
    }

    CompositionLocalProvider(LocalActionButtonAnimation provides actionButtonAnimation) {
        Box(modifier = Modifier.fillMaxSize()) {
            val stickAlignment = if (isRotation270) Alignment.CenterEnd else Alignment.CenterStart
            val stickPaddingStart = if (isStickToCamera && !isRotation270) offsetYDp.dp else 0.dp
            val stickPaddingEnd = if (isStickToCamera && isRotation270) offsetYDp.dp else 0.dp

            // Transparent hit target for actions assigned to the physical camera-hole area.
            // It is deliberately separate from the visible Empty Island pill and only receives
            // taps while the island has no event and no resting pill is being displayed.
            if (event == null &&
                (assistantTapEnabled || cameraHoleTapEnabled) &&
                !showsWhenEmpty &&
                !cameraHoleQuickActions &&
                !isStickToCamera
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = offsetX, y = offsetY)
                        .width(width)
                        .height(collapsed.heightDp.dp)
                        .pointerInput(assistantTapEnabled, cameraHoleTapEnabled, cameraHoleTapHaptics) {
                            detectTapGestures(
                                onTap = {
                                    when {
                                        assistantTapEnabled -> onAssistantTap()
                                        cameraHoleTapEnabled -> {
                                            if (cameraHoleTapHaptics) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            onCameraHoleTap()
                                        }
                                    }
                                }
                            )
                        },
                )
            }

            Box(
                modifier = Modifier
                    .align(if (isStickToCamera) stickAlignment else Alignment.TopCenter)
                    .padding(start = stickPaddingStart, end = stickPaddingEnd)
                    .offset(
                        x = shellOffsetX,
                        y = shellOffsetY
                    ),
            ) {
                if (present || shellProgress > 0f || assistantTapEnabled) {
                    IslandSurface(
                        modifier = Modifier
                            .width(revealWidth)
                            .height(revealHeight)
                            .blur(exitBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .graphicsLayer {
                                // The spring overshoot intentionally extends beyond the pill's resting
                                // bounds. Do not clip that transformed layer to its rectangular layout.
                                clip = false
                                val extraPx = PressExpandDp.toPx() * 2f * pressExpand.value
                                val widen =
                                    if (size.width > 0f) (size.width + extraPx) / size.width else 1f
                                val mergeSwell = satelliteMergeSwell(satelliteMerge.value)
                                val travel =
                                    (abs(dismissOffsetX.value) / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
                                // Keep the shell physically connected to the finger: a slight tilt and
                                // cross-axis squash communicate drag force, then the shared spring settle
                                // carries the pill back without snapping its rotation or proportions.
                                scaleX = boopScale.value * widen * arrivalScaleX.value *
                                    (1f + 0.055f * mergeSwell - 0.045f * travel)
                                scaleY = boopScale.value *
                                    (1f + 0.035f * mergeSwell + 0.075f * travel)
                                rotationZ = (dismissOffsetX.value / size.width.coerceAtLeast(1f) * 7f)
                                    .coerceIn(-7f, 7f)
                                translationX = dismissOffsetX.value
                                // Reveal alpha is for arrival only. On exit, keep the shell
                                // visible until its geometry reaches the notch instead of letting
                                // the reveal fade hide the unfinished shrink.
                                val revealAlpha = if (present) {
                                    (reveal.value / 0.2f).coerceIn(0f, 1f)
                                } else {
                                    1f
                                }
                                // A small 5% opacity remnant at the end of the geometric
                                // collapse softens the disappearance; revealAlpha then removes
                                // the retained surface after the shrink animation completes.
                                val collapseAlpha = 1f - exitProgress.coerceIn(0f, 1f) * 0.95f
                                alpha = (1f - travel * 0.75f).coerceIn(0.25f, 1f) *
                                    collapseAlpha * revealAlpha
                            }
                            .pointerInput(
                                forcedExpanded,
                                isExpanded,
                                replying,
                                emptyPill,
                                pressWidens,
                                shownEvent?.id
                            ) {
                                if (forcedExpanded == true) {
                                    return@pointerInput
                                }

                                detectTapGestures(
                                    onPress = {
                                        if (replying) {
                                            return@detectTapGestures
                                        }

                                        if (!isExpanded && !emptyPill) {
                                            scope.launch {
                                                if (pressWidens) {
                                                    pressExpand.animateTo(1f, motion.boop())
                                                } else {
                                                    // Empty cutout scale tap animation
                                                    boopScale.animateTo(0.96f, motion.boop())
                                                }
                                            }
                                        }

                                        tryAwaitRelease()

                                        if (!isExpanded && !emptyPill) {
                                            scope.launch {
                                                if (pressWidens) {
                                                    pressExpand.animateTo(0f, motion.boop())
                                                } else {
                                                    boopScale.animateTo(1f, motion.boop())
                                                }
                                            }
                                        }
                                    },

                                    // Long press:
                                    // - If the current event has an app/content intent, open it.
                                    // - Otherwise, use the long press as a fallback to expand the island.
                                    onLongPress = {
                                        if (replying) {
                                            return@detectTapGestures
                                        }

                                        if (vibrateOnTap) {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress,
                                            )
                                        }

                                        if (shownEvent?.contentIntent != null) {
                                            // Long-press opens the notification's/application's content.
                                            tapExpanded = false
                                            onActivate()
                                        } else if (forcedExpanded == null) {
                                            // No application/content intent available.
                                            // Use long press as a fallback expansion gesture.
                                            tapExpanded = true

                                            // The empty-pill center expands through the shared
                                            // island size and corner animations, without a separate pop.
                                        }
                                    },
                                    onTap = {
                                        if (vibrateOnTap) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }

                                        if (event == null && assistantTapEnabled && !emptyPill) {
                                            onAssistantTap()
                                            return@detectTapGestures
                                        }

                                        if (emptyPill) {
                                            // "Open center" expands the resting pill into the shortcut
                                            // grid (a second tap toggles it closed); every other "On
                                            // click" action (e.g. open an app) runs via onEmptyClick.
                                            if (emptyOpensCenter) {
                                                if (forcedExpanded == null) {
                                                    val wasExpanded = tapExpanded
                                                    tapExpanded = !tapExpanded
                                                    if (wasExpanded && cameraHoleQuickActions) {
                                                        onCameraHoleQuickActionsDismiss()
                                                    }
                                                    // The empty-pill center uses the same width/height
                                                    // transition as notification expansion; avoid an
                                                    // extra scale pop that makes the surface feel detached.
                                                }
                                            } else {
                                                onEmptyClick()
                                            }
                                            return@detectTapGestures
                                        }

                                        // While typing a reply, ignore taps on the surface itself.
                                        if (replying) return@detectTapGestures

                                        // The phone tile is normal-only, so a tap never toggles it open;
                                        // instead it opens the dialer's in-call screen (its content intent).
                                        if (isNormalOnly) {
                                            if (shownEvent.contentIntent != null) onActivate()
                                            return@detectTapGestures
                                        }

                                        // Tap to open the app
                                        if ((isExpanded || forcedExpanded == false) && shownEvent?.contentIntent != null) {
                                            tapExpanded = false
                                            onActivate()
                                        } else if (forcedExpanded == null) {
                                            tapExpanded = !tapExpanded
                                            if (isExpanded) {
                                                scope.launch {
                                                    motion.pop(boopScale, peak = 1.02f)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            // Swipe up on the expanded island to shrink it back to the normal cutout.
                            .pointerInput(
                                forcedExpanded,
                                isExpanded,
                                replying,
                                shrinkOnSwipeUp,
                                emptyPill,
                                shownEvent?.id
                            ) {
                                // The resting empty cutout has no expanded state to shrink back from, so
                                // don't install the detector at all — it would only swallow vertical drags.
                                if (shownEvent?.assistant?.interactivePanel == true || forcedExpanded != null || !shrinkOnSwipeUp || emptyPill) return@pointerInput
                                val threshold = SWIPE_UP_SHRINK_THRESHOLD_DP.dp.toPx()
                                var dragTotal = 0f
                                detectVerticalDragGestures(
                                    onDragStart = { dragTotal = 0f },
                                    onDragEnd = {
                                        if (isExpanded && !replying && dragTotal <= -threshold) {
                                            tapExpanded = false
                                        }
                                    },
                                ) { change, dragAmount ->
                                    dragTotal += dragAmount
                                    change.consume()
                                }
                            }
                            // Swipe sideways to dismiss the cutout (and, for a notification, clear it from
                            // the system). Only the direction(s) and cutout state(s) the user allows lets go.
                            .pointerInput(
                                forcedExpanded,
                                swipeToDismiss,
                                swipeDismissDirection,
                                swipeDismissTarget,
                                isExpanded,
                                replying,
                                emptyPill,
                                shownEvent?.id
                            ) {
                                val targetAllows = when (swipeDismissTarget) {
                                    SwipeDismissTarget.BOTH -> true
                                    SwipeDismissTarget.EXPANDED -> isExpanded
                                    SwipeDismissTarget.NORMAL -> !isExpanded
                                }
                                // The resting empty cutout is meant to stay: a swipe must neither slide it
                                // away nor clear the departed notification it still remembers.
                                if (forcedExpanded != null || !swipeToDismiss || replying || emptyPill || !targetAllows) return@pointerInput
                                val allowLeft = swipeDismissDirection != SwipeDismissDirection.RIGHT
                                val allowRight = swipeDismissDirection != SwipeDismissDirection.LEFT
                                val threshold = SWIPE_DISMISS_THRESHOLD_DP.dp.toPx()
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        val x = dismissOffsetX.value
                                        val dismiss =
                                            (x <= -threshold && allowLeft) || (x >= threshold && allowRight)
                                        if (dismiss) {
                                            onDismiss()
                                        } else {
                                            scope.launch {
                                                dismissOffsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = motion.gestureSettle(),
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = { scope.launch { dismissOffsetX.animateTo(0f) } },
                                ) { change, dragAmount ->
                                    // Clamp to the allowed direction(s) so a disabled side can't be dragged.
                                    val next = (dismissOffsetX.value + dragAmount).let {
                                        when {
                                            !allowLeft -> it.coerceAtLeast(0f)
                                            !allowRight -> it.coerceAtMost(0f)
                                            else -> it
                                        }
                                    }
                                    scope.launch { dismissOffsetX.snapTo(next) }
                                    change.consume()
                                }
                            },
                        shape = cornerShape(
                            revealTopLeft,
                            revealTopRight,
                            revealBottomLeft,
                            revealBottomRight
                        ),
                        appearance = appearance,
                        progress = expandProgress,
                        appColor = shownEvent?.primaryColor(),
                    ) {
                        Crossfade(
                            targetState = isExpanded,
                            animationSpec = motion.contentTransition(),
                            label = "islandContent"
                        ) { showExpanded ->
                            if (emptyPill) {
                                if (showExpanded) {
                                    CenterContent(
                                        shortcuts = centerShortcuts,
                                        showLabels = centerShowLabels,
                                        fillContainers = centerFillContainers,
                                        themedIcons = centerThemedIcons,
                                        onContentHeight = { centerContentHeightDp = it },
                                        onShortcut = { shortcut ->
                                            // Any press counts as activity, restarting the auto-collapse
                                            // timer so the center stays up while it's being used.
                                            centerInteraction++
                                            // In-place toggles (torch) keep the center open; everything
                                            // else closes it as we act, so it isn't left over the screen
                                            // (and out of a screenshot the shortcut may trigger).
                                            if (!shortcut.keepsCenterOpen) tapExpanded = false
                                            onCenterShortcut(shortcut)
                                        },
                                    )
                                } else if (emptyIcon != null || persistentPillLeftWidget != PillWidgetType.NONE || persistentPillRightWidget != PillWidgetType.NONE) {
                                    EmptyPillContent(
                                        icon = emptyIcon,
                                        containerColor = emptyIconColor,
                                        leftWidget = persistentPillLeftWidget,
                                        rightWidget = persistentPillRightWidget,
                                        heightDp = collapsed.heightDp,
                                        isStickToCamera = isStickToCamera,
                                    )
                                }
                            } else {
                                shownEvent?.let { e ->
                                    if (e.call != null) {
                                        CallNormalContent(event = e, onAction = onAction)
                                    } else if (showExpanded) {
                                        ExpandedContent(
                                            event = e,
                                            systemEventType = displayedSystemEventType,
                                            chargingState = displayedChargingState,
                                            showActions = showActions,
                                            appearance = appearance,
                                            collapsedHeightDp = collapsed.heightDp,
                                            notchBottomInsetDp = (
                                                collapsed.offsetYDp + collapsed.heightDp - expanded.offsetYDp + 4
                                            ).coerceAtLeast(0),
                                            replyingTo = replyingTo,
                                            replySent = confirmingSent,
                                            progressData = e.progressData,
                                            onAction = onAction,
                                            onStartReply = { replyingTo = it },
                                            onCancelReply = { replyingTo = null },
                                            onSendReply = { text ->
                                                replyingTo?.let { action ->
                                                    sentReply = action to text
                                                    scope.launch {
                                                        delay(REPLY_SENT_FEEDBACK_MS.milliseconds)
                                                        onReply(action, text)
                                                    }
                                                }
                                                replyingTo = null
                                            },
                                            onDismiss = onDismiss,
                                            notificationTextExpanded = notificationTextExpanded,
                                            onNotificationTextExpandedChange = onNotificationTextExpandedChange,
                                            onAssistantPrompt = onAssistantPrompt,
                                            onNewAssistantChat = onNewAssistantChat,
                                            onSelectAssistantChat = onSelectAssistantChat,
                                            onHeightMeasured = { heightDp ->
                                                // Measurements can become smaller when the overlay window
                                                // contracts. Never feed that clipped viewport back as the
                                                // content's natural height; only grow to a newly observed size.
                                                if (heightDp > assistantContentHeightDp) {
                                                    assistantContentHeightDp = heightDp
                                                    onAssistantContentHeightChanged(e.id, heightDp)
                                                }
                                            },
                                            onAssistantInputActiveChange = { active ->
                                                assistantInputActive = active
                                                onReplyActiveChange(active)
                                            },
                                            onMediaInteraction = { mediaInteraction++ },
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer {
                                                    alpha = if (e.notificationKey != null) arrivalIconAlpha.value else 1f
                                                },
                                        ) {
                                            CollapsedContent(
                                                event = e,
                                                heightDp = collapsed.heightDp,
                                                isStickToCamera = isStickToCamera,
                                                trailingInsetDp = collapsedTrailingInsetDp,
                                                satelliteSharing = satelliteShown && satelliteSharing,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Microphone / camera / location dots
                        if (showPermissionDots) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                PermissionDotRow(
                                    usage = permissionUsage,
                                    heightDp = collapsed.heightDp,
                                    colors = permissionDotColors,
                                    vertical = permissionDotsVertical,
                                    modifier = Modifier
                                        .align(
                                            if (permissionDotsOnLeft) Alignment.CenterStart
                                            else Alignment.CenterEnd
                                        )
                                        .padding(
                                            start = if (permissionDotsOnLeft) {
                                                permissionDotStartInsetDp(collapsed.heightDp).dp
                                            } else {
                                                0.dp
                                            },
                                            end = if (permissionDotsOnLeft) {
                                                0.dp
                                            } else {
                                                permissionDotEndInsetDp(collapsed.heightDp).dp
                                            },
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            // Anchor activity feedback to the physical camera cutout, not album art.
            // The pill shifts away from the satellite bubble; the camera position stays fixed.
            if (satelliteShown && satelliteSharing && shownEvent != null) {
                SatelliteActivityRing(
                    event = shownEvent,
                    diameterDp = collapsed.heightDp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            x = dims.offsetXDp.dp,
                            y = offsetY + (collapsed.heightDp * 0.03f).dp,
                        ),
                )
            }

            // The bubble is a sibling of the pill so the pill keeps its own centre and the bubble simply
            // follows its edge. It disappears while expanded, during calls, and in camera-anchored mode.
            lastSatellite?.takeIf { satelliteReveal.value > 0.01f }?.let { bubble ->
                val diameterDp = collapsed.heightDp
                val step = revealWidth / 2 + SATELLITE_GAP_DP.dp + (diameterDp / 2f).dp
                val mergeProgress = satelliteMerge.value.coerceIn(0f, 1f)
                val mergeDirection = if (satellitePosition == SatellitePosition.LEFT) -1f else 1f
                val satelliteOffsetX = offsetX + (step * mergeDirection * (1f - mergeProgress))

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            x = satelliteOffsetX,
                            y = offsetY,
                        ),
                ) {
                    SatelliteBubble(
                        event = bubble,
                        diameterDp = diameterDp,
                        appearance = appearance,
                        onClick = onSatelliteClick,
                        modifier = Modifier.graphicsLayer {
                            val merge = satelliteMerge.value.coerceIn(0f, 1f)
                            // Stretch along the travel axis as the bubble is pulled into the pill,
                            // then contract and fade only after it has visually joined the surface.
                            val stretch = 1f + 0.38f * satelliteMergeStretch(merge)
                            scaleX = satelliteReveal.value * if (satellitePosition == SatellitePosition.LEFT) stretch else stretch
                            scaleY = satelliteReveal.value * (1f - 0.12f * merge)
                            alpha = satelliteReveal.value * satelliteMergeAlpha(merge)
                        },
                    )
                }
            }
        }
    }
}
