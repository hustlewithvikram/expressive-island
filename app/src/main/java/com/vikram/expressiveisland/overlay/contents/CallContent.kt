package com.vikram.expressiveisland.overlay.contents

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.CallAudioBus
import com.vikram.expressiveisland.core.OnCall
import com.vikram.expressiveisland.core.OnCallBus
import com.vikram.expressiveisland.data.CALL_MAX_WIDTH_PERCENT
import com.vikram.expressiveisland.data.CALL_MIN_WIDTH_PERCENT
import com.vikram.expressiveisland.data.asCallCutout
import com.vikram.expressiveisland.overlay.island.CallTileOptions
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.expandedActionsExtraDp
import com.vikram.expressiveisland.overlay.resolve
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * The incoming-call two-row layout, sized to the expanded cutout plus [callIncomingExtraDp]. Top (below
 * a camera-clearing pad): the caller's photo and a single label — their contact name if they have one,
 * otherwise their number. Bottom (pinned to the edge, a flexible gap between): full-width Take (answer,
 * primary) and Hang up (decline, red) buttons, degrading to a single full-width button if the dialer
 * exposes only one of the two actions.
 */
@Composable
fun IncomingCallExpandedContent(
    event: IslandEvent,
    call: CallTileOptions,
    onCall: OnCall?,
    onAction: (IslandAction) -> Unit,
) {
    val photo = onCall?.photo?.takeIf { call.showPhoto }
    val hangUp = event.actions.firstOrNull { it.destructive } ?: event.actions.firstOrNull { !it.answer }
    val answer = event.actions.firstOrNull { it.answer }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = CALL_INCOMING_SIDE_PAD_DP.dp,
                end = CALL_INCOMING_SIDE_PAD_DP.dp,
                top = CALL_INCOMING_TOP_PAD_DP.dp,
                bottom = CALL_INCOMING_BOTTOM_PAD_DP.dp,
            ),
    ) {
        // Caller row — pinned just below the top camera clearance, so the single label (already the
        // name when known, else the number) sits clear of the camera hole.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CALL_ROW_SPACING_DP.dp),
        ) {
            if (photo != null) {
                ContactPhoto(bitmap = photo, size = CALL_INCOMING_AVATAR_DP.dp)
            } else {
                IconBadge(event = event, badgeSize = CALL_INCOMING_AVATAR_DP.dp, iconSize = 24.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                TileTitle(title = event.label)
                onCall?.callerNumber
                    ?.takeIf { it.isNotBlank() && it != event.label }
                    ?.let { number ->
                        Text(
                            text = number,
                            color = LocalContentColor.current.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
        }
        // A flexible gap pushes the button row down to the bottom edge.
        Spacer(modifier = Modifier.weight(1f))
        // Button row — Take (answer) then Hang up (decline), each filling half the width.
        if (call.showActions && (answer != null || hangUp != null)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CALL_INCOMING_BUTTON_GAP_DP.dp),
            ) {
                if (answer != null) {
                    val answerFill = call.incomingAnswerColor.resolve()
                    CallWideButton(
                        icon = Icons.Rounded.Call,
                        label = stringResource(R.string.phone_answer),
                        container = answerFill,
                        content = if (answerFill.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.weight(1f),
                        showLabel = call.showButtonLabels,
                        onClick = { onAction(answer) },
                    )
                }
                if (hangUp != null) {
                    val hangUpFill = call.incomingHangUpColor.resolve()
                    CallWideButton(
                        icon = Icons.Rounded.CallEnd,
                        label = stringResource(R.string.phone_hang_up),
                        container = hangUpFill,
                        content = if (hangUpFill.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.weight(1f),
                        showLabel = call.showButtonLabels,
                        onClick = { onAction(hangUp) },
                    )
                }
            }
        }
    }
}

/**
 * The compact single-row call layout. Left to right: photo, the caller text, and the call button(s).
 * A connected call shows the duration over the caller name and one hang-up button; an [incoming] call
 * shows just the caller label (the contact name if known, otherwise the number — never both) and two
 * buttons, decline then answer.
 */
@Composable
fun CallSingleRowContent(
    event: IslandEvent,
    call: CallTileOptions,
    onCall: OnCall?,
    incoming: Boolean,
    onAction: (IslandAction) -> Unit,
) {
    val photo = onCall?.photo?.takeIf { call.showPhoto }
    // The decline / hang-up (destructive) action; fall back to the first action if the dialer flags none.
    val hangUp = event.actions.firstOrNull { it.destructive } ?: event.actions.firstOrNull { !it.answer }
    val answer = event.actions.firstOrNull { it.answer }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = CALL_ROW_PADDING_DP.dp,
                end = CALL_ROW_PADDING_DP.dp,
                bottom = if (incoming) CALL_ROW_PADDING_DP.dp else 0.dp,
            ),
        verticalAlignment = if (incoming) Alignment.Bottom else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CALL_ROW_SPACING_DP.dp),
    ) {
        if (photo != null) {
            ContactPhoto(bitmap = photo, size = CALL_AVATAR_DP.dp)
        } else {
            IconBadge(event = event, badgeSize = CALL_AVATAR_DP.dp, iconSize = 24.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            if (incoming) {
                TileTitle(title = event.label)
                onCall?.callerNumber
                    ?.takeIf { it.isNotBlank() && it != event.label }
                    ?.let { number ->
                        Text(
                            text = number,
                            color = LocalContentColor.current.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            } else {
                AnimatedVisibility(visible = call.showDuration) {
                    CallStatus(onCall = onCall)
                }
            }
            if (!incoming) TileTitle(title = event.label)
        }
        if (call.showActions) {
            if (incoming) {
                if (answer != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.Call,
                        description = "Answer",
                        container = call.incomingAnswerColor.resolve(),
                        content = if (call.incomingAnswerColor.resolve().luminance() > 0.5f) Color.Black else Color.White,
                        onClick = { onAction(answer) },
                    )
                }
                if (hangUp != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.CallEnd,
                        description = "Decline",
                        container = call.incomingHangUpColor.resolve(),
                        content = if (call.incomingHangUpColor.resolve().luminance() > 0.5f) Color.Black else Color.White,
                        onClick = { onAction(hangUp) },
                    )
                }
            } else if (hangUp != null) {
                val fill = call.hangUpColor.resolve()
                CallCircleButton(
                    icon = Icons.Rounded.CallEnd,
                    description = if (onCall?.outgoing == true) "Cancel call" else "Hang up",
                    container = fill,
                    content = if (fill.luminance() > 0.5f) PillTextColorDark else PillTextColor,
                    onClick = { onAction(hangUp) },
                )
            }
        }
    }
}

/**
 * The phone tile's normal-only layout (it has no expanded state), dispatched by call state. A
 * connected call, and an incoming call when the two-row layout is off, use one compact row
 * ([CallSingleRowContent]). An incoming (still ringing) call with the "Expanded layout for incoming
 * calls" setting on uses the taller two-row [IncomingCallExpandedContent] — the caller (below the
 * camera) over full-width Take / Hang up buttons, matching the fuller shape from [asCallCutout]. Live
 * state (photo, caller number, connected-or-ringing, duration start) is read from [OnCallBus].
 */
@Composable
fun CallNormalContent(
    event: IslandEvent,
    onAction: (IslandAction) -> Unit,
    onCallOverride: OnCall? = null,
) {
    val call = event.call ?: return
    val liveOnCall by OnCallBus.state.collectAsStateWithLifecycle()
    val onCall = onCallOverride ?: liveOnCall
    val incoming = if (event.id == -1L) {
        !call.previewOutgoing && !call.previewOngoing
    } else {
        onCall?.ongoing == false
    }

    if (incoming && call.incomingExpandedLayout && call.showActions && event.actions.isNotEmpty()) {
        IncomingCallExpandedContent(
            event = event,
            call = call,
            onCall = onCall,
            onAction = onAction,
        )
    } else {
        CallSingleRowContent(
            event = event,
            call = call,
            onCall = onCall,
            onAction = onAction,
            incoming = incoming,
        )
    }
}


/**
 * Connected-call expanded island: caller and duration, live Mute/Speaker toggles, an Open action
 * that launches the dialer's own in-call screen, and the dialer's real hang-up action.
 */
@Composable
fun CallExpandedContent(
    event: IslandEvent,
    onCall: OnCall?,
    onAction: (IslandAction) -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val call = event.call ?: return
    val contactPhoto = onCall?.photo
    val photo = contactPhoto?.takeIf { call.showPhoto }
    val hangUp = event.actions.firstOrNull { it.destructive }
        ?: event.actions.firstOrNull { !it.answer }
    val liveMuted by CallAudioBus.muted.collectAsStateWithLifecycle()
    val liveSpeakerOn by CallAudioBus.speakerOn.collectAsStateWithLifecycle()
    var previewMuted by remember(event.detail) { mutableStateOf(false) }
    var previewSpeakerOn by remember(event.detail) { mutableStateOf(false) }
    val isPreview = event.id == -1L
    val muted = if (isPreview) previewMuted else liveMuted
    val speakerOn = if (isPreview) previewSpeakerOn else liveSpeakerOn
    val activeFill = call.otherButtonColor.resolve()

    LaunchedEffect(isPreview) {
        if (!isPreview) CallAudioBus.refresh(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (call.expandedPhotoBackground && contactPhoto != null) {
            Image(
                bitmap = contactPhoto,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (call.expandedPhotoBlur && call.expandedPhotoBlurRadius > 0f)
                            Modifier.blur(call.expandedPhotoBlurRadius.dp)
                        else Modifier
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f)),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = CALL_EXPANDED_TOP_PAD_DP.dp,
                    bottom = 14.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (photo != null) {
                ContactPhoto(bitmap = photo, size = 48.dp)
            } else {
                IconBadge(event = event, badgeSize = 48.dp, iconSize = 24.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                TileTitle(title = event.label)
                if (call.showDuration) CallStatus(onCall = onCall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (onCall?.outgoing == true) {
                CallControlButton(
                    icon = Icons.Rounded.OpenInNew,
                    label = "Open phone",
                    active = false,
                    activeColor = activeFill,
                    modifier = Modifier.weight(1f),
                    onClick = onOpen,
                )
            } else {
                CallControlButton(
                    icon = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    label = "Mute",
                    active = muted,
                    activeColor = activeFill,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isPreview) previewMuted = !previewMuted
                        else CallAudioBus.toggleMute(context)
                    },
                )
                CallControlButton(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = "Speaker",
                    active = speakerOn,
                    activeColor = activeFill,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isPreview) previewSpeakerOn = !previewSpeakerOn
                        else CallAudioBus.toggleSpeaker(context)
                    },
                )
                CallControlButton(
                    icon = Icons.Rounded.OpenInNew,
                    label = "Open",
                    active = false,
                    activeColor = activeFill,
                    modifier = Modifier.weight(1f),
                    onClick = onOpen,
                )
            }
        }

        if (call.showActions && hangUp != null) {
            val fill = call.expandedHangUpColor.resolve()
            Surface(
                onClick = { onAction(hangUp) },
                color = fill,
                contentColor = if (fill.luminance() > 0.5f) Color.Black else Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.CallEnd, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            if (onCall?.outgoing == true) "Cancel call" else stringResource(R.string.phone_hang_up),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
        }
    }
}


/** Compact connected-call pill: caller portrait on the left and a live duration on the right. */
@Composable
fun MiniCallContent(
    event: IslandEvent,
    call: CallTileOptions,
    onCall: OnCall?,
    heightDp: Int,
) {
    val photo = onCall?.photo?.takeIf { call.showPhoto }
    val start = onCall?.startTimeMs
    var now by remember(start) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start, call.showDuration) {
        if (start != null && call.showDuration) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    val duration = if (start != null) {
        val total = ((now - start) / 1_000L).coerceAtLeast(0L)
        val hours = total / 3600L
        val minutes = (total % 3600L) / 60L
        val seconds = total % 60L
        if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    } else null

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "Caller photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size((heightDp - 8).coerceAtLeast(18).dp)
                    .clip(CircleShape),
            )
        } else if (call.showPhoto) {
            IconBadge(event = event, badgeSize = (heightDp - 8).coerceAtLeast(18).dp, iconSize = 18.dp)
        }
        if (call.showDuration && duration != null) {
            Text(
                text = duration,
                color = LocalContentColor.current,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val fill = if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant
    val ink = if (active) {
        if (fill.luminance() > 0.5f) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        color = fill,
        contentColor = ink,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(58.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(21.dp))
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    }
}

private const val CALL_EXPANDED_TOP_PAD_DP = 38

/**
 * The width (as a screen-width percentage) the call cutout should span for [callerName]:
 * [CALL_MIN_WIDTH_PERCENT] by default, widening to fit a long name up to [CALL_MAX_WIDTH_PERCENT].
 * [trailingButtons] reserves room for that many trailing call buttons (one for a connected call's
 * hang-up, two for an incoming call's decline + answer, zero when actions are hidden). An [incoming]
 * call always spans the full [CALL_MAX_WIDTH_PERCENT] (never narrower) so its two buttons and the
 * number/name always have room. The pill is sized to this width and its content laid out within it —
 * a name too long for even the max width ellipsis — so measuring the name here (rather than letting
 * content drive the size) lets the overlay's rendering and its touchable region agree exactly on the
 * pill's width. [density] converts the measured text to dp.
 */
internal fun callCutoutWidthPercent(
    callerName: String,
    trailingButtons: Int,
    incoming: Boolean,
    displayWidthDp: Int,
    density: Float,
): Int {
    // Everything on the row that isn't the name: leading avatar + its spacing, the trailing button(s) +
    // their spacing (when shown), and the row's horizontal padding on both edges. Mirrors CallNormalContent.
    val trailingDp = trailingButtons * (CALL_HANGUP_BUTTON_DP + CALL_ROW_SPACING_DP)
    val fixedDp = CALL_ROW_PADDING_DP * 2 + CALL_AVATAR_DP + CALL_ROW_SPACING_DP + trailingDp
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = CALL_NAME_SIZE_SP * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val nameWidthDp = paint.measureText(callerName) / density
    val neededDp = fixedDp + nameWidthDp + CALL_NAME_SLACK_DP
    val percent = (neededDp / displayWidthDp.coerceAtLeast(1) * 100f).roundToInt()
    // An incoming call is pinned to the full width; a connected one adapts from the minimum up.
    val floor = if (incoming) CALL_MAX_WIDTH_PERCENT else CALL_MIN_WIDTH_PERCENT
    return percent.coerceIn(floor, CALL_MAX_WIDTH_PERCENT)
}

/**
 * The extra height (over the expanded cutout) the incoming two-row layout claims for its bottom Take /
 * Hang up row, mirroring [expandedActionsExtraDp]. Shared with the overlay controller so the window and
 * touchable region stay as tall as what [IncomingCallExpandedContent] renders.
 */
internal fun callIncomingExtraDp(): Int = CALL_INCOMING_BUTTON_DP + CALL_INCOMING_BUTTON_GAP_DP

/** Extra space below the configured expanded shell for connected-call controls. */
internal fun callExpandedExtraDp(showHangUp: Boolean): Int =
    CALL_EXPANDED_ROW_GAP_DP + CALL_EXPANDED_TOGGLE_DP +
        if (showHangUp) CALL_EXPANDED_ROW_GAP_DP + CALL_EXPANDED_HANGUP_DP else 0

private const val CALL_EXPANDED_ROW_GAP_DP = 10
private const val CALL_EXPANDED_TOGGLE_DP = 58
private const val CALL_EXPANDED_HANGUP_DP = 48



