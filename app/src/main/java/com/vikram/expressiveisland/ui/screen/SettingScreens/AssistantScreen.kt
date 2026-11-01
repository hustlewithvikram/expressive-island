package com.vikram.expressiveisland.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vikram.expressiveisland.R
import com.vikram.expressiveisland.core.DynamicTile
import com.vikram.expressiveisland.data.AssistantActivationMode
import com.vikram.expressiveisland.ui.AppViewModel
import com.vikram.expressiveisland.ui.components.ExpressiveSegmentedRow

@Composable
internal fun AssistantScreen(
    viewModel: AppViewModel,
    contentPadding: PaddingValues,
) {
    val settings by viewModel.assistantTile.collectAsStateWithLifecycle()
    val selectedTab = if (settings.activationMode == AssistantActivationMode.AUTO) 0 else 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ExpressiveSegmentedRow(
            options = listOf("Auto", "Manual"),
            selectedIndex = selectedTab,
            onSelect = {
                viewModel.setAssistantActivationMode(
                    if (it == 0) AssistantActivationMode.AUTO else AssistantActivationMode.MANUAL,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            allowFullLabels = true,
        )

        if (settings.activationMode == AssistantActivationMode.AUTO) {
            AssistantInfoCard(
                title = "Automatic assistant detection",
                description = "Reads supported assistant app windows and displays their activity. Automatic expansion follows the controls below.",
            )

            SettingsToggleCard(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                title = stringResource(R.string.assistant_auto_read_title),
                description = stringResource(R.string.assistant_auto_read_desc),
                checked = settings.autoReadAssistantEvents,
                onCheckedChange = viewModel::setAssistantAutoReadEvents,
            )

            SettingsToggleCard(
                shape = RoundedCornerShape(4.dp),
                title = stringResource(R.string.assistant_auto_expand_title),
                description = stringResource(R.string.assistant_auto_expand_desc),
                checked = settings.autoExpandAssistantResponses,
                onCheckedChange = viewModel::setAssistantAutoExpandResponses,
            )

            SettingsToggleCard(
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                title = stringResource(R.string.assistant_display_answer_title),
                description = stringResource(R.string.assistant_display_answer_desc),
                checked = settings.displayAnswerInCutout,
                onCheckedChange = viewModel::setAssistantDisplayAnswerInCutout,
            )
        } else {
            AssistantInfoCard(
                title = "Manual assistant activation",
                description = "Automatic screen reading is disabled. Open App Assistant through the Camera Hole Touch action configured in Behaviour.",
            )

            Text(
                text = "Panel behaviour",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            )

            var expandedToPill by remember(settings.manualExpandedToPillTimeoutSeconds) {
                mutableFloatStateOf(settings.manualExpandedToPillTimeoutSeconds.toFloat())
            }
            SettingsSliderCard(
                shape = RoundedCornerShape(24.dp),
                title = "Expanded to Pill timeout",
                description = "How long the expanded assistant stays open before shrinking to the compact pill.",
                valueText = stringResource(R.string.assistant_timeout_value, expandedToPill.toInt()),
                value = expandedToPill,
                valueRange = 5f..60f,
                step = 1f,
                onValueChange = { expandedToPill = it },
                onCommit = { viewModel.setAssistantManualExpandedToPillTimeoutSeconds(expandedToPill.toInt()) },
            )

            SettingsToggleCard(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                title = "Auto hide after some time",
                description = "Automatically remove the compact assistant pill after its timeout.",
                checked = settings.manualAutoHideEnabled,
                onCheckedChange = viewModel::setAssistantManualAutoHideEnabled,
            )

            AnimatedVisibility(visible = settings.manualAutoHideEnabled) {
                var pillToHidden by remember(settings.manualPillToHiddenTimeoutSeconds) {
                    mutableFloatStateOf(settings.manualPillToHiddenTimeoutSeconds.toFloat())
                }
                SettingsSliderCard(
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    title = "Pill to Hidden timeout",
                    description = "How long the compact assistant pill remains visible before it disappears.",
                    valueText = stringResource(R.string.assistant_timeout_value, pillToHidden.toInt()),
                    value = pillToHidden,
                    valueRange = 2f..60f,
                    step = 1f,
                    onValueChange = { pillToHidden = it },
                    onCommit = { viewModel.setAssistantManualPillToHiddenTimeoutSeconds(pillToHidden.toInt()) },
                )
            }

            Text(
                text = "Expanded panel elements",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            )

            SettingsToggleCard(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                title = "Microphone button",
                description = "Show or hide the voice assistant shortcut.",
                checked = settings.showAssistantMicButton,
                onCheckedChange = viewModel::setAssistantShowMicButton,
            )
            SettingsToggleCard(
                shape = RoundedCornerShape(4.dp),
                title = "Prompt input",
                description = "Show or hide the text prompt field.",
                checked = settings.showAssistantPromptInput,
                onCheckedChange = viewModel::setAssistantShowPromptInput,
            )
            SettingsToggleCard(
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                title = "Send button",
                description = if (settings.showAssistantPromptInput) {
                    "Show the send button beside the prompt field. When hidden, Enter submits the prompt."
                } else {
                    "Enable Prompt input first. The send button is automatically hidden when the prompt field is off."
                },
                checked = settings.showAssistantSendButton && settings.showAssistantPromptInput,
                enabled = settings.showAssistantPromptInput,
                onCheckedChange = viewModel::setAssistantShowSendButton,
            )
        }

        Text(
            text = "Collapse gestures",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp),
        )
        SettingsToggleCard(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            title = "Swipe up to collapse",
            description = "Swipe up on the assistant header to return to the pill.",
            checked = settings.swipeUpToCollapse,
            onCheckedChange = viewModel::setAssistantSwipeUpToCollapse,
        )
        SettingsToggleCard(
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            title = "Tap to collapse",
            description = "Tap the expanded assistant background to return to the pill.",
            checked = settings.tapToCollapse,
            onCheckedChange = viewModel::setAssistantTapToCollapse,
        )

        Text(
            text = "Expanded layout",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp),
        )
        var maxHeight by remember(settings.maxCutoutHeightPercent) {
            mutableFloatStateOf(settings.maxCutoutHeightPercent.toFloat())
        }
        SettingsSliderCard(
            shape = RoundedCornerShape(24.dp),
            title = stringResource(R.string.assistant_max_height_title),
            description = "Maximum height of the expanded assistant island, shared by Auto and Manual modes.",
            valueText = stringResource(R.string.assistant_max_height_value, maxHeight.toInt()),
            value = maxHeight,
            valueRange = 10f..80f,
            step = 5f,
            onValueChange = { maxHeight = it },
            onCommit = { viewModel.setAssistantMaxCutoutHeightPercent(maxHeight.toInt()) },
        )

        Text(
            text = "Shared appearance",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp),
        )
        ColorPickerCard(
            label = stringResource(R.string.tile_icon_container_title),
            selected = settings.iconContainerColor,
            onSelect = viewModel::setAssistantIconContainerColor,
            defaultLabel = stringResource(R.string.music_default_accent),
            defaultColor = Color(DynamicTile.ASSISTANT.accent),
        )
        SettingsToggleCard(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
            title = stringResource(R.string.assistant_animated_icon_title),
            description = stringResource(R.string.assistant_animated_icon_desc),
            checked = settings.useAnimatedIcon,
            onCheckedChange = viewModel::setAssistantUseAnimatedIcon,
        )

        Text(
            text = stringResource(R.string.assistant_tile_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun AssistantInfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
