package com.vikram.expressiveisland.overlay.contents

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
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
    val hangUp = event.actions.firstOrNull { it.destructive } ?: event.actions.firstOrNull()
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
            Text(
                text = event.label,
                modifier = Modifier.weight(1f),
                color = LocalContentColor.current,
                fontSize = CALL_NAME_SIZE_SP.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
                    CallWideButton(
                        icon = Icons.Rounded.Call,
                        label = stringResource(R.string.phone_answer),
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onAction(answer) },
                    )
                }
                if (hangUp != null) {
                    CallWideButton(
                        icon = Icons.Rounded.CallEnd,
                        label = stringResource(R.string.phone_hang_up),
                        container = MaterialTheme.colorScheme.error,
                        content = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.weight(1f),
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
    val hangUp = event.actions.firstOrNull { it.destructive } ?: event.actions.firstOrNull()
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
            // Connected calls put the ticking duration above the name; an incoming call shows only
            // the caller label (name or number), so there is nothing to stack above it.
            if (!incoming) {
                AnimatedVisibility(visible = call.showDuration) {
                    CallStatus(onCall = onCall)
                }
            }
            Text(
                text = event.label,
                color = LocalContentColor.current,
                fontSize = CALL_NAME_SIZE_SP.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (call.showActions) {
            if (incoming) {
                if (answer != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.Call,
                        description = "Answer",
                        container = MaterialTheme.colorScheme.primary,
                        content = MaterialTheme.colorScheme.onPrimary,
                        onClick = { onAction(answer) },
                    )
                }
                if (hangUp != null) {
                    CallCircleButton(
                        icon = Icons.Rounded.CallEnd,
                        description = "Decline",
                        container = MaterialTheme.colorScheme.error,
                        content = MaterialTheme.colorScheme.onError,
                        onClick = { onAction(hangUp) },
                    )
                }
            } else if (hangUp != null) {
                val fill = call.hangUpColor.resolve()
                CallCircleButton(
                    icon = Icons.Rounded.CallEnd,
                    description = "Hang up",
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
) {
    val call = event.call ?: return
    val onCall by OnCallBus.state.collectAsStateWithLifecycle()
    val incoming = onCall?.ongoing == false

    CallSingleRowContent(
        event = event,
        call = call,
        onCall = onCall,
        onAction = onAction,
        incoming = incoming
    )
}

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


