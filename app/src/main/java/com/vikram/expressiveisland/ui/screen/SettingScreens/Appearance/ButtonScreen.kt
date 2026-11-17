package com.vikram.expressiveisland.ui.screen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import kotlin.math.roundToInt

import android.app.PendingIntent
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.data.ActionButtonAlignment
import com.vikram.expressiveisland.data.ActionButtonStyle
import com.vikram.expressiveisland.data.AppearanceSettings
import com.vikram.expressiveisland.data.ReplyInputStyle
import com.vikram.expressiveisland.data.SentAlignment
import com.vikram.expressiveisland.core.IslandPreviewCategory
import com.vikram.expressiveisland.ui.AppViewModel

/** Accent used by the preview event, matching the accent shown on the sibling settings screens. */
private val PreviewAccent = Color(0xFF60A5FA)

/**
 * "Action buttons" screen (reached from the Appearance screen). Configures the chips and inline
 * reply field shown in the expanded cutout: whether they appear at all, the chip style/colour/height,
 * the reply field style, and whether the cancel button sits on the leading edge. Every control feeds
 * a live preview at the top so the effect of each choice is visible immediately.
 */
@Composable
internal fun ButtonScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current


    val behaviour by viewModel.behaviour.collectAsStateWithLifecycle()
    var activeChoiceDialog by rememberSaveable { mutableStateOf<String?>(null) }
    // Local height so the sliders/preview react immediately; committed to prefs on release.
    var buttonHeight by remember(appearance.actionButtonHeightDp) {
        mutableStateOf(appearance.actionButtonHeightDp.toFloat())
    }

    // The preview reflects the in-flight height before it is persisted.

    IslandLivePreviewEffect(
        category = IslandPreviewCategory.ACTION_BUTTONS,
        label = "Action buttons",
        detail = "Expanded actions and reply controls",
        expanded = true,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingsToggleCard(shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp), title = stringResource(R.string.action_buttons_enable_title), description = stringResource(R.string.action_buttons_enable_desc), checked = behaviour.showActionButtons, onCheckedChange = viewModel::setShowActionButtons)
            SettingsToggleCard(shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), title = stringResource(R.string.action_buttons_toast_title), description = stringResource(R.string.action_buttons_toast_desc), checked = behaviour.toastOnAction, onCheckedChange = viewModel::setToastOnAction)
        }

        // Button appearance settings.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SelectionSettingCard(title = stringResource(R.string.action_buttons_style_title), value = buttonStyleLabel(appearance.actionButtonStyle), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp), onClick = { activeChoiceDialog = "style" })
            ColorPickerCard(label = stringResource(R.string.action_buttons_color_title), selected = appearance.actionButtonColor, onSelect = viewModel::setActionButtonColor, defaultLabel = stringResource(R.string.cd_color_default_accent), defaultColor = PreviewAccent, shape = RoundedCornerShape(4.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    AdjustableSlider(label = stringResource(R.string.action_buttons_height_title), valueText = "${buttonHeight.roundToInt()} dp", value = buttonHeight, valueRange = AppearanceSettings.MIN_ACTION_BUTTON_HEIGHT_DP.toFloat()..AppearanceSettings.MAX_ACTION_BUTTON_HEIGHT_DP.toFloat(), step = 2f, onValueChange = { buttonHeight = it; viewModel.setActionButtonHeight(it.roundToInt()) }, onCommit = { viewModel.setActionButtonHeight(buttonHeight.roundToInt()) })
                }
            }
            SelectionSettingCard(title = stringResource(R.string.action_buttons_alignment_title), value = alignmentLabel(appearance.actionButtonAlignment), shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), onClick = { activeChoiceDialog = "alignment" })
        }

        // Reply field and its related controls.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SelectionSettingCard(title = stringResource(R.string.action_buttons_input_style_title), value = replyStyleLabel(appearance.replyInputStyle), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp), onClick = { activeChoiceDialog = "reply" })
            SettingsToggleCard(shape = RoundedCornerShape(4.dp), title = stringResource(R.string.action_buttons_cancel_left_title), description = stringResource(R.string.action_buttons_cancel_left_desc), checked = appearance.cancelButtonOnLeft, onCheckedChange = viewModel::setCancelButtonOnLeft)
            ColorPickerCard(label = stringResource(R.string.appearance_send_color), selected = appearance.sendButtonColor, onSelect = viewModel::setSendButtonColor, defaultLabel = stringResource(R.string.cd_color_default_accent), defaultColor = PreviewAccent, shape = RoundedCornerShape(4.dp))
            ColorPickerCard(label = stringResource(R.string.appearance_cancel_color), selected = appearance.cancelButtonColor, onSelect = viewModel::setCancelButtonColor, defaultLabel = stringResource(R.string.cd_color_default_neutral), defaultColor = MaterialTheme.colorScheme.onSurfaceVariant, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
        }

        SelectionSettingCard(title = stringResource(R.string.action_buttons_sent_alignment_title), value = sentAlignmentLabel(appearance.sentAlignment), onClick = { activeChoiceDialog = "sent" })

        when (activeChoiceDialog) {
            "style" -> SelectionChoiceDialog(stringResource(R.string.action_buttons_style_title), appearance.actionButtonStyle, listOf(ChoiceOption(ActionButtonStyle.EXPRESSIVE_TONAL, stringResource(R.string.style_expressive_tonal_title), stringResource(R.string.style_expressive_tonal_desc)), ChoiceOption(ActionButtonStyle.EXPRESSIVE_FILLED, stringResource(R.string.style_expressive_filled_title), stringResource(R.string.style_expressive_filled_desc)), ChoiceOption(ActionButtonStyle.MATERIAL_YOU, stringResource(R.string.style_material_you_title), stringResource(R.string.style_material_you_desc)), ChoiceOption(ActionButtonStyle.OUTLINED, stringResource(R.string.style_outlined_title), stringResource(R.string.style_outlined_desc))), { activeChoiceDialog = null }, { viewModel.setActionButtonStyle(it); activeChoiceDialog = null })
            "alignment" -> SelectionChoiceDialog(stringResource(R.string.action_buttons_alignment_title), appearance.actionButtonAlignment, listOf(ChoiceOption(ActionButtonAlignment.LEFT, stringResource(R.string.action_buttons_align_left_title), stringResource(R.string.action_buttons_align_left_desc)), ChoiceOption(ActionButtonAlignment.CENTER, stringResource(R.string.action_buttons_align_center_title), stringResource(R.string.action_buttons_align_center_desc)), ChoiceOption(ActionButtonAlignment.RIGHT, stringResource(R.string.action_buttons_align_right_title), stringResource(R.string.action_buttons_align_right_desc)), ChoiceOption(ActionButtonAlignment.FULL, stringResource(R.string.action_buttons_align_full_title), stringResource(R.string.action_buttons_align_full_desc))), { activeChoiceDialog = null }, { viewModel.setActionButtonAlignment(it); activeChoiceDialog = null })
            "reply" -> SelectionChoiceDialog(stringResource(R.string.action_buttons_input_style_title), appearance.replyInputStyle, listOf(ChoiceOption(ReplyInputStyle.EXPRESSIVE, stringResource(R.string.input_expressive_title), stringResource(R.string.input_expressive_desc)), ChoiceOption(ReplyInputStyle.MATERIAL_YOU, stringResource(R.string.input_material_you_title), stringResource(R.string.input_material_you_desc)), ChoiceOption(ReplyInputStyle.MATERIAL_2, stringResource(R.string.input_material_2_title), stringResource(R.string.input_material_2_desc)), ChoiceOption(ReplyInputStyle.SEGMENTED, stringResource(R.string.input_segmented_title), stringResource(R.string.input_segmented_desc))), { activeChoiceDialog = null }, { viewModel.setReplyInputStyle(it); activeChoiceDialog = null })
            "sent" -> SelectionChoiceDialog(stringResource(R.string.action_buttons_sent_alignment_title), appearance.sentAlignment, listOf(ChoiceOption(SentAlignment.LEFT, stringResource(R.string.action_buttons_align_left_title), stringResource(R.string.action_buttons_sent_left_desc)), ChoiceOption(SentAlignment.CENTER, stringResource(R.string.action_buttons_align_center_title), stringResource(R.string.action_buttons_sent_center_desc)), ChoiceOption(SentAlignment.RIGHT, stringResource(R.string.action_buttons_align_right_title), stringResource(R.string.action_buttons_sent_right_desc))), { activeChoiceDialog = null }, { viewModel.setSentAlignment(it); activeChoiceDialog = null })
        }
    }
}


@Composable
private fun SelectionSettingCard(title: String, value: String, shape: Shape = RoundedCornerShape(24.dp), onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Change", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private data class ChoiceOption<T>(val value: T, val title: String, val description: String)

@Composable
private fun <T> SelectionChoiceDialog(title: String, selected: T, options: List<ChoiceOption<T>>, onDismiss: () -> Unit, onConfirm: (T) -> Unit) {
    var pending by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEach { option ->
                OptionRow(option.title, option.description, pending == option.value) { pending = option.value }
            }
        } },
        confirmButton = { TextButton(onClick = { onConfirm(pending) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun buttonStyleLabel(value: ActionButtonStyle) = when (value) {
    ActionButtonStyle.EXPRESSIVE_TONAL -> stringResource(R.string.style_expressive_tonal_title)
    ActionButtonStyle.EXPRESSIVE_FILLED -> stringResource(R.string.style_expressive_filled_title)
    ActionButtonStyle.MATERIAL_YOU -> stringResource(R.string.style_material_you_title)
    ActionButtonStyle.OUTLINED -> stringResource(R.string.style_outlined_title)
}
@Composable
private fun alignmentLabel(value: ActionButtonAlignment) = when (value) {
    ActionButtonAlignment.LEFT -> stringResource(R.string.action_buttons_align_left_title)
    ActionButtonAlignment.CENTER -> stringResource(R.string.action_buttons_align_center_title)
    ActionButtonAlignment.RIGHT -> stringResource(R.string.action_buttons_align_right_title)
    ActionButtonAlignment.FULL -> stringResource(R.string.action_buttons_align_full_title)
}
@Composable
private fun replyStyleLabel(value: ReplyInputStyle) = when (value) {
    ReplyInputStyle.EXPRESSIVE -> stringResource(R.string.input_expressive_title)
    ReplyInputStyle.MATERIAL_YOU -> stringResource(R.string.input_material_you_title)
    ReplyInputStyle.MATERIAL_2 -> stringResource(R.string.input_material_2_title)
    ReplyInputStyle.SEGMENTED -> stringResource(R.string.input_segmented_title)
}
@Composable
private fun sentAlignmentLabel(value: SentAlignment) = when (value) {
    SentAlignment.LEFT -> stringResource(R.string.action_buttons_align_left_title)
    SentAlignment.CENTER -> stringResource(R.string.action_buttons_align_center_title)
    SentAlignment.RIGHT -> stringResource(R.string.action_buttons_align_right_title)
}

/** A titled surface card that stacks a set of selectable option rows. */
@Composable
private fun OptionGroupCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun ButtonStyleOption(
    title: String,
    description: String,
    style: ActionButtonStyle,
    selected: ActionButtonStyle,
    onSelect: (ActionButtonStyle) -> Unit,
) = OptionRow(
    title = title,
    description = description,
    selected = style == selected,
    onClick = { onSelect(style) },
)

@Composable
private fun AlignmentOption(
    title: String,
    description: String,
    alignment: ActionButtonAlignment,
    selected: ActionButtonAlignment,
    onSelect: (ActionButtonAlignment) -> Unit,
) = OptionRow(
    title = title,
    description = description,
    selected = alignment == selected,
    onClick = { onSelect(alignment) },
)

@Composable
private fun SentAlignmentOption(
    title: String,
    description: String,
    alignment: SentAlignment,
    selected: SentAlignment,
    onSelect: (SentAlignment) -> Unit,
) = OptionRow(
    title = title,
    description = description,
    selected = alignment == selected,
    onClick = { onSelect(alignment) },
)

@Composable
private fun ReplyStyleOption(
    title: String,
    description: String,
    style: ReplyInputStyle,
    selected: ReplyInputStyle,
    onSelect: (ReplyInputStyle) -> Unit,
) = OptionRow(
    title = title,
    description = description,
    selected = style == selected,
    onClick = { onSelect(style) },
)

/** A single-choice row: title, supporting text and a trailing radio, the whole row tappable. */
@Composable
private fun OptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        RadioButton(selected = selected, onClick = onClick)
    }
}

/**
 * An in-app rendition of the inline reply row, so the effect of the field style, height and the
 * cancel-button placement is visible without opening a notification. Uses app-theme colours rather
 * than the island's own palette; it only mirrors the shapes and layout.
 */
@Composable
private fun ReplyInputPreview(
    inputStyle: ReplyInputStyle,
    cancelOnLeft: Boolean,
    heightDp: Int,
    sendColor: Color = MaterialTheme.colorScheme.primary,
    cancelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val segmented = inputStyle == ReplyInputStyle.SEGMENTED
    val cap = (heightDp / 2).dp
    val inner = 8.dp
    val startCap =
        RoundedCornerShape(topStart = cap, bottomStart = cap, topEnd = inner, bottomEnd = inner)
    val endCap =
        RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = cap, bottomEnd = cap)
    val fieldShape: Shape = when (inputStyle) {
        ReplyInputStyle.EXPRESSIVE -> CircleShape
        ReplyInputStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
        ReplyInputStyle.MATERIAL_2 -> RoundedCornerShape(4.dp)
        // In the segmented bar the field's inner edges are lightly rounded; its leading edge caps
        // the bar when the cancel button isn't on the left.
        ReplyInputStyle.SEGMENTED -> if (cancelOnLeft) RoundedCornerShape(inner) else startCap
    }
    val cancel = @Composable {
        Box(
            modifier = Modifier
                .size(heightDp.dp)
                .clip(
                    if (segmented && cancelOnLeft) startCap else if (segmented) RoundedCornerShape(
                        inner
                    ) else CircleShape
                )
                .background(cancelColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = cancelColor,
                modifier = Modifier.size(22.dp),
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (segmented) 4.dp else 10.dp),
    ) {
        if (cancelOnLeft) cancel()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(heightDp.dp)
                .clip(fieldShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.action_buttons_reply_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!cancelOnLeft) cancel()
        val send = sendColor
        Box(
            modifier = Modifier
                .size(heightDp.dp)
                .clip(if (segmented) endCap else CircleShape)
                .background(send),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                tint = if (send.luminance() > 0.5f) Color(0xFF0A0A0A) else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
