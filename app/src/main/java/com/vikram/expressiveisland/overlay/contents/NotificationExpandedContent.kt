package com.vikram.expressiveisland.overlay.contents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.ActionButtonAlignment
import com.vikram.expressiveisland.data.ActionButtonAnimation
import com.vikram.expressiveisland.data.ActionButtonStyle
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.data.ReplyInputStyle
import com.vikram.expressiveisland.data.SentAlignment
import com.vikram.expressiveisland.overlay.island.IslandAction
import com.vikram.expressiveisland.overlay.island.IslandEvent
import com.vikram.expressiveisland.overlay.resolve
import com.vikram.expressiveisland.overlay.toHorizontal
import com.vikram.expressiveisland.service.ProgressData

// The expanded notification's progress bar: its thickness, and the gap holding it off the text above.
private const val PROGRESS_BAR_HEIGHT_DP = 8
private const val PROGRESS_BAR_TOP_GAP_DP = 6


@Composable
fun NotificationExpandedContent(
    event: IslandEvent,
    appearance: AppearanceSettings,
    collapsedHeightDp: Int,
    replyingTo: IslandAction?,
    replySent: Boolean,
    showActions: Boolean,
    progressData: ProgressData?,
    onAction: (IslandAction) -> Unit,
    onStartReply: (IslandAction) -> Unit,
    onCancelReply: () -> Unit,
    onSendReply: (String) -> Unit,
) {
    val notificationHeader = rememberNotificationHeader(
        event = event,
        appearance = appearance,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = collapsedHeightDp.dp,
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                ACTIONS_ROW_SPACING_DP.dp,
            ),
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconBadge(
                    event = event,
                    badgeSize = 44.dp,
                    iconSize = 26.dp,
                )

                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    notificationHeader?.let { header ->
                        Text(
                            text = header,
                            color = LocalContentColor.current.copy(alpha = 0.62f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = event.label,
                        color = LocalContentColor.current,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    event.detail?.let { detail ->
                        val detailModifier =
                            Modifier.weight(1f, fill = false).let { modifier ->
                                if (appearance.showFullNotificationText) {
                                    modifier.verticalScroll(
                                        rememberScrollState(),
                                    )
                                } else {
                                    modifier
                                }
                            }

                        Text(
                            text = detail,
                            modifier = detailModifier,
                            color = LocalContentColor.current.copy(alpha = 0.70f),
                            fontSize = 12.sp,
                            maxLines =
                                if (appearance.showFullNotificationText) {
                                    Int.MAX_VALUE
                                } else {
                                    2
                                },
                            overflow =
                                if (appearance.showFullNotificationText) {
                                    TextOverflow.Clip
                                } else {
                                    TextOverflow.Ellipsis
                                },
                        )
                    }

                    var lastProgressData by remember {
                        mutableStateOf(progressData)
                    }

                    if (progressData != null) {
                        lastProgressData = progressData
                    }

                    AnimatedVisibility(
                        visible = progressData != null,
                    ) {
                        val barModifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = PROGRESS_BAR_TOP_GAP_DP.dp,
                            )
                            .requiredHeight(
                                PROGRESS_BAR_HEIGHT_DP.dp,
                            )

                        lastProgressData?.let { p ->
                            if (p.isIndeterminate) {
                                LinearProgressIndicator(
                                    modifier = barModifier,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                                    strokeCap =
                                        ProgressIndicatorDefaults.LinearStrokeCap,
                                )
                            } else {
                                val fraction =
                                    if (p.max <= 0) {
                                        0f
                                    } else {
                                        (p.current.toFloat() / p.max)
                                            .coerceIn(0f, 1f)
                                    }

                                val animatedFraction by animateFloatAsState(
                                    targetValue = fraction,
                                    label = "notificationProgress",
                                )

                                LinearProgressIndicator(
                                    progress = {
                                        animatedFraction
                                    },
                                    modifier = barModifier,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                                    strokeCap =
                                        ProgressIndicatorDefaults.LinearStrokeCap,
                                )
                            }
                        }
                    }
                }
            }

            val sendColor =
                appearance.sendButtonColor?.resolve()
                    ?: event.accent

            when {
                replySent -> {
                    ReplySentRow(
                        tint = sendColor,
                        heightDp = appearance.actionButtonHeightDp,
                        alignment = appearance.sentAlignment,
                    )
                }

                replyingTo != null -> {
                    ReplyRow(
                        hint = replyingTo.reply?.hint,
                        accent = event.accent,
                        sendColor = sendColor,
                        cancelColor =
                            appearance.cancelButtonColor?.resolve(),
                        inputStyle = appearance.replyInputStyle,
                        cancelOnLeft = appearance.cancelButtonOnLeft,
                        heightDp = appearance.actionButtonHeightDp,
                        onSend = onSendReply,
                        onCancel = onCancelReply,
                    )
                }

                showActions && event.actions.isNotEmpty() -> {
                    val chipFill =
                        appearance.actionButtonColor?.resolve()
                            ?: event.accent

                    ActionChipRow(
                        actions = event.actions.take(3),
                        style = appearance.actionButtonStyle,
                        fill = chipFill,
                        heightDp = appearance.actionButtonHeightDp,
                        alignment = appearance.actionButtonAlignment,
                        onChip = { action ->
                            if (action.reply != null) {
                                onStartReply(action)
                            } else {
                                onAction(action)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * The expanded action chips row. In [ActionButtonAlignment.FULL] the chips share the width equally;
 * every other alignment sizes them to content and positions the row as a group. When the button
 * animation is [ActionButtonAnimation.EXPAND] *and* the row is full of more than one chip, a
 * pressed chip borrows width from its siblings ([FULL_EXPAND_DELTA]) — its weight springs up while
 * theirs spring down by the same total — an expressive give-and-take that keeps the row at 100%.
 * In every other case each chip animates itself in place via [ActionChip]'s own [pressScale].
 */
@Composable
fun ActionChipRow(
    actions: List<IslandAction>,
    style: ActionButtonStyle,
    fill: Color,
    heightDp: Int,
    alignment: ActionButtonAlignment,
    onChip: (IslandAction) -> Unit,
) {
    val full = alignment == ActionButtonAlignment.FULL
    val redistribute = full && actions.size > 1 &&
            LocalActionButtonAnimation.current == ActionButtonAnimation.EXPAND
    val interactions = remember(actions.size) { List(actions.size) { MutableInteractionSource() } }
    // Which chip is currently held (first press wins) — drives the width give-and-take. Collected for
    // every chip on each composition so the number of composable calls stays constant.
    val pressedFlags = interactions.map { it.collectIsPressedAsState().value }
    val pressedIndex = if (redistribute) pressedFlags.indexOfFirst { it } else -1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment.toHorizontal()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEachIndexed { i, action ->
            val chipModifier = when {
                redistribute -> {
                    val target = when {
                        pressedIndex < 0 -> 1f
                        i == pressedIndex -> 1f + FULL_EXPAND_DELTA
                        else -> 1f - FULL_EXPAND_DELTA / (actions.size - 1)
                    }
                    val weight by animateFloatAsState(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = 0.42f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "chipWeight",
                    )
                    Modifier.weight(weight)
                }

                full -> Modifier.weight(1f)
                else -> Modifier
            }
            ActionChip(
                action = action,
                style = style,
                fill = fill,
                heightDp = heightDp,
                onClick = { onChip(action) },
                modifier = chipModifier,
                interaction = interactions[i],
                // When redistributing, the give-and-take of widths IS the press animation, so the
                // chip must not also expand itself in place.
                animatePress = !redistribute,
            )
        }
    }
}

/**
 * The post-send confirmation shown briefly in place of the reply field: a circular [tint] badge
 * whose check mark springs in with a little overshoot, and a "Sent" label that fades up beside it,
 * so the user gets clear feedback that the message went out before the island dismisses.
 */
@Composable
private fun ReplySentRow(tint: Color, heightDp: Int, alignment: SentAlignment) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, alignment.toHorizontal()),
    ) {
        Box(
            modifier = Modifier
                .size(heightDp.dp)
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                }
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = if (tint.luminance() > 0.5f) PillTextColorDark else PillTextColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(R.string.reply_sent),
            color = LocalContentColor.current.copy(alpha = appear.value),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * An inline reply field with send/cancel affordances; requests focus so the keyboard appears.
 * [inputStyle] shapes the text field (Expressive pill / Material You / Material 2), [cancelOnLeft]
 * moves the cancel button to the leading edge, and [heightDp] sizes the field and buttons.
 */
@Composable
private fun ReplyRow(
    hint: String?,
    accent: Color,
    sendColor: Color,
    cancelColor: Color?,
    inputStyle: ReplyInputStyle,
    cancelOnLeft: Boolean,
    heightDp: Int,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val send = { if (text.isNotBlank()) onSend(text.trim()) }

    val cancelInteraction = remember { MutableInteractionSource() }
    val sendInteraction = remember { MutableInteractionSource() }

    // The segmented style joins cancel, field and send into one bar (see SegmentedReplyRow); the
    // others are separate controls with only the field's corner rounding differing.
    if (inputStyle == ReplyInputStyle.SEGMENTED) {
        SegmentedReplyRow(
            text = text,
            onValueChange = { text = it },
            hint = hint,
            accent = accent,
            sendColor = sendColor,
            cancelColor = cancelColor,
            cancelOnLeft = cancelOnLeft,
            heightDp = heightDp,
            focusRequester = focusRequester,
            onSend = send,
            onCancel = onCancel,
        )
        return
    }

    val fieldShape = when (inputStyle) {
        ReplyInputStyle.EXPRESSIVE -> CircleShape
        ReplyInputStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
        ReplyInputStyle.MATERIAL_2 -> RoundedCornerShape(4.dp)
        ReplyInputStyle.SEGMENTED -> CircleShape // handled above; unreachable.
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Cancel can sit before the field (leading) or between the field and send (trailing).
        if (cancelOnLeft) {
            ReplyCancelButton(cancelColor, heightDp, cancelInteraction, onCancel)
        }
        ReplyField(
            modifier = Modifier.weight(1f),
            text = text,
            onValueChange = { text = it },
            hint = hint,
            accent = accent,
            shape = fieldShape,
            heightDp = heightDp,
            focusRequester = focusRequester,
            onSend = send,
        )
        if (!cancelOnLeft) {
            ReplyCancelButton(cancelColor, heightDp, cancelInteraction, onCancel)
        }
        ReplySendButton(sendColor, text.isNotBlank(), heightDp, sendInteraction, send)
    }
}

/**
 * The "segmented" reply style: cancel, field and send sit flush in one connected bar, split by a
 * small gap, with the two outer segments carrying fully-rounded end-caps and the inner edges only
 * lightly rounded. [cancelOnLeft] chooses which end the cancel button caps (send always trails).
 */
@Composable
private fun SegmentedReplyRow(
    text: String,
    onValueChange: (String) -> Unit,
    hint: String?,
    accent: Color,
    sendColor: Color,
    cancelColor: Color?,
    cancelOnLeft: Boolean,
    heightDp: Int,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val cap = (heightDp / 2).dp
    val inner = 8.dp
    val startCap =
        RoundedCornerShape(topStart = cap, bottomStart = cap, topEnd = inner, bottomEnd = inner)
    val endCap =
        RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = cap, bottomEnd = cap)
    val innerShape = RoundedCornerShape(inner)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val cancel = @Composable { shape: Shape ->
            ReplySegmentButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Cancel reply",
                container = LocalContentColor.current.copy(alpha = 0.12f),
                content = cancelColor ?: LocalContentColor.current.copy(alpha = 0.7f),
                shape = shape,
                heightDp = heightDp,
                onClick = onCancel,
            )
        }
        // Leading end-cap: cancel when it's on the left, otherwise the field itself.
        if (cancelOnLeft) cancel(startCap)
        ReplyField(
            modifier = Modifier.weight(1f),
            text = text,
            onValueChange = onValueChange,
            hint = hint,
            accent = accent,
            shape = if (cancelOnLeft) innerShape else startCap,
            heightDp = heightDp,
            focusRequester = focusRequester,
            onSend = onSend,
        )
        if (!cancelOnLeft) cancel(innerShape)
        val sendEnabled = text.isNotBlank()
        ReplySegmentButton(
            icon = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Send reply",
            container = if (sendEnabled) sendColor else LocalContentColor.current.copy(alpha = 0.12f),
            content = when {
                !sendEnabled -> LocalContentColor.current.copy(alpha = 0.4f)
                sendColor.luminance() > 0.5f -> PillTextColorDark
                else -> PillTextColor
            },
            shape = endCap,
            heightDp = heightDp,
            enabled = sendEnabled,
            onClick = onSend,
        )
    }
}

/** One end-cap of the segmented reply bar: a shaped, filled tap target with a centred icon. */
@Composable
private fun ReplySegmentButton(
    icon: ImageVector,
    contentDescription: String,
    container: Color,
    content: Color,
    shape: Shape,
    heightDp: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = Modifier.size(heightDp.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ReplyField(
    modifier: Modifier,
    text: String,
    onValueChange: (String) -> Unit,
    hint: String?,
    accent: Color,
    shape: Shape,
    heightDp: Int,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(heightDp.dp)
            .clip(shape)
            .background(LocalContentColor.current.copy(alpha = 0.12f))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = text,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = LocalContentColor.current, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (text.isEmpty() && !hint.isNullOrBlank()) {
                    Text(
                        text = hint,
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun ReplyCancelButton(
    cancelColor: Color?,
    heightDp: Int,
    interaction: MutableInteractionSource,
    onCancel: () -> Unit,
) {
    IconButton(
        onClick = onCancel,
        interactionSource = interaction,
        modifier = Modifier
            .size(heightDp.dp)
            .pressScale(interaction),
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Cancel reply",
            tint = cancelColor ?: LocalContentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ReplySendButton(
    sendColor: Color,
    enabled: Boolean,
    heightDp: Int,
    interaction: MutableInteractionSource,
    onSend: () -> Unit,
) {
    FilledIconButton(
        onClick = onSend,
        enabled = enabled,
        interactionSource = interaction,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = sendColor,
            contentColor = if (sendColor.luminance() > 0.5f) PillTextColorDark else PillTextColor,
            disabledContainerColor = LocalContentColor.current.copy(alpha = 0.12f),
            disabledContentColor = LocalContentColor.current.copy(alpha = 0.4f),
        ),
        modifier = Modifier
            .size(heightDp.dp)
            .pressScale(interaction),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Send reply",
            modifier = Modifier.size(22.dp),
        )
    }
}
