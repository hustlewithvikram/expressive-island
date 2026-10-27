package com.vikram.expressiveisland.overlay

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.vikram.expressiveisland.overlay.contents.EmptyPillContent
import com.vikram.expressiveisland.overlay.contents.ExpandedContent
import com.vikram.expressiveisland.overlay.contents.IslandSurface
import com.vikram.expressiveisland.overlay.contents.LocalActionButtonAnimation
import com.vikram.expressiveisland.overlay.contents.PressExpandDp
import com.vikram.expressiveisland.overlay.satellite.SatelliteBubble
import com.vikram.expressiveisland.overlay.contents.callCutoutWidthPercent
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.island.IslandMotion
import com.vikram.expressiveisland.overlay.island.SATELLITE_GAP_DP
import com.vikram.expressiveisland.overlay.satellite.SatellitePosition
import com.vikram.expressiveisland.system.PermissionUsage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.unit.lerp as lerpDp


// How far the island must be dragged upward before a swipe-up collapses it.
private const val SWIPE_UP_SHRINK_THRESHOLD_DP = 24

// How far the island must be dragged sideways before releasing dismisses it.
private const val SWIPE_DISMISS_THRESHOLD_DP = 90

// How long the "reply sent" confirmation stays on screen before the reply is dispatched.
private const val REPLY_SENT_FEEDBACK_MS = 900L

// The tuned baseline for the island's primary expand/collapse transition. Every tween-based
// animation is expressed relative to this, so the user's single "animation duration" knob scales
// them all in proportion (see `animScale` in DynamicIsland). Its default equals this value.
private const val BASE_TRANSITION_MS = IslandMotion.BASE_TRANSITION_MS

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
    emptyOpensCenter: Boolean = false,
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
    collapseRequest: Int = 0,
    satellite: IslandEvent? = null,
    satellitePosition: SatellitePosition = SatellitePosition.RIGHT,
    onSatelliteClick: () -> Unit = {},
) {
    var lastEvent by remember { mutableStateOf<IslandEvent?>(null) }
    if (event != null) {
        lastEvent = event
    }

    val shownEvent = lastEvent
    val chargingState by ChargingBus.state.collectAsStateWithLifecycle()
    val headphonesState by HeadphonesBus.state.collectAsStateWithLifecycle()
    val mediaActive = shownEvent?.media != null
    val emptyPill = event == null && showsWhenEmpty

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

    var centerInteraction by remember { mutableIntStateOf(0) }
    var mediaInteraction by remember { mutableIntStateOf(0) }
    var replyingTo by remember(shownEvent?.id) { mutableStateOf<IslandAction?>(null) }
    val replying = replyingTo != null
    var sentReply by remember(shownEvent?.id) { mutableStateOf<Pair<IslandAction, String>?>(null) }
    val confirmingSent = sentReply != null
    val isCall = shownEvent?.call != null
    val isAssistantNormalOnly =
        shownEvent?.assistant != null && !shownEvent.assistant.displayAnswerInCutout
    val isNormalOnly = isCall || isAssistantNormalOnly || shownEvent?.normalOnly == true
    val centerExpanded = emptyPill && emptyOpensCenter && tapExpanded

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

    val animScale = animationDurationMs / BASE_TRANSITION_MS.toFloat()
    fun scaled(baseMs: Int) = (baseMs * animScale).roundToInt()

    val motion = remember(animationStyle, animationSpeed, animationBounce, animationDurationMs) {
        IslandMotion(animationStyle, animationSpeed, animationBounce, animationDurationMs)
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
    ) {
        if (forcedExpanded == null && tapExpanded && autoCollapse && !replying && !confirmingSent) {
            delay(autoCollapseMs.milliseconds)
            tapExpanded = false
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

    var assistantContentHeightDp by remember(shownEvent?.assistant != null) { mutableIntStateOf(0) }
    var centerContentHeightDp by remember { mutableIntStateOf(0) }
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    val heightBonus = when {
        emptyPill && isExpanded -> {
            if (centerContentHeightDp > 0) {
                collapsed.heightDp + CENTER_TOP_GAP_DP + centerContentHeightDp - dims.heightDp
            } else {
                CENTER_SHORTCUTS_EXTRA_DP
            }
        }

        emptyPill -> 0

        isExpanded && (
                systemEventType == SystemEventType.CHARGING_STARTED ||
                        systemEventType == SystemEventType.BATTERY_LOW ||
                        systemEventType == SystemEventType.WIFI_CONNECTED
                ) -> 20

        isExpanded && shownEvent?.assistant != null && shownEvent.assistant.displayAnswerInCutout -> {
            val maxCutoutHeightDp =
                (screenHeightDp * shownEvent.assistant.maxCutoutHeightPercent / 100)
            val fitHeightDp = if (assistantContentHeightDp > 0) assistantContentHeightDp else 110
            val targetHeightDp = fitHeightDp.coerceIn(110, maxCutoutHeightDp)
            (targetHeightDp - dims.heightDp)
        }

        isExpanded && (hasActions || hasMediaControls || hasCallActions || hasTimerActions) ->
            expandedActionsExtraDp(appearance.actionButtonHeightDp)

        else -> 0
    }

    val present = event != null || showsWhenEmpty
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(present) {
        reveal.animateTo(
            targetValue = if (present) 1f else 0f,
            animationSpec = motion.float(baseMs = if (present) 320 else 200),
        )
    }

    LaunchedEffect(emptyPill) {
        if (emptyPill) {
            tapExpanded = false
            if (dismissOffsetX.value != 0f) {
                reveal.snapTo(0f)
                dismissOffsetX.snapTo(0f)
                reveal.animateTo(1f, animationSpec = motion.float(baseMs = 320))
            }
        }
    }

    val spec: AnimationSpec<Dp> = if (reveal.value == 0f) snap() else motion.dp()
    val isAssistantAnswer = isExpanded && shownEvent?.assistant?.displayAnswerInCutout == true
    val heightSpec: AnimationSpec<Dp> = spec

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

    val dotDp = collapsed.heightDp.dp
    val revealWidth = lerpDp(dotDp, width, reveal.value)
    val revealHeight = lerpDp(dotDp, height, reveal.value)
    val dotCorner = dotDp / 2
    val revealTopLeft = lerpDp(dotCorner, topLeft, reveal.value)
    val revealTopRight = lerpDp(dotCorner, topRight, reveal.value)
    val revealBottomLeft = lerpDp(dotCorner, bottomLeft, reveal.value)
    val revealBottomRight = lerpDp(dotCorner, bottomRight, reveal.value)

    var lastSatellite by remember { mutableStateOf<IslandEvent?>(null) }
    if (satellite != null) lastSatellite = satellite
    val satelliteShown = satellite != null && present && !isCall && !isStickToCamera
    val satelliteReveal = remember { Animatable(0f) }
    LaunchedEffect(satelliteShown) {
        satelliteReveal.animateTo(
            targetValue = if (satelliteShown) 1f else 0f,
            animationSpec = motion.float(baseMs = if (satelliteShown) 320 else 200),
        )
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

            Box(
                modifier = Modifier
                    .align(if (isStickToCamera) stickAlignment else Alignment.TopCenter)
                    .padding(start = stickPaddingStart, end = stickPaddingEnd)
                    .offset(
                        x = if (isStickToCamera) 0.dp else offsetX,
                        y = if (isStickToCamera) 0.dp else offsetY
                    ),
            ) {
                if (present || reveal.value > 0f) {
                    IslandSurface(
                        modifier = Modifier
                            .width(revealWidth)
                            .height(revealHeight)
                            .graphicsLayer {
                                val extraPx = PressExpandDp.toPx() * 2f * pressExpand.value
                                val widen =
                                    if (size.width > 0f) (size.width + extraPx) / size.width else 1f
                                scaleX = boopScale.value * widen
                                scaleY = boopScale.value
                                translationX = dismissOffsetX.value
                                val travel =
                                    abs(dismissOffsetX.value) / size.width.coerceAtLeast(1f)
                                val revealAlpha = (reveal.value / 0.2f).coerceIn(0f, 1f)
                                alpha = (1f - travel).coerceIn(0.25f, 1f) * revealAlpha
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

                                        if (!isExpanded) {
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

                                        if (!isExpanded) {
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

                                            scope.launch {
                                                motion.pop(
                                                    boopScale,
                                                    peak = 1.03f,
                                                )
                                            }
                                        }
                                    },
                                    onTap = {
                                        if (vibrateOnTap) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }

                                        if (emptyPill) {
                                            // "Open center" expands the resting pill into the shortcut
                                            // grid (a second tap toggles it closed); every other "On
                                            // click" action (e.g. open an app) runs via onEmptyClick.
                                            if (emptyOpensCenter) {
                                                if (forcedExpanded == null) {
                                                    tapExpanded = !tapExpanded
                                                    if (tapExpanded) {
                                                        scope.launch {
                                                            motion.pop(
                                                                boopScale,
                                                                peak = 1.03f
                                                            )
                                                        }
                                                    }
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
                                if (forcedExpanded != null || !shrinkOnSwipeUp || emptyPill) return@pointerInput
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
                                                    animationSpec = spring(
                                                        dampingRatio = 0.6f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    ),
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
                            animationSpec = tween(scaled(150)),
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
                                } else if (emptyIcon != null) {
                                    EmptyPillContent(
                                        icon = emptyIcon,
                                        containerColor = emptyIconColor,
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
                                            systemEventType = systemEventType,
                                            chargingState = chargingState,
                                            showActions = showActions,
                                            appearance = appearance,
                                            collapsedHeightDp = collapsed.heightDp,
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
                                            onHeightMeasured = { assistantContentHeightDp = it },
                                        )
                                    } else {
                                        CollapsedContent(
                                            event = e,
                                            heightDp = collapsed.heightDp,
                                            isStickToCamera = isStickToCamera,
                                            trailingInsetDp = collapsedTrailingInsetDp,
                                        )
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

            // The bubble is a sibling of the pill so the pill keeps its own centre and the bubble simply
            // follows its edge. It disappears while expanded, during calls, and in camera-anchored mode.
            lastSatellite?.takeIf { satelliteReveal.value > 0.01f }?.let { bubble ->
                val diameterDp = collapsed.heightDp
                val step = revealWidth / 2 + SATELLITE_GAP_DP.dp + (diameterDp / 2f).dp
                val satelliteOffsetX =
                    if (satellitePosition == SatellitePosition.LEFT) offsetX - step else offsetX + step

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
                            scaleX = satelliteReveal.value
                            scaleY = satelliteReveal.value
                            alpha = satelliteReveal.value
                        },
                    )
                }
            }
        }
    }
}
