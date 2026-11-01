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

/**
 * Assistant tile settings. Auto reads supported assistant windows; Manual disables screen reading
 * and automatic expansion, leaving Behaviour > Camera Hole Touch as the single manual trigger.
 */
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
                description = "Expressive Island reads supported assistant app windows and displays their activity in the cutout. Automatic expansion follows the controls below.",
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
                shape = RoundedCornerShape(4.dp),
                title = stringResource(R.string.assistant_display_answer_title),
                description = stringResource(R.string.assistant_display_answer_desc),
                checked = settings.displayAnswerInCutout,
                onCheckedChange = viewModel::setAssistantDisplayAnswerInCutout,
            )

            AnimatedVisibility(visible = settings.displayAnswerInCutout) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    var maxHeight by remember(settings.maxCutoutHeightPercent) {
                        mutableFloatStateOf(settings.maxCutoutHeightPercent.toFloat())
                    }
                    SettingsSliderCard(
                        shape = RoundedCornerShape(4.dp),
                        title = stringResource(R.string.assistant_max_height_title),
                        description = stringResource(R.string.assistant_max_height_desc),
                        valueText = stringResource(R.string.assistant_max_height_value, maxHeight.toInt()),
                        value = maxHeight,
                        valueRange = 10f..80f,
                        step = 5f,
                        onValueChange = { maxHeight = it },
                        onCommit = { viewModel.setAssistantMaxCutoutHeightPercent(maxHeight.toInt()) },
                    )

                    var pillTimeout by remember(settings.pillTimeoutSeconds) {
                        mutableFloatStateOf(settings.pillTimeoutSeconds.toFloat())
                    }
                    SettingsSliderCard(
                        shape = RoundedCornerShape(4.dp),
                        title = stringResource(R.string.assistant_pill_timeout_title),
                        description = stringResource(R.string.assistant_pill_timeout_desc),
                        valueText = stringResource(R.string.assistant_timeout_value, pillTimeout.toInt()),
                        value = pillTimeout,
                        valueRange = 2f..30f,
                        step = 1f,
                        onValueChange = { pillTimeout = it },
                        onCommit = { viewModel.setAssistantPillTimeoutSeconds(pillTimeout.toInt()) },
                    )

                    var expandedTimeout by remember(settings.expandedTimeoutSeconds) {
                        mutableFloatStateOf(settings.expandedTimeoutSeconds.toFloat())
                    }
                    SettingsSliderCard(
                        shape = RoundedCornerShape(4.dp),
                        title = stringResource(R.string.assistant_expanded_timeout_title),
                        description = stringResource(R.string.assistant_expanded_timeout_desc),
                        valueText = stringResource(R.string.assistant_timeout_value, expandedTimeout.toInt()),
                        value = expandedTimeout,
                        valueRange = 5f..60f,
                        step = 1f,
                        onValueChange = { expandedTimeout = it },
                        onCommit = { viewModel.setAssistantExpandedTimeoutSeconds(expandedTimeout.toInt()) },
                    )
                }
            }
        } else {
            AssistantInfoCard(
                title = "Manual assistant activation",
                description = "Automatic screen reading and automatic expansion are disabled in this mode. Open App Assistant using the Camera Hole Touch action configured in Behaviour. That trigger stays in one place and is not duplicated here.",
            )

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            )

            SettingsToggleCard(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                title = stringResource(R.string.assistant_animated_icon_title),
                description = stringResource(R.string.assistant_animated_icon_desc),
                checked = settings.useAnimatedIcon,
                onCheckedChange = viewModel::setAssistantUseAnimatedIcon,
            )

            Text(
                text = stringResource(R.string.tile_icon_container_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            )
            ColorPickerCard(
                label = stringResource(R.string.tile_icon_container_label),
                selected = settings.iconContainerColor,
                onSelect = viewModel::setAssistantIconContainerColor,
                defaultLabel = stringResource(R.string.music_default_accent),
                defaultColor = Color(DynamicTile.ASSISTANT.accent),
            )
        }

        Text(
            text = stringResource(R.string.assistant_tile_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun AssistantInfoCard(
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
